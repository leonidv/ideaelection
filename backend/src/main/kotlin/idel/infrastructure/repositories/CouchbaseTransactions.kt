package idel.infrastructure.repositories

import arrow.core.Either
import com.couchbase.client.java.Cluster
import com.couchbase.transactions.AttemptContext
import com.couchbase.transactions.Transactions
import com.couchbase.transactions.error.TransactionFailed
import mu.KotlinLogging

class CouchbaseTransactions(val cluster: Cluster) {
    val log = KotlinLogging.logger {}

    private val transaction = Transactions.create(cluster)

    fun transaction(operations : (ctx : AttemptContext) -> Unit) : Either<Exception, Unit> {
        return try {
            val txResult = transaction.run {ctx -> operations(ctx)}
            log.trace {"transaction result: ${txResult}"}
            Either.right(Unit)
        } catch(e : TransactionFailed) {
            log.warn {"transaction failed, ${e.result().log().logs().joinToString(separator = "\n")}"}
            Either.left(e)
        } catch (e : Exception) {
            Either.left(e)
        }
    }

}