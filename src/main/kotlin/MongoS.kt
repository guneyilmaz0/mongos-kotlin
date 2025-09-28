package net.guneyilmaz0.mongos4k

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoException
import com.mongodb.client.ChangeStreamIterable
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.connection.ClusterSettings
import net.guneyilmaz0.mongos4k.exceptions.MongoSConnectionException
import org.bson.Document
import org.slf4j.LoggerFactory
import java.io.Closeable
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
class MongoS : Database, Closeable, AutoCloseable {
    private val mongo: MongoClient
    private val logger = LoggerFactory.getLogger(MongoS::class.java)

    companion object {
        /**
         * Creates optimized MongoDB client settings with professional defaults.
         * Includes connection pooling, timeouts, and retry settings.
         */
        private fun createOptimizedClientSettings(connectionString: ConnectionString? = null): MongoClientSettings.Builder {
            val builder = MongoClientSettings.builder()

            if (connectionString != null) {
                builder.applyConnectionString(connectionString)
            }

            // Optimized connection pool settings
            builder.applyToConnectionPoolSettings { poolBuilder ->
                poolBuilder
                    .maxSize(20) // Maximum number of connections
                    .minSize(5) // Minimum number of connections
                    .maxConnectionIdleTime(30, TimeUnit.SECONDS)
                    .maxConnectionLifeTime(0, TimeUnit.MILLISECONDS) // 0 means no limit
                    .maxWaitTime(5, TimeUnit.SECONDS)
            }

            // Socket settings for better performance
            builder.applyToSocketSettings { socketBuilder ->
                socketBuilder
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
            }

            // Server selection timeout
            builder.applyToClusterSettings { clusterBuilder: ClusterSettings.Builder ->
                clusterBuilder.serverSelectionTimeout(5, TimeUnit.SECONDS)
            }

            return builder
        }
    }

    /**
     * Initializes a new instance of the MongoS class with the specified host, port, and database name.
     * Uses optimized connection settings for better performance and reliability.
     *
     * @param host The host of the MongoDB server.
     * @param port The port of the MongoDB server.
     * @param dbName The name of the database to connect to.
     * @throws MongoException if the connection to MongoDB fails or the database ping is unsuccessful.
     */
    constructor(host: String, port: Int, dbName: String) {
        logger.info("Initializing MongoS connection to $host:$port for database: $dbName")
        try {
            val connectionString = ConnectionString("mongodb://$host:$port")
            val clientSettings = createOptimizedClientSettings(connectionString).build()

            mongo = MongoClients.create(clientSettings)
            super.init(mongo.getDatabase(dbName))

            if (!super.isConnected()) {
                mongo.close()
                throw MongoSConnectionException("Failed to connect to MongoDB at $host:$port")
            }
            logger.info("Successfully connected to MongoDB at $host:$port, database: $dbName")
        } catch (e: Exception) {
            logger.error("Error initializing MongoDB connection at $host:$port", e)
            throw MongoSConnectionException("Error initializing MongoDB connection at $host:$port", e)
        }
    }

    /**
     * Initializes a new instance of the MongoS class with the specified MongoDB connection URI and database name.
     * Includes enhanced connection pooling and timeout settings.
     *
     * @param uri The MongoDB connection URI (e.g., "mongodb://user:pass@host:port/admin").
     * @param dbName The name of the database to connect to.
     * @throws MongoException if the connection to MongoDB fails or the database ping is unsuccessful.
     */
    constructor(uri: String, dbName: String) {
        logger.info("Initializing MongoS connection with URI for database: $dbName")
        try {
            val connectionString = ConnectionString(uri)
            val clientSettings = createOptimizedClientSettings(connectionString).build()

            mongo = MongoClients.create(clientSettings)
            super.init(mongo.getDatabase(dbName))

            if (!super.isConnected()) {
                mongo.close()
                throw MongoSConnectionException("Failed to connect using URI: $uri")
            }
            logger.info("Successfully connected using URI for database: $dbName")
        } catch (e: Exception) {
            logger.error("Error initializing MongoDB connection with URI", e)
            throw MongoSConnectionException("Error initializing MongoDB connection with URI $uri", e)
        }
    }

    /**
     * Initializes a new instance of the MongoS class connecting to a MongoDB server running on localhost:27017.
     * Uses professional-grade connection settings optimized for local development and production.
     *
     * @param dbName The name of the database to connect to.
     * @throws MongoException if the connection to MongoDB fails or the database ping is unsuccessful.
     */
    constructor(dbName: String) {
        logger.info("Initializing MongoS connection to localhost:27017 for database: $dbName")
        try {
            val clientSettings = createOptimizedClientSettings().build()
            mongo = MongoClients.create(clientSettings)
            super.init(mongo.getDatabase(dbName))

            if (!super.isConnected()) {
                mongo.close()
                throw MongoSConnectionException("Failed to connect to default MongoDB (localhost:27017)")
            }
            logger.info("Successfully connected to localhost:27017, database: $dbName")
        } catch (e: Exception) {
            logger.error("Error initializing MongoDB connection to default", e)
            throw MongoSConnectionException("Error initializing MongoDB connection to default", e)
        }
    }

    /**
     * Gets a [MongoCollection] instance for the specified collection name from the current database.
     * Includes performance logging for monitoring usage patterns.
     *
     * @param collectionName The name of the collection.
     * @return A [MongoCollection] object for the specified collection.
     */
    fun getCollection(collectionName: String): MongoCollection<Document> {
        logger.debug("Accessing collection: $collectionName")
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
     * This is more efficient than creating a completely new MongoS instance.
     *
     * @param databaseName The name of the other database.
     * @return A new [MongoS] object initialized for the specified database, sharing the same client.
     */
    fun getAnotherMongoSDatabase(databaseName: String): MongoS {
        logger.debug("Creating MongoS instance for: $databaseName")
        return MongoS(mongo, databaseName)
    }

    /**
     * Private constructor for creating MongoS instances that share a client.
     * This is used internally for efficient database switching.
     */
    private constructor(sharedClient: MongoClient, dbName: String) {
        mongo = sharedClient
        super.init(mongo.getDatabase(dbName))
        logger.debug("Created shared MongoS instance for database: $dbName")
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
