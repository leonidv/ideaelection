package idel.scriptformanual

import idel.tests.apiobject.GroupsApi
import idel.tests.apiobject.User
import idel.tests.infrastructure.extractField

fun main() {
    val userB = User("userB", idelUrl = "https://api.test.saedi.io")

    val createGroupResponse = userB.groups.create(
        name = "Group with 1000 idea",
        entryMode = GroupsApi.EntryMode.PUBLIC,
        description = "group for testing huge count of ideas"
    )

    val groupId = createGroupResponse.extractField("id")
    (1..1000).forEach {i ->
        userB.ideas.quickAdd(groupId, "$i")
    }

}