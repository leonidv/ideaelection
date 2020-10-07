package idel.api

import arrow.core.Either
import arrow.core.Option
import idel.domain.*
import idel.infrastructure.security.IdelOAuth2User
import io.konform.validation.Invalid
import io.konform.validation.Valid
import org.springframework.core.convert.converter.Converter
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.lang.IllegalArgumentException
import java.util.*

@RestController
@RequestMapping("/groups")
class GroupController(private val repository: GroupRepository) {

    val factory = GroupFactory()


    fun <T> editablePropertiesAreValid(
            properties: IGroupEditableProperties,
            ifValid :() -> ResponseEntity<ResponseOrError<T>>) : ResponseEntity<ResponseOrError<T>> {
        val validateResult = GroupValidation.propertiesValidation(properties);
        return when (validateResult) {
            is Valid -> ifValid()
            is Invalid -> ResponseOrError.invalid(validateResult.errors)
        }
    }

    @PostMapping
    fun create(
            @RequestBody properties : GroupEditableProperties,
            @AuthenticationPrincipal user : IdelOAuth2User
    ) : ResponseEntity<ResponseOrError<Group>> = editablePropertiesAreValid(properties) {
        val either = factory.createGroup(user.id(), properties) as Either.Right<Group>
        val group = either.b
        repository.add(group)
        ResponseOrError.ok(group)
    }

    @GetMapping
    fun findAvailableForJoining(@AuthenticationPrincipal user : IdelOAuth2User,
                                @RequestParam(required = false, defaultValue = "0") first: Int,
                                @RequestParam(required = false, defaultValue = "10") last: Int,
                                @RequestParam(required = false, defaultValue = "") sorting: GroupSorting,
                                @RequestParam("onlyAvailable") onlyAvailableForJoining : Optional<Boolean>
    ) : ResponseEntity<ResponseOrError<List<Group>>> {

        // no time to add Option support
        val onlyAvailable = Option.fromNullable(onlyAvailableForJoining.orElseGet {null})
        val filtering = GroupFiltering(availableForJoiningEmail = onlyAvailable.map {user.email} )

        return ResponseOrError.data(repository.load(first, last, sorting, filtering))
    }

}

class StringToGroupSortingConverter : Converter<String, GroupSorting> {
    val DEFAULT = GroupSorting.CTIME_DESC;

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