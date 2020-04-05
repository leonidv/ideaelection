package ideael.domain

typealias UserId = String

data class Voter(
    val email : UserId,
    val name : String,
    val profilePhoto : String) {

    companion object Voters {
        val DUMMY = Voter("dummy@mail","dummy dummy","none")
    }

    fun vote(idea : Idea) : Idea {
        return idea.addVote(this.id())
    }

    fun tookVote(idea : Idea) : Idea {
        return idea.removeVote(this.id())
    }

    fun id() : UserId {
        return email
    }
}
