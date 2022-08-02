package idel.infrastructure

import idel.domain.*
import idel.domain.SecurityService
import idel.infrastructure.repositories.psql.*
import org.jetbrains.exposed.sql.Database
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.thymeleaf.TemplateEngine
import javax.sql.DataSource

@Configuration
class DomainConfiguration(

) {
    @Autowired
    private lateinit var dataSource: DataSource

    @Bean
    fun exposedDatabase(): Database {
        return Database.connect(dataSource)
    }

    @Bean
    fun groupRepository(): GroupRepository {
        return GroupPgRepository()
    }

    @Bean
    fun groupSecurity(
        userRepository: UserRepository,
        securityService: SecurityService,
        groupRepository: GroupRepository
    ): GroupSecurity {
        return GroupSecurity(userRepository, securityService, groupRepository)
    }

    @Bean
    fun userRepository(): UserRepository {
        return UserPgRepository()
    }

    @Bean
    fun userSecurity(): UserSecurity {
        return UserSecurity()
    }


    @Bean
    fun userService(
        userRepository: UserRepository,
        userSettingsRepository: UserSettingsRepository,
        groupMembershipService: GroupMembershipService
    ): UserService {
        return UserService(userRepository, userSettingsRepository, groupMembershipService)
    }


    @Bean
    fun userSettingRepository(): UserSettingsRepository {
        return UserSettingsPgRepository()
    }

    @Bean
    fun joinRequestRepository(): JoinRequestRepository {
        return JoinRequestPgRepository()
    }

    @Bean
    fun ideaRepository(): IdeaPgRepository {
        return IdeaPgRepository()
    }

    @Bean
    fun ideaSecurity(securityService: SecurityService, ideaRepository: IdeaRepository): IdeaSecurity {
        return IdeaSecurity(securityService, ideaRepository)
    }

    @Bean
    fun inviteRepository(): InviteRepository {
        return InvitePgRepository()
    }

    @Bean
    fun inviteSecurity(inviteRepository: InviteRepository, groupSecurity: GroupSecurity): InviteSecurity {
        return InviteSecurity(inviteRepository, groupSecurity)
    }

    @Bean
    fun groupMemberRepository(): GroupMemberRepository {
        return GroupMemberPgRepository()
    }

    @Bean
    fun commentRepository(): CommentRepository {
        return CommentPgRepository()
    }

    @Bean
    fun groupService(groupMemberRepository: GroupMemberRepository): GroupService {
        return GroupService(groupMemberRepository)
    }

    @Bean
    fun groupMemberSecurity(
        securityService: SecurityService,
        groupMemberRepository: GroupMemberRepository
    ): GroupMemberSecurity {
        return GroupMemberSecurity(securityService, groupMemberRepository)
    }

    @Bean
    fun groupMembershipService(
        groupRepository: GroupRepository,
        userRepository: UserRepository,
        joinRequestRepository: JoinRequestRepository,
        inviteRepository: InviteRepository,
        groupMemberRepository: GroupMemberRepository,
        emailSender: EmailSender

    ): GroupMembershipService {
        return GroupMembershipService(
            groupRepository = groupRepository,
            userRepository = userRepository,
            joinRequestRepository = joinRequestRepository,
            inviteRepository = inviteRepository,
            groupMemberRepository = groupMemberRepository,
            emailSender = emailSender
        )
    }


    @Bean
    fun securityService(
        groupMemberRepository: GroupMemberRepository,
        groupRepository: GroupRepository
    ): SecurityService {
        return SecurityService(groupMemberRepository, groupRepository)
    }

    @Bean
    fun emailSender(
        userRepository: UserRepository,
        thymeleafEngine: TemplateEngine,
        mailSender: JavaMailSender
    ): EmailSender {
        return SmtpEmailSender(mailSender, thymeleafEngine)
    }
}