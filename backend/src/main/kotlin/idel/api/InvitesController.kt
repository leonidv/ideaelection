package idel.api

import arrow.core.computations.either
import arrow.core.Either
import idel.domain.*
import mu.KotlinLogging
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/invites")
class InvitesController(
    private val groupMembershipService: GroupMembershipService,
    private val inviteRepository: InviteRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val security: ApiSecurity
) : DataOrErrorHelper {
    override val log = KotlinLogging.logger {}

    data class InvitesPayload(val invites: List<Invite>, val groups: List<Group>, val users: Set<User>)

    data class InvitePayload(val invite: Invite, val group: Group, val users: Set<User>)

    data class InviteCreateParams(
        val groupId: GroupId,
        val message: String,
        val registeredUsersIds: Set<UserId>,
        val newUsersEmails: Set<String>
    )


    @PostMapping
    fun create(
        @AuthenticationPrincipal user: User,
        @RequestBody params: InviteCreateParams
    ): ResponseDataOrError<GroupMembershipService.InviteToGroupResult> {
        return security.group.asAdmin(params.groupId, user) {group ->
            val result = groupMembershipService.inviteUsersToGroup(
                group = group,
                message = params.message,
                author = user,
                registeredUserIds = params.registeredUsersIds,
                newUserEmails = params.newUsersEmails
            )
            Either.Right(result)
        }.asHttpResponse()
    }


    @GetMapping("/{inviteId}")
    fun load(@AuthenticationPrincipal user: User, @PathVariable inviteId: UUID): ResponseDataOrError<InvitePayload> {
        return security.invite.asOwnerOrGroupAdmin(user, inviteId) {invite, accessLevel ->
            fTransaction {
                either.eager {
                    val group = groupRepository.load(invite.groupId).bind()
                    val author = userRepository.load(invite.author).bind()

                    InvitePayload(invite, group, setOf(author))
                }
            }
        }.asHttpResponse()
    }

    @GetMapping(params = ["userId"])
    fun loadInvitesForUser(
        @AuthenticationPrincipal user: User,
        @RequestParam userId: UUID,
        @RequestParam(required = false, defaultValue = "") order: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ): ResponseDataOrError<InvitesPayload> {
        return security.user.asHimSelf(user.id, user) {
            either.eager {
                val invites =
                    inviteRepository.loadByUser(
                        userId = userId,
                        statuses = setOf(AcceptStatus.UNRESOLVED),
                        order = order,
                        pagination = pagination
                    ).bind()
                enrichInvites(invites).bind()
            }
        }.asHttpResponse()
    }

    @GetMapping(params = ["groupId"])
    fun loadInvitesForGroup(
        @AuthenticationPrincipal user: User,
        @RequestParam groupId: GroupId,
        @RequestParam(required = false, defaultValue = "") order: GroupMembershipRequestOrdering,
        pagination: Repository.Pagination
    ): ResponseDataOrError<InvitesPayload> {
        return security.group.asAdmin(groupId, user) {
            either.eager {
                val invites =
                    inviteRepository.loadByGroup(
                        groupId = groupId,
                        statuses = setOf(AcceptStatus.UNRESOLVED),
                        order = order,
                        pagination = pagination
                    ).bind()
                enrichInvites(invites).bind()
            }
        }.asHttpResponse()
    }

    private fun enrichInvites(invites: List<Invite>): Either<DomainError, InvitesPayload> {
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
        @PathVariable inviteId: UUID,
        @RequestBody resolution: Resolution
    ): ResponseDataOrError<Invite> {
        return security.invite.asOwnerOrGroupAdmin(user, inviteId) {invite, level ->
            groupMembershipService.resolveInvite(user, invite, resolution.status, level)
        }.asHttpResponse()
    }
}