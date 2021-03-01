package idel.api

import arrow.core.*
import arrow.core.extensions.fx
import idel.domain.*
import idel.infrastructure.security.IdelOAuth2User
import io.konform.validation.Invalid
import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.Optional
import kotlin.IllegalArgumentException

@RestController
@RequestMapping("/groups")
class GroupController(
        private val groupRepository: GroupRepository,
        private val userRepository: UserRepository,
        private val groupMemberRepository: GroupMemberRepository,
        apiSecurityFactory: ApiSecurityFactory
) {
    val log = KotlinLogging.logger {}

    val factory = GroupFactory()

    val security = apiSecurityFactory.create(log)

    data class GroupInitInfo(
            override val name: String,
            override val description: String,
            override val logo: String,
            override val entryMode: GroupEntryMode,
            val administrators: List<UserId>
    ) : IGroupEditableProperties



    @PostMapping
    fun create(
            @RequestBody properties: GroupInitInfo,
            @AuthenticationPrincipal user: IdelOAuth2User
    ): EntityOrError<Group> {
        val y: Either<Exception, Either<Invalid<IGroupEditableProperties>, Group>> = Either.fx {
            val (adminsUserInfo) = userRepository.loadUserInfo(properties.administrators)
            val (creatorUserInfo) = userRepository.loadUserInfo(listOf(user.id))

            val eGroup = factory.createGroup(creatorUserInfo.first(), properties, adminsUserInfo)
            eGroup
        }

        return when (y) {
            is Either.Left -> DataOrError.fromEither(y, log) // process possible options of error
            is Either.Right -> when (val eGroup = y.b) {
                is Either.Left -> DataOrError.invalid(eGroup.a.errors)
                is Either.Right -> DataOrError.fromEither(groupRepository.add(eGroup.b), log)
            }
        }
    }


    @GetMapping("/{groupId}")
    fun load(@AuthenticationPrincipal user: IdelOAuth2User,
             @PathVariable groupId: String) : EntityOrError<Group> {
        return DataOrError.fromEither( groupRepository.load(groupId), log)
    }


    @GetMapping(params = ["userId"])
    fun loadByUser(
            @AuthenticationPrincipal user: IdelOAuth2User,
            @RequestParam userId : String,
            @RequestParam(required = false, defaultValue = "") order: GroupOrdering,
            pagination: Repository.Pagination
    ) : EntityOrError<List<Group>> {
        return security.user.asHimSelf(userId, user) {
            groupRepository.loadByUser(userId,pagination, order)
        }
    }

    @GetMapping(params = ["onlyAvailable"])
    fun loadAvailableForUser(
            @AuthenticationPrincipal user : IdelOAuth2User,
            @RequestParam(defaultValue = "true") onlyAvailable: Boolean,
            @RequestParam(defaultValue = "") ordering: GroupOrdering,
            pagination: Repository.Pagination
    ) : EntityOrError<List<Group>> {
        val result = groupRepository.loadOnlyAvailable(pagination, ordering)
        return DataOrError.fromEither(result, log)
    }

    @GetMapping("/{groupId}/members")
    fun loadUsers(
        @AuthenticationPrincipal user : IdelOAuth2User,
        @PathVariable groupId: String,
        @RequestParam username : Optional<String>,
        pagination: Repository.Pagination
    ) : EntityOrError<List<UserInfo>> {
        return security.group.asMember(groupId, user) {
            userRepository
                .loadByGroup(groupId,pagination,username.asOption())
                .map { users -> // map either
                    users.map {UserInfo.ofUser(it)}
                }
        }
    }



    @DeleteMapping("/{groupId}/members/{userId}")
    fun removeUser(
        @AuthenticationPrincipal user : IdelOAuth2User,
        @PathVariable groupId: String,
        @PathVariable userId : String
    ) : EntityOrError<String> {
        return security.groupMember.asAdminOrHimSelf(groupId, userId, user) { _, _ ->
            groupMemberRepository.removeFromGroup(groupId, userId).map {"ok"}
        }
    }
}

class StringToGroupSortingConverter : Converter<String, GroupOrdering> {
    val DEFAULT = GroupOrdering.CTIME_DESC

    override fun convert(source: String): GroupOrdering {
        return if (source.isNullOrBlank()) {
            DEFAULT
        } else {
            try {
                GroupOrdering.valueOf(source.toUpperCase())
            } catch (ex: IllegalArgumentException) {
                DEFAULT
            }
        }
    }
}