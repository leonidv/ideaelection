package idel.domain

import arrow.core.*
import com.couchbase.client.core.cnc.Context
import com.couchbase.client.core.error.DocumentNotFoundException
import com.couchbase.client.core.error.context.ErrorContext
import io.kotest.assertions.arrow.either.shouldBeLeft
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
            name = "Test group",
            description = "",
            logo = "",
            entryMode = GroupEntryMode.PRIVATE,
            entryQuestion = "",
            domainRestrictions = emptyList(),
            joiningKey = generateId(),
            state = GroupState.ACTIVE,
            membersCount = 3,
            ideasCount = 0
        )

        val groupMemberUserA = GroupMember.of(group.id, userA, GroupMemberRole.GROUP_ADMIN)
        val groupMemberUserB = GroupMember.of(group.id, userB, GroupMemberRole.MEMBER)
        val groupMemberUserC = GroupMember.of(group.id, userC, GroupMemberRole.MEMBER)

        val groupMemberRepository = mockk<GroupMemberRepository>()

        every {groupMemberRepository.load(group.id, userA.id)} returns Either.Right(groupMemberUserA)
        every {groupMemberRepository.load(group.id, userB.id)} returns Either.Right(groupMemberUserB)
        every {groupMemberRepository.load(group.id, userC.id)} returns Either.Right(groupMemberUserC)
        every {groupMemberRepository.load(group.id, userD.id)} returns Either.Left(DocumentNotFoundException(object :
            ErrorContext(null) {}))

        val groupRepository = mockk<GroupRepository>()
        every {groupRepository.exists(group.id)} returns Either.Right(true)

        val securityService = SecurityService(groupMemberRepository, groupRepository)


        describe("group security level") {
            table(
                headers("user", "levels"),
                row(userA, setOf(GroupAccessLevel.ADMIN, GroupAccessLevel.MEMBER)),
                row(userB, setOf(GroupAccessLevel.MEMBER)),
                row(userC, setOf(GroupAccessLevel.MEMBER)),
            ).forAll {user, requiredLevels ->
                it("[${user.displayName}] has levels ${requiredLevels}") {
                    val actualLevel = securityService.groupAccessLevel(group.id, user)
                    actualLevel.shouldBeRight(requiredLevels)
                }
            }

            it("${userD.id} can't get access to document (notfound)") {
                val actualLevels = securityService.groupAccessLevel(group.id, userD)
                actualLevels.shouldBeLeft()
            }
        }

        describe("idea security level") {
            describe("group has idea created by [userB], idea assigned to [userC]") {
                val idea = idea(userB, group, Some(userC))

                table(
                    headers("user", "idea access levels"),
                    row(
                        userA, setOf(
                            IdeaAccessLevel.GROUP_ADMIN, IdeaAccessLevel.GROUP_MEMBER,
                            IdeaAccessLevel.ASSIGNEE, IdeaAccessLevel.AUTHOR
                        )
                    ),
                    row(userB, setOf(IdeaAccessLevel.GROUP_MEMBER, IdeaAccessLevel.AUTHOR)),
                    row(userC, setOf(IdeaAccessLevel.GROUP_MEMBER, IdeaAccessLevel.ASSIGNEE)),
                ).forAll {user, requiredLevels ->
                    it("[${user.id}] has levels ${requiredLevels}") {
                        val actualLevels = securityService.ideaAccessLevels(idea, user)
                        actualLevels.shouldBeRight(requiredLevels)
                    }
                }

                it("${userD.id} can't get access to document (notfound)") {
                    val actualLevels = securityService.ideaAccessLevels(idea, userD)
                    actualLevels.shouldBeLeft()
                }
            }

            describe("group has idea created by [userB] and assigned to himself ([userB])") {
                val idea = idea(userB, group, Some(userB))
                val requiredLevels =
                    setOf(IdeaAccessLevel.GROUP_MEMBER, IdeaAccessLevel.AUTHOR, IdeaAccessLevel.ASSIGNEE)
                it("[${userB.id}] has levels $requiredLevels") {
                    val actualLevels = securityService.ideaAccessLevels(idea, userB)
                    actualLevels.shouldBeRight(requiredLevels)
                }
            }

            describe("author [userD] is not member of idea's group (was kicked or idea was moved") {
                val idea = idea(userD, group, None)
                it("[${userD.id}] get document not found error ") {
                    val actualLevels = securityService.ideaAccessLevels(idea, userD);
                    actualLevels.shouldBeLeft()
                }
            }
        }

        describe("groupmember security level, groupmember belongs to [userB]") {
            val groupMember = GroupMember.of(group.id, userB, GroupMemberRole.MEMBER)

            table(
                headers("user", "accessLevels"),
                row(
                    userA,
                    setOf(
                        GroupMemberAccessLevel.GROUP_MEMBER,
                        GroupMemberAccessLevel.GROUP_ADMIN,
                        GroupMemberAccessLevel.HIM_SELF
                    )
                ),
                row(userB, setOf(GroupMemberAccessLevel.GROUP_MEMBER, GroupMemberAccessLevel.HIM_SELF)),
                row(userC, setOf(GroupMemberAccessLevel.GROUP_MEMBER)),
            ).forAll {user, requiredLevels ->
                it("${user.displayName} has levels $requiredLevels") {
                    val actualLevels = securityService.groupMemberAccessLevels(groupMember, group.id, user)
                    actualLevels.shouldBeRight(requiredLevels)
                }

            }

            it("${userD.id} can't get access to document (notfound)") {
                val actualLevels = securityService.groupMemberAccessLevels(groupMember, group.id, userD)
                actualLevels.shouldBeLeft()
            }
        }
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
        summary = "Test idea",
        description = "[b]some[/b] description",
        descriptionPlainText = "some description",
        link = "",
        assignee = assignee.map {it.id}.getOrElse {""},
        implemented = false,
        author = creator.id,
        voters = voters.map {it.id},
        archived = false,
        deleted = false
    )
}