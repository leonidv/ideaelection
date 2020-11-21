package idel.infrastructure

import com.couchbase.client.java.Cluster
import com.couchbase.client.java.Collection
import idel.domain.*
import idel.infrastructure.repositories.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DomainConfiguration {


    @Autowired
    private lateinit var couchbaseCollection: Collection

    @Autowired
    private lateinit var couchbaseCluster: Cluster

    @Bean
    fun groupRepository() : GroupRepository {
        return GroupCouchbaseRepository(couchbaseCluster,couchbaseCollection)
    }

    @Bean
    fun userRepository() : UserRepository {
        return UserCouchbaseRepository(couchbaseCluster, couchbaseCollection)
    }

    @Bean
    fun joinRequestRepository() : JoinRequestRepository {
        return JoinRequestCouchbaseRepository(couchbaseCluster, couchbaseCollection)
    }

    @Bean
    fun ideaRepository() : IdeaCouchbaseRepository {
        return IdeaCouchbaseRepository(couchbaseCluster, couchbaseCollection)
    }

    @Bean
    fun inviteRepository() : InviteRepository {
        return InviteCouchbaseRepository(couchbaseCluster, couchbaseCollection)
    }

    @Bean
    fun groupMemberRepository() : GroupMemberRepository {
        return GroupMemberCouchbaseRepository(couchbaseCluster, couchbaseCollection)
    }

//    @Bean
//    fun groupMembershipRepository() : GroupMembershipRepository {
//        return GroupMembershipCouchbaseRepository(couchbaseCluster, couchbaseCollection)
//    }

    @Bean
    fun groupMembershipService(
            groupRepository: GroupRepository,
            userRepository: UserRepository,
            joinRequestRepository: JoinRequestRepository,
            inviteRepository: InviteRepository,
            groupMemberRepository: GroupMemberRepository

    ): GroupMembershipService {
        return GroupMembershipService(
                groupRepository = groupRepository,
                userRepository = userRepository,
                joinRequestRepository = joinRequestRepository,
                inviteRepository = inviteRepository,
                groupMemberRepository = groupMemberRepository
        )
    }

    @Bean
    fun securityService(groupMemberRepository: GroupMemberRepository) : SecurityService {
        return SecurityService(groupMemberRepository)
    }

}