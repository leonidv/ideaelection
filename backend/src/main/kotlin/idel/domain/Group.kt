package idel.domain


import arrow.core.Either
import arrow.core.Some
import arrow.core.computations.either
import arrow.core.flatten
import io.konform.validation.Validation
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.minLength
import java.time.LocalDateTime

/**
 * Editable properties of Group.
 *
 * See properties documentation in the [Group] class.
 */
interface IGroupEditableProperties {
    val name: String
    val description: String
    val entryMode: GroupEntryMode
    val entryQuestion: String
    val domainRestrictions: List<String>
    val logo: String
}

class GroupEditableProperties(
    override val name: String,
    override val description: String,
    override val logo: String,
    override val entryMode: GroupEntryMode,
    override val entryQuestion: String,
    override val domainRestrictions: List<String>
) : IGroupEditableProperties {
}

enum class GroupEntryMode {
    /**
     * Anybody can join a group without approving by group's administrator.
     */
    PUBLIC,

    /**
     * Anybody can try to join a group, but group's admin should accept join request.
     */
    CLOSED,

    /**
     * Only admin can invite users to group.
     */
    PRIVATE
}

enum class GroupState {
    /**
     * Group is available for working.
     */
    ACTIVE,

    /**
     * User deleted group and can't work with it.
     */
    DELETED
}

/**
 * Group is binding between users and ideas.
 *
 * Group allows to share ideas between specific users.
 *
 * ## Implementations notes
 *
 * [membersCount] and [ideasCount] are calculated fields. [GroupFactory] always init them in constant values
 * (see field's doc for details). Real values calculated in Repository, based on database.
 *
 */
class Group(
    /**
     * Generated idenitfier.
     */
    override val id: String,

    /**
     * Time of the creation
     */
    val ctime: LocalDateTime,

    /**
     * User which created the group
     */
    val creator: UserInfo,

    /**
     * State of group (active, archived, etc).
     */
    val state: GroupState,

    /**
     * Name of group.
     */
    override val name: String,

    /**
     * Short description of group.
     */
    override val description: String,

    /**
     * Logotype of group. Link to image.
     */
    override val logo: String,

    /**
     * Regulate how user's can join to group.
     */
    override val entryMode: GroupEntryMode,

    /**
     * Question for which a user should answer if want to join to the group
     */
    override val entryQuestion: String,

    /**
     * Restrictions by domain
     */
    override val domainRestrictions: List<String>,

    /**
     * Count of group's members.
     *
     * Calculated field. New group always have one member - creator, due this initial value is 1
     * (it's set in [GroupFactory.createGroup])
     */
    val membersCount : Int,

    /**
     * Count of group's ideas.
     *
     * New group never has any ideas. Initial value is 0 (it's set in [GroupFactory.createGroup]).
     */
    val ideasCount : Int,
    /**
     * Link for join to private groups.
     */
    val joiningKey: String,

    ) : IGroupEditableProperties, Identifiable {

    companion object {
        fun generateJoiningKey() = generateId()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Group

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


    private fun clone(
        name: String = this.name,
        logo: String = this.logo,
        description: String = this.description,
        state: GroupState = this.state,
        entryMode: GroupEntryMode = this.entryMode,
        entryQuestion: String = this.entryQuestion,
        domainRestrictions: List<String> = this.domainRestrictions,
        joiningKey: String = this.joiningKey

    ): Group {
        return Group(
            id = this.id,
            creator = this.creator,
            ctime = this.ctime,
            state = state,
            name = name,
            logo = logo,
            description = description,
            entryMode = entryMode,
            entryQuestion = entryQuestion,
            domainRestrictions = domainRestrictions,
            joiningKey = joiningKey,
            membersCount = this.membersCount,
            ideasCount = this.ideasCount
        )
    }

    fun update(properties: IGroupEditableProperties): Either<ValidationException, Group> {
        return GroupValidation.ifValid(properties) {
            clone(
                name = properties.name,
                logo = properties.logo,
                description = properties.description,
                entryMode = properties.entryMode,
                entryQuestion = properties.entryQuestion,
                domainRestrictions = properties.domainRestrictions
            )
        }
    }

    /**
     * Logical delete. Change status to [[GroupState.DELETED]] which is mean that user can't work with group.
     */
    fun delete(): Group {
        return this.clone(state = GroupState.DELETED)
    }

    fun isDeleted(): Boolean {
        return state == GroupState.DELETED
    }

    /**
     * Replace link to join with new.
     */
    fun regenerateJoiningKey() : Group {
        return clone(joiningKey = generateJoiningKey())
    }

    override fun toString(): String {
        return "Group(id='$id', ctime=$ctime, creator=$creator, state=$state, name='$name', " +
                "description='$description', logo='${logo.subSequence(0, 100)}', " +
                "entryMode=$entryMode, entryQuestion='$entryQuestion')"
    }
}


class GroupValidation {
    companion object : Validator<IGroupEditableProperties> {

        override val validation = Validation<IGroupEditableProperties> {
            IGroupEditableProperties::name {
                minLength(3)
                maxLength(255)
            }

            IGroupEditableProperties::description {
                minLength(1)
                maxLength(300)
            }

            IGroupEditableProperties::entryQuestion {
                maxLength(200)
            }

            IGroupEditableProperties::logo  {
                maxLength(150_000) // approx 100kb in base64
                isImageBase64(allowEmptyValue = false)
            }
        }
    }

}


class GroupFactory {
    fun createGroup(
        creator: UserInfo,
        properties: IGroupEditableProperties
    ): Either<ValidationException, Group> {
        return GroupValidation.ifValid(properties) {
            Group(
                id = generateId(),
                ctime = LocalDateTime.now(),
                creator = creator,
                state = GroupState.ACTIVE,
                name = properties.name,
                description = properties.description,
                logo = properties.logo,
                entryMode = properties.entryMode,
                entryQuestion = properties.entryQuestion,
                domainRestrictions = properties.domainRestrictions,
                joiningKey = Group.generateJoiningKey(),
                membersCount = 1, // because creator is always member of group
                ideasCount = 0
            )
        }
    }
}

enum class GroupOrdering {
    CTIME_ASC,
    CTIME_DESC,
    TITLE_ASC,
    TITLE_DESC
}


interface GroupRepository : BaseRepository<Group>, CouchbaseTransactionBaseRepository<Group> {

    /**
     * Load user's groups.
     *
     * In fact, the best place for this method is [GroupMemberRepository], but it's required too hard refactoring.
     */
    fun loadByUser(
        userId: String,
        pagination: Repository.Pagination,
        ordering: GroupOrdering
    ): Either<Exception, List<Group>>

    /**
     * Loads only available and visible groups.
     */
    fun loadOnlyAvailable(pagination: Repository.Pagination, ordering: GroupOrdering): Either<Exception, List<Group>>

    /**
     * Load group by link to join.
     */
    fun loadByJoiningKey(key: String): Either<Exception, Group>

}

/**
 * Read-only calculated metrics of group
 */
data class GroupMetrics(
    val groupId: String,
    val userId: String,
    val totalGroupsIdeas : Int,
    val totalGroupsMembers : Int,
    val userHasInvite : Boolean,
    val userHasJoinRequest: Boolean
)


/**
 * It's not good solution. This is methods should be in the [Group], but then repositories will be persisted into DB :)
 * Should be refactoring after migrating to PostgreSQL
 */
class GroupService(val groupMemberRepository: GroupMemberRepository) {

    private fun checkNoLastAdmin(groupId: String): Either<Exception, Boolean> {
        val pagination = Repository.Pagination(0, 2)
        val admins = groupMemberRepository.loadByGroup(
            groupId,
            pagination,
            roleFilter = Some(GroupMemberRole.GROUP_ADMIN)
        )

        return admins.map {it.size > 1}
    }

    fun changeRoleInGroup(groupId: String, userId: String, nextRole: GroupMemberRole): Either<Exception, GroupMember> {
        return either.eager<Exception, Either<Exception, GroupMember>> {
            val canChange = when (nextRole) {
                GroupMemberRole.GROUP_ADMIN -> Either.Right(true)
                GroupMemberRole.MEMBER -> checkNoLastAdmin(groupId)
            }.bind()

            if (canChange) {
                val member = groupMemberRepository.load(groupId, userId).bind()
                groupMemberRepository.changeRole(member.changeRole(nextRole))
            } else {
                Either.Left(InvalidOperation("Group should contains at least one admin, you try to remove last admin"))
            }

        }.flatten()
    }
}
