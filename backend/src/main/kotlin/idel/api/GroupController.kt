package idel.api

import arrow.core.*
import arrow.core.extensions.fx
import idel.domain.*
import idel.infrastructure.security.IdelOAuth2User
import io.konform.validation.Invalid
import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.Optional
import kotlin.IllegalArgumentException

@RestController
@RequestMapping("/groups")
class GroupController(
        private val groupRepository: GroupRepository,
        private val userRepository: UserRepository
) {
    val log = KotlinLogging.logger {}

    val factory = GroupFactory()


    data class GroupInitInfo(
            override val title: String,
            override val description: String,
            override val logo: String,
            override val entryMode: GroupEntryMode,
            val administrators: List<UserId>,
            val members: List<UserId>
    ) : IGroupEditableProperties

    @PostMapping
    fun create(
            @RequestBody properties: GroupInitInfo,
            @AuthenticationPrincipal user: IdelOAuth2User
    ): EntityOrError<Group> {
        val y: Either<Exception, Either<Invalid<IGroupEditableProperties>, Group>> = Either.fx {
            val (adminsUserInfo) = userRepository.loadUserInfo(properties.administrators)
            val (membersUserInfo) = userRepository.loadUserInfo(properties.members)
            val (creatorUserInfo) = userRepository.loadUserInfo(listOf(user.id))

            val eGroup = factory.createGroup(creatorUserInfo.first(), properties, adminsUserInfo, membersUserInfo)
            eGroup
        }

        return when (y) {
            is Either.Left -> DataOrError.internal(y.a) // trouble on getting data from database
            is Either.Right -> when (val eGroup = y.b) {
                is Either.Left -> DataOrError.invalid(eGroup.a.errors)
                is Either.Right -> DataOrError.fromEither(groupRepository.add(eGroup.b), log)
            }
        }
    }


    @GetMapping
    fun load(@AuthenticationPrincipal user: IdelOAuth2User,
             @RequestParam(required = false, defaultValue = "0") first: Int,
             @RequestParam(required = false, defaultValue = "10") last: Int,
             @RequestParam(required = false, defaultValue = "") sorting: GroupSorting,
             @RequestParam(required = false, defaultValue = "false") onlyAvailable: Boolean,
             @RequestParam("userId") userId: Optional<String>
    ): ResponseEntity<DataOrError<List<Group>>> {
        val pagination = Repository.Pagination(first, last)
        return if (onlyAvailable) {
            DataOrError.fromEither(groupRepository.loadOnlyAvailable(pagination, sorting), log)
        } else {
            DataOrError.ok(emptyList())
        }
    }

    @DeleteMapping("{groupId}/members/{userId}")
    fun kick(@PathVariable groupId: String, @PathVariable userId: String
    ): EntityOrError<String> {
        val result = groupRepository.removeMember(groupId, userId).map {"ok"}
        return DataOrError.fromEither(result, log)
    }

}

class StringToGroupSortingConverter : Converter<String, GroupSorting> {
    val DEFAULT = GroupSorting.CTIME_DESC

    override fun convert(source: String): GroupSorting {
        return if (source.isNullOrBlank()) {
            DEFAULT
        } else {
            try {
                GroupSorting.valueOf(source.toUpperCase())
            } catch (ex: IllegalArgumentException) {
                DEFAULT
            }
        }
    }
}