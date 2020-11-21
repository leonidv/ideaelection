package idel.domain

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import idel.infrastructure.repositories.PersistsUser
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class SecurityServiceSpec : DescribeSpec({
    context("[userA] is admin of [testGroup] which has members [userB, userC]") {
        val userA = user("userA")
        val userB = user("userB")
        val userC = user("userC")
        val userD = user("userD")

        val group = Group(
                id = "testGroup",
                ctime = LocalDateTime.now(),
                creator = UserInfo.ofUser(userB),
                title = "Test group",
                description = "",
                logo = "",
                entryMode = GroupEntryMode.PRIVATE,
                administrators = listOf(UserInfo.ofUser(userA))
        )

        val groupMemberRepository = mockk<GroupMemberRepository>()
        val members = listOf(userB, userC)
        members.forEach {user ->
            every {groupMemberRepository.isMember(group.id, user.id)} returns Either.right(true)
        }
        every {groupMemberRepository.isMember(group.id, userD.id)} returns Either.right(false)

        val securityService = SecurityService(groupMemberRepository)


        describe("group security level") {
            table(
                    headers("user", "levels"),
                    row(userA, setOf(GroupAccessLevel.ADMIN, GroupAccessLevel.MEMBER)),
                    row(userB, setOf(GroupAccessLevel.MEMBER)),
                    row(userC, setOf(GroupAccessLevel.MEMBER)),
                    row(userD, setOf(GroupAccessLevel.NOT_MEMBER))
            ).forAll {user, requiredLevels ->
                it("[${user.displayName}] has levels ${requiredLevels}") {
                    val actualLevel = securityService.groupAccessLevel(group, user)
                    actualLevel.shouldBeRight(requiredLevels)
                }
            }
        }

        describe("idea security level") {
            describe("group has idea created by [userB], idea assigned to [userC]") {
                val idea = idea(userB, group, Option.just(userC))

                table(
                        headers("user","idea access levels"),
                        row(userA, setOf(IdeaAccessLevel.GROUP_ADMIN, IdeaAccessLevel.GROUP_MEMBER,
                                IdeaAccessLevel.ASSIGNEE, IdeaAccessLevel.AUTHOR)),
                        row(userB, setOf(IdeaAccessLevel.GROUP_MEMBER, IdeaAccessLevel.AUTHOR)),
                        row(userC, setOf(IdeaAccessLevel.GROUP_MEMBER, IdeaAccessLevel.ASSIGNEE)),
                        row(userD, setOf(IdeaAccessLevel.DENIED))
                ).forAll{user, requiredLevels ->
                    it("[${user.id}] has levels ${requiredLevels}") {
                        val actualLevels = securityService.ideaAccessLevels(group, idea, user)
                        actualLevels.shouldBeRight(requiredLevels)
                    }
                }

            }

            describe("group has idea created by [userB] and assigned to himself ([userB])") {
                val idea = idea(userB, group, Option.just(userB))
                val requiredLevels = setOf(IdeaAccessLevel.GROUP_MEMBER, IdeaAccessLevel.AUTHOR, IdeaAccessLevel.ASSIGNEE)
                it("[${userB.id}] has levels [$requiredLevels]") {
                    val actualLevels = securityService.ideaAccessLevels(group, idea, userB)
                    actualLevels.shouldBeRight(requiredLevels)
                }
            }
        }



        // TODO добавить тесты на комбинацию Author, Assignee. Добавить тест, когда автор не входит в группу (доступа нет)


    }


})

fun user(name: String, roles: Set<String> = setOf(Roles.USER)): User = object : User {
    override val id: String = "$name@spec"
    override val email: String = "$name@mail"
    override val displayName: String = "$name"
    override val avatar: String = ""
    override val roles: Set<String> = roles;
}

fun idea(creator: User, group: Group, assignee: Option<User>, voters: Set<User> = emptySet()): Idea {
    return Idea(
            id = generateId(),
            groupId = group.id,
            ctime = LocalDateTime.now(),
            title = "Test idea",
            description = "some description",
            link = "",
            assignee = assignee.map {it.id}.getOrElse {""},
            implemented = false,
            offeredBy = creator.id,
            voters = voters.map {it.id}.toSet()
    )
}