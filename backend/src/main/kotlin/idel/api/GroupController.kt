package idel.api

import idel.domain.Group
import idel.domain.GroupFactory
import idel.domain.GroupRepository
import idel.infrastructure.security.IdelOAuth2User
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

    data class GroupProperties(
            override val title : String,
            override val description : String,
            override val usersRestrictions : Set<String>) : idel.domain.GroupEditableProperties

    @PostMapping
    fun create(
            @RequestBody properties : GroupProperties,
            @AuthenticationPrincipal user : IdelOAuth2User
    ) : ResponseEntity<ResponseOrError<Group>> {
        val group = factory.from(user.id(), properties)
        repository.add(group)
        return ResponseOrError.ok(group)
    }
}