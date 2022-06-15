package idel.api

import idel.domain.*

class ApiSecurity(
    val user :  UserSecurity,
    val group : GroupSecurity,
    val idea : IdeaSecurity,
    val groupMember : GroupMemberSecurity,
    val invite: InviteSecurity)



