package net.guneyilmaz0.mongos4k

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoException
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.ChangeStreamIterable
import net.guneyilmaz0.mongos4k.config.MongoSConfig
import net.guneyilmaz0.mongos4k.exceptions.MongoSConnectionException
import net.guneyilmaz0.mongos4k.operations.MongoSBatchOperations
import net.guneyilmaz0.mongos4k.operations.MongoSIndexManager
import net.guneyilmaz0.mongos4k.operations.MongoSTransactionManager
import net.guneyilmaz0.mongos4k.monitoring.MongoSPerformanceMonitor
import net.guneyilmaz0.mongos4k.utilities.MongoSBackupUtility
import net.guneyilmaz0.mongos4k.validation.MongoSValidator
import org.bson.Document

/**
 * Represents a MongoDB client, extending [Database] functionalities.
 * This class provides constructors to connect to a MongoDB server and initialize a specific database.
 * It also offers methods to access collections, watch for changes, and switch to other databases on the same client.
 *
 * @property mongo The underlying [MongoClient] instance.
 * @author guneyilmaz0
 * @throws MongoSConnectionException if the connection to MongoDB fails during initialization.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class MongoS : Database {
    val mongo: MongoClient
    private val config: MongoSConfig?
    
    // Lazy initialization of utility classes
    val batchOperations: MongoSBatchOperations by lazy { MongoSBatchOperations.create(this) }
    val indexManager: MongoSIndexManager by lazy { MongoSIndexManager.create(this) }
    val transactionManager: MongoSTransactionManager by lazy { MongoSTransactionManager.create(this) }
    val performanceMonitor: MongoSPerformanceMonitor by lazy { MongoSPerformanceMonitor.create(this) }
    val backupUtility: MongoSBackupUtility by lazy { MongoSBackupUtility.create(this) }

    /**
     * Initializes a new instance of the MongoS class with the specified host, port, and database name.
     *
     * @param host The host of the MongoDB server.
     * @param port The port of the MongoDB server.
     * @param dbName The name of the database to connect to.
     * @throws MongoSConnectionException if the connection to MongoDB fails or the database ping is unsuccessful.
     */
    constructor(host: String, port: Int, dbName: String) {
        MongoSValidator.validateHostPort(host, port)
        MongoSValidator.validateDatabaseName(dbName)
        
        this.config = null
        try {
            mongo = MongoClients.create("mongodb://$host:$port")
            super.init(mongo.getDatabase(dbName))
            if (!super.isConnected()) {
                mongo.close()
                throw MongoSConnectionException("Failed to connect to MongoDB or ping was unsuccessful at $host:$port")
            }
        } catch (e: MongoSConnectionException) {
            throw e
        } catch (e: Exception) {
            throw MongoSConnectionException("Error initializing MongoDB connection at $host:$port: ${e.message}", e)
        }
    }

    /**
     * Initializes a new instance of the MongoS class with the specified MongoDB connection URI and database name.
     *
     * @param uri The MongoDB connection URI (e.g., "mongodb://user:pass@host:port/admin").
     * @param dbName The name of the database to connect to.
     * @throws MongoSConnectionException if the connection to MongoDB fails or the database ping is unsuccessful.
     */
    constructor(uri: String, dbName: String) {
        MongoSValidator.validateConnectionUri(uri)
        MongoSValidator.validateDatabaseName(dbName)
        
        this.config = null
        try {
            val connectionString = ConnectionString(uri)
            val clientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build()
            mongo = MongoClients.create(clientSettings)
            super.init(mongo.getDatabase(dbName))
            if (!super.isConnected()) {
                mongo.close()
                throw MongoSConnectionException("Failed to connect to MongoDB or ping was unsuccessful using URI: $uri")
            }
        } catch (e: MongoSConnectionException) {
            throw e
        } catch (e: Exception) {
            throw MongoSConnectionException("Error initializing MongoDB connection with URI $uri: ${e.message}", e)
        }
    }

    /**
     * Initializes a new instance of the MongoS class connecting to a MongoDB server running on localhost:27017.
     *
     * @param dbName The name of the database to connect to.
     * @throws MongoSConnectionException if the connection to MongoDB fails or the database ping is unsuccessful.
     */
    constructor(dbName: String) {
        MongoSValidator.validateDatabaseName(dbName)
        
        this.config = null
        try {
            mongo = MongoClients.create()
            super.init(mongo.getDatabase(dbName))
            if (!super.isConnected()) {
                mongo.close()
                throw MongoSConnectionException("Failed to connect to default MongoDB (localhost:27017) or ping was unsuccessful.")
            }
        } catch (e: MongoSConnectionException) {
            throw e
        } catch (e: Exception) {
            throw MongoSConnectionException("Error initializing MongoDB connection to default (localhost:27017): ${e.message}", e)
        }
    }

    /**
     * Initializes a new instance of the MongoS class with advanced configuration.
     *
     * @param config The MongoDB configuration.
     * @param dbName The name of the database to connect to.
     * @throws MongoSConnectionException if the connection to MongoDB fails or the database ping is unsuccessful.
     */
    constructor(config: MongoSConfig, dbName: String) {
        MongoSValidator.validateDatabaseName(dbName)
        
        this.config = config
        try {
            mongo = MongoClients.create(config.toMongoClientSettings())
            super.init(mongo.getDatabase(dbName))
            if (!super.isConnected()) {
                mongo.close()
                throw MongoSConnectionException("Failed to connect to MongoDB or ping was unsuccessful with custom config")
            }
        } catch (e: MongoSConnectionException) {
            throw e
        } catch (e: Exception) {
            throw MongoSConnectionException("Error initializing MongoDB connection with custom config: ${e.message}", e)
        }
    }

    /**
     * Gets the configuration used for this connection.
     *
     * @return The MongoSConfig instance if custom config was used, null otherwise.
     */
    fun getConfig(): MongoSConfig? = config

    /**
     * Gets connection statistics and health information.
     *
     * @return A map containing connection statistics.
     */
    fun getConnectionStats(): Map<String, Any> {
        return try {
            val stats = mutableMapOf<String, Any>()
            stats["connected"] = isConnected()
            stats["database"] = database.name
            
            // Get server status if available
            try {
                val serverStatus = database.runCommand(Document("serverStatus", 1))
                stats["serverVersion"] = serverStatus.getString("version")
                stats["uptime"] = serverStatus.getInteger("uptime")
                serverStatus.get("connections")?.let { stats["connections"] = it }
            } catch (e: Exception) {
                stats["serverStatus"] = "Unable to retrieve: ${e.message}"
            }
            
            stats
        } catch (e: Exception) {
            mapOf("error" to "Failed to get connection stats: ${e.message}")
        }
    }

    /**
     * Gets a [MongoCollection] instance for the specified collection name from the current database.
     *
     * @param collectionName The name of the collection.
     * @return A [MongoCollection] object for the specified collection.
     */
    fun getCollection(collectionName: String): MongoCollection<Document> {
        MongoSValidator.validateCollectionName(collectionName)
        return database.getCollection(collectionName)
    }

    /**
     * Gets a [Database] instance for another database using the same [MongoClient].
     *
     * @param databaseName The name of the other database.
     * @return A new [Database] object initialized for the specified database.
     *         Note: This new Database instance will not be a MongoS instance.
     */
    fun getAnotherDatabase(databaseName: String): Database {
        val newDb = Database()
        newDb.init(mongo.getDatabase(databaseName))
        return newDb
    }

    /**
     * Watches a collection for changes.
     *
     * @param collectionName The name of the collection to watch.
     * @return A [ChangeStreamIterable] which can be used to iterate over change events.
     */
    fun watchCollection(collectionName: String): ChangeStreamIterable<Document> =
        getCollection(collectionName).watch()

    /**
     * Closes the underlying MongoDB client and releases any resources.
     * It's important to call this method when the MongoS instance is no longer needed
     * to prevent resource leaks.
     */
    fun close() {
        mongo.close()
    }
}