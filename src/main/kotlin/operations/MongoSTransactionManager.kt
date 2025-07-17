package net.guneyilmaz0.mongos4k.operations

import com.mongodb.TransactionOptions
import com.mongodb.WriteConcern
import com.mongodb.ReadConcern
import com.mongodb.ReadPreference
import com.mongodb.client.ClientSession
import com.mongodb.client.TransactionBody
import net.guneyilmaz0.mongos4k.MongoS
import net.guneyilmaz0.mongos4k.exceptions.MongoSOperationException
import kotlinx.coroutines.runBlocking

/**
 * Transaction manager for MongoDB operations.
 * Provides methods to execute operations within transactions for ACID compliance.
 *
 * @author guneyilmaz0
 */
class MongoSTransactionManager(private val mongoS: MongoS) {

    /**
     * Executes a transaction with the provided operations.
     *
     * @param T The return type of the transaction.
     * @param operations The operations to execute within the transaction.
     * @param options Optional transaction options.
     * @return The result of the transaction operations.
     * @throws MongoSOperationException If the transaction fails.
     */
    fun <T> withTransaction(
        operations: (ClientSession) -> T,
        options: TransactionOptions? = null
    ): T {
        return try {
            mongoS.mongo.startSession().use { session ->
                val transactionOptions = options ?: TransactionOptions.builder()
                    .readPreference(ReadPreference.primary())
                    .readConcern(ReadConcern.SNAPSHOT)
                    .writeConcern(WriteConcern.MAJORITY)
                    .build()

                session.withTransaction(
                    TransactionBody<T> { operations(session) },
                    transactionOptions
                )
            }
        } catch (e: Exception) {
            throw MongoSOperationException("Transaction failed", e)
        }
    }

    /**
     * Creates transaction options for specific use cases.
     */
    object TransactionOptionsBuilder {
        /**
         * Creates options for a read-committed transaction.
         */
        fun readCommitted(): TransactionOptions {
            return TransactionOptions.builder()
                .readPreference(ReadPreference.primary())
                .readConcern(ReadConcern.MAJORITY)
                .writeConcern(WriteConcern.MAJORITY)
                .build()
        }

        /**
         * Creates options for a snapshot isolation transaction.
         */
        fun snapshot(): TransactionOptions {
            return TransactionOptions.builder()
                .readPreference(ReadPreference.primary())
                .readConcern(ReadConcern.SNAPSHOT)
                .writeConcern(WriteConcern.MAJORITY)
                .build()
        }

        /**
         * Creates options for a linearizable transaction.
         */
        fun linearizable(): TransactionOptions {
            return TransactionOptions.builder()
                .readPreference(ReadPreference.primary())
                .readConcern(ReadConcern.LINEARIZABLE)
                .writeConcern(WriteConcern.MAJORITY)
                .build()
        }

        /**
         * Creates options for a fast transaction with eventual consistency.
         */
        fun eventual(): TransactionOptions {
            return TransactionOptions.builder()
                .readPreference(ReadPreference.secondary())
                .readConcern(ReadConcern.LOCAL)
                .writeConcern(WriteConcern.ACKNOWLEDGED)
                .build()
        }
    }

    companion object {
        /**
         * Creates a new transaction manager instance.
         *
         * @param mongoS The MongoS instance.
         * @return A new MongoSTransactionManager instance.
         */
        fun create(mongoS: MongoS): MongoSTransactionManager = MongoSTransactionManager(mongoS)
    }
}