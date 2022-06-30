package idel.api


import arrow.core.Either
import arrow.core.continuations.either
import idel.domain.EntityNotFound
import idel.domain.fTransaction
import idel.infrastructure.repositories.psql.*

import liquibase.integration.spring.SpringLiquibase
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.annotation.PostConstruct

/**
 * Share config of application.
 * Passwords shares only in test mode (see
 */
@RestController
@RequestMapping("/storage")
class StorageController(database : Database ) {

    /**
     * Order is matter.
     */
    val tables = transaction {
        listOf(
            JoinRequestsTable,
            InvitesTable,
            IdeasTable,
            GroupMembersTable,
            GroupsTable,
            UserSettingsTable,
            UsersTable
        )
    }

    val tableByName = tables.associateBy {it.tableName}

    lateinit var x: SpringLiquibase

    private val log = KotlinLogging.logger {}

    @Value("\${testmode}")
    private var testMode = false

    @PostConstruct
    fun postInit() {
        if (testMode) {
            log.warn("Test mode! Don't use in production - you can loose all your data!")
        }
    }


    @DeleteMapping("/{type}")
    fun deleteEntities(@PathVariable type: String): ResponseEntity<DataOrError<Int>> {
        if (!testMode) {
            ResponseDataOrError.notFound()
        }

        val result =
            fTransaction {
                wrappedSQLStatementFlatten {
                    either.eager {
                        val table: Table =
                            Either
                                .fromNullable(tableByName[type])
                                .mapLeft {EntityNotFound("entity",type)}.bind()
                        table.deleteAll()
                    }
                }
        }

        return DataOrError.fromEither(result, log)
    }

    @DeleteMapping("/flush")
    fun flush(): ResponseEntity<DataOrError<Int>> {
        val result = fTransaction {
            wrappedSQLStatement {
                tables.fold(0) {acc, table -> acc + table.deleteAll()}
            }
        }
        return DataOrError.fromEither(result,log)
    }
}