package idel.api

import arrow.core.Either
import idel.domain.*
import idel.infrastructure.security.IdelOAuth2User
import io.konform.validation.Invalid
import io.konform.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/groups")
class GroupController(private val repository: GroupRepository) {

    val factory = GroupFactory()


    fun <T> editablePropertiesAreValid(
            properties: IGroupEditableProperties,
            ifValid :() -> ResponseEntity<ResponseOrError<T>>) : ResponseEntity<ResponseOrError<T>> {
        val validateResult = IGroupEditableProperties.validation(properties);
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
}