package idel.api

import arrow.core.computations.either
import arrow.core.Either
import idel.domain.*
import mu.KotlinLogging
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/invites")
class InvitesController(
    private val groupMembershipService: GroupMembershipService,
    private val inviteRepository: InviteRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    apiSecurityFactory: ApiSecurityFactory
) {
    private val log = KotlinLogging.logger {}

    val security = apiSecurityFactory.create(log)

    data class InvitesPayload(val invites: List<Invite>, val groups: List<Group>, val users: Set<User>)

    data class InvitePayload(val invite: Invite, val group: Group, val users: Set<User>)

    data class InviteCreateParams(
        val groupId: String,
        val message: String,
        val registeredUsersIds: Set<String>,
        val newUsersEmails: Set<String>
    )

    data class AddInviteResult(val invites: Set<Invite>, val errorForUsers: Set<String>)

    @PostMapping
    fun create(
        @AuthenticationPrincipal user: User,
        @RequestBody params: InviteCreateParams
    ): EntityOrError<AddInviteResult> {
        return security.group.asAdmin(params.groupId, user) {
            groupMembershipService.inviteUsersToGroup(
                groupId = params.groupId,
                message = params.message,
                author = user,
                registeredUserIds = params.registeredUsersIds,
                newUserEmails = params.newUsersEmails
            ).map {result -> AddInviteResult(invites = result.createdInvites, errorForUsers = result.errorsFor)}
        }
    }


    @GetMapping("/{inviteId}")
    fun load(@PathVariable inviteId: String): EntityOrError<InvitePayload> {
        val result: Either<Exception, InvitePayload> = either.eager {
            val invite = inviteRepository.load(inviteId).bind()
            val group = groupRepository.load(invite.groupId).bind()
            val author = userRepository.load(invite.author).bind()

            InvitePayload(invite, group, setOf(author))
        }

        return DataOrError.fromEither(result, log)
    }

    @GetMapping(params = ["userId"])
    fun loadInvitesForUser(
        @AuthenticationPrincipal user: User,
        @RequestParam userId: String,
        @RequestParam(required = false, defaultValue = "") order: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ): EntityOrError<InvitesPayload> {
        return security.user.asHimSelf(user.id, user) {
            either.eager {
                val invites = inviteRepository.loadByUser(userId, order, pagination).bind()
                enrichInvites(invites).bind()
            }
        }
    }

    @GetMapping(params = ["groupId"])
    fun loadInvitesForGroup(
        @AuthenticationPrincipal user: User,
        @RequestParam groupId: String,
        @RequestParam(required = false, defaultValue = "") order: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ): EntityOrError<InvitesPayload> {
        return security.group.asAdmin(groupId, user) {
            either.eager {
                val invites = inviteRepository.loadByGroup(groupId, order, pagination).bind()
                enrichInvites(invites).bind()
            }

        }
    }

    private fun enrichInvites(invites: List<Invite>): Either<Exception, InvitesPayload> {
        return either.eager {
            val groups = invites.map {it.groupId}.toSet()
                .map {groupId -> groupRepository.load(groupId).bind()}
            val usersIds = invites.map {invite ->
                setOf(invite.author, invite.userId)
            }.flatten().filterNotNull()
            val users = usersIds.map {userId -> userRepository.load(userId).bind()}

            InvitesPayload(invites, groups, users.toSet())
        }
    }

    data class Resolution(val status: AcceptStatus)

    @PatchMapping("{inviteId}/status")
    fun resolve(
        @AuthenticationPrincipal user: User,
        @PathVariable inviteId: String,
        @RequestBody resolution: Resolution
    ): EntityOrError<Invite> {
        val result = groupMembershipService.resolveInvite(inviteId, resolution.status)
        return DataOrError.fromEither(result, log)
    }

    @DeleteMapping("/{inviteId}")
    fun delete(@AuthenticationPrincipal user: User, @PathVariable inviteId: String): EntityOrError<String> {
        val result = inviteRepository.delete(inviteId).map {"ok"}
        return DataOrError.fromEither(result, log);
    }
}