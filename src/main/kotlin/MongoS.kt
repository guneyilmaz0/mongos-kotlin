package net.guneyilmaz0.mongos4k

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoException
import com.mongodb.client.ChangeStreamIterable
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import net.guneyilmaz0.mongos4k.exceptions.MongoSConnectionException
import org.bson.Document
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Professional MongoDB client with enhanced features and optimizations.
 * This class provides constructors to connect to a MongoDB server and initialize a specific database.
 * It also offers methods to access collections, watch for changes, and switch to other databases on the same client.
 *
 * Key Features:
 * - Optimized connection pooling and settings
 * - Enhanced error handling and logging
 * - Resource management with proper cleanup
 * - Connection health monitoring
 * - Professional configuration defaults
 *
 * @property mongo The underlying [MongoClient] instance.
 * @author guneyilmaz0
 * @throws MongoException if the connection to MongoDB fails during initialization.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class MongoS : Database, AutoCloseable {
    private val mongo: MongoClient
    private val logger = LoggerFactory.getLogger(MongoS::class.java)

    companion object {
        /**
         * Lightweight client defaults: short timeouts and a modest pool, with no eagerly opened
         * connections. Applied *before* the connection string so options in the URI always win.
         * No socket read timeout is set, so change streams keep working.
         */
        private fun clientSettings(connectionString: ConnectionString?): MongoClientSettings {
            val builder =
                MongoClientSettings.builder()
                    .applyToConnectionPoolSettings {
                        it.maxSize(20).maxConnectionIdleTime(30, TimeUnit.SECONDS).maxWaitTime(5, TimeUnit.SECONDS)
                    }
                    .applyToSocketSettings { it.connectTimeout(5, TimeUnit.SECONDS) }
                    .applyToClusterSettings { it.serverSelectionTimeout(5, TimeUnit.SECONDS) }
            if (connectionString != null) builder.applyConnectionString(connectionString)
            return builder.build()
        }

        /** Removes credentials from a URI so it can be logged or put in a message. */
        private fun redact(uri: String): String = uri.replace(Regex("//[^/@]*@"), "//***@")
    }

    /**
     * Connects to `host:port` and binds to [dbName].
     *
     * @throws MongoSConnectionException if the connection fails.
     */
    constructor(host: String, port: Int, dbName: String) {
        mongo = connect(ConnectionString("mongodb://$host:$port"), dbName, "$host:$port")
    }

    /**
     * Connects using a MongoDB URI (e.g. `mongodb://user:pass@host:port/admin`) and binds to [dbName].
     *
     * @throws MongoSConnectionException if the connection fails.
     */
    constructor(uri: String, dbName: String) {
        mongo = connect(ConnectionString(uri), dbName, redact(uri))
    }

    /**
     * Connects to `localhost:27017` and binds to [dbName].
     *
     * @throws MongoSConnectionException if the connection fails.
     */
    constructor(dbName: String) {
        mongo = connect(null, dbName, "localhost:27017")
    }

    /** Internal: shares an existing client (see [getAnotherMongoSDatabase]). */
    private constructor(sharedClient: MongoClient, dbName: String) {
        mongo = sharedClient
        super.init(mongo.getDatabase(dbName))
    }

    private fun connect(connectionString: ConnectionString?, dbName: String, target: String): MongoClient {
        val client =
            try {
                MongoClients.create(clientSettings(connectionString))
            } catch (e: Exception) {
                throw MongoSConnectionException("Error initializing MongoDB client for $target", e)
            }
        try {
            super.init(client.getDatabase(dbName))
            super.isConnected()
        } catch (e: Exception) {
            client.close()
            if (e is MongoSConnectionException) throw MongoSConnectionException("Failed to connect to $target", e.cause ?: e)
            throw MongoSConnectionException("Error connecting to $target", e)
        }
        logger.info("Connected to {}, database: {}", target, dbName)
        return client
    }

    /**
     * Gets a [MongoCollection] instance for the specified collection name from the current database.
     * Includes performance logging for monitoring usage patterns.
     *
     * @param collectionName The name of the collection.
     * @return A [MongoCollection] object for the specified collection.
     */
    fun getCollection(collectionName: String): MongoCollection<Document> {
        return database.getCollection(collectionName)
    }

    /**
     * Gets a [Database] instance for another database using the same optimized [MongoClient].
     * Reuses the existing client connection for better resource efficiency.
     *
     * @param databaseName The name of the other database.
     * @return A new [Database] object initialized for the specified database.
     *         Note: This new Database instance will not be a MongoS instance but will share the same client.
     */
    fun getAnotherDatabase(databaseName: String): Database {
        logger.debug("Creating Database instance for: $databaseName")
        val newDb = Database()
        newDb.init(mongo.getDatabase(databaseName))
        return newDb
    }

    /**
     * Creates a new MongoS instance for another database using the same client connection.
     * Closing any instance sharing the client closes it for all of them.
     *
     * @param databaseName The name of the other database.
     * @return A new [MongoS] object initialized for the specified database, sharing the same client.
     */
    fun getAnotherMongoSDatabase(databaseName: String): MongoS {
        logger.debug("Creating MongoS instance for: $databaseName")
        return MongoS(mongo, databaseName)
    }

    /**
     * Watches a collection for changes with enhanced error handling and logging.
     * Perfect for real-time applications and data synchronization.
     *
     * @param collectionName The name of the collection to watch.
     * @return A [ChangeStreamIterable] which can be used to iterate over change events.
     */
    fun watchCollection(collectionName: String): ChangeStreamIterable<Document> {
        logger.debug("Setting up change stream watch for collection: $collectionName")
        return try {
            getCollection(collectionName).watch().also {
                logger.info("Change stream established for collection: $collectionName")
            }
        } catch (e: Exception) {
            logger.error("Failed to setup change stream for collection: $collectionName", e)
            throw MongoSConnectionException("Failed to setup change stream for collection: $collectionName", e)
        }
    }

    /**
     * Checks the health of the MongoDB connection and logs connection statistics.
     * Provides detailed information about connection pool status.
     *
     * @return True if the connection is healthy, false otherwise.
     */
    fun checkConnectionHealth(): Boolean {
        return try {
            val isHealthy = super.isConnected()
            if (isHealthy) {
                logger.debug("MongoDB connection health check passed")
            } else {
                logger.warn("MongoDB connection health check failed")
            }
            isHealthy
        } catch (e: Exception) {
            logger.error("MongoDB connection health check error", e)
            false
        }
    }

    /**
     * Gets information about the current database including collections and basic statistics.
     * Useful for monitoring and administration purposes.
     *
     * @return A map containing database information and statistics.
     */
    fun getDatabaseInfo(): Map<String, Any> {
        return try {
            val collections = getCollections()
            val info =
                mutableMapOf<String, Any>(
                    "name" to database.name,
                    "collections" to collections,
                    "collectionCount" to collections.size,
                )

            // Add collection statistics
            val collectionStats =
                collections.associateWith { collectionName ->
                    try {
                        getCollectionStats(collectionName)
                    } catch (e: Exception) {
                        logger.warn("Failed to get stats for collection $collectionName", e)
                        emptyMap<String, Any>()
                    }
                }
            info["collectionStats"] = collectionStats

            logger.debug("Retrieved database info for: ${database.name}")
            info.toMap() // Return immutable map
        } catch (e: Exception) {
            logger.error("Failed to get database info", e)
            mapOf("error" to (e.message ?: "Unknown error"), "name" to database.name)
        }
    }

    /**
     * Closes the underlying MongoDB client and releases any resources with proper cleanup.
     * It's important to call this method when the MongoS instance is no longer needed
     * to prevent resource leaks. This method is idempotent and safe to call multiple times.
     */
    override fun close() {
        try {
            logger.info("Closing MongoS client for database: ${database.name}")
            mongo.close()
            logger.info("MongoS client closed successfully")
        } catch (e: Exception) {
            logger.error("Error occurred while closing MongoS client", e)
        }
    }

    /**
     * Provides a string representation of the MongoS instance for debugging and logging.
     */
    override fun toString(): String {
        return "MongoS(database=${database.name})"
    }
}
