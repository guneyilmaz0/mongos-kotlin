package net.guneyilmaz0.mongos4k

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoException
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.ChangeStreamIterable
import org.bson.Document

/**
 * Represents a MongoDB client, extending [Database] functionalities.
 * This class provides constructors to connect to a MongoDB server and initialize a specific database.
 * It also offers methods to access collections, watch for changes, and switch to other databases on the same client.
 *
 * @property mongo The underlying [MongoClient] instance.
 * @author guneyilmaz0
 * @throws MongoException if the connection to MongoDB fails during initialization.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class MongoS : Database {
    private val mongo: MongoClient

    /**
     * Initializes a new instance of the MongoS class with the specified host, port, and database name.
     *
     * @param host The host of the MongoDB server.
     * @param port The port of the MongoDB server.
     * @param dbName The name of the database to connect to.
     * @throws MongoException if the connection to MongoDB fails or the database ping is unsuccessful.
     */
    constructor(host: String, port: Int, dbName: String) {
        try {
            mongo = MongoClients.create("mongodb://$host:$port")
            super.init(mongo.getDatabase(dbName))
            if (!super.isConnected()) {
                mongo.close()
                throw MongoException("Failed to connect to MongoDB or ping was unsuccessful at $host:$port")
            }
        } catch (e: Exception) {
            throw MongoException("Error initializing MongoDB connection at $host:$port: ${e.message}", e)
        }
    }

    /**
     * Initializes a new instance of the MongoS class with the specified MongoDB connection URI and database name.
     *
     * @param uri The MongoDB connection URI (e.g., "mongodb://user:pass@host:port/admin").
     * @param dbName The name of the database to connect to.
     * @throws MongoException if the connection to MongoDB fails or the database ping is unsuccessful.
     */
    constructor(uri: String, dbName: String) {
        try {
            val connectionString = ConnectionString(uri)
            val clientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build()
            mongo = MongoClients.create(clientSettings)
            super.init(mongo.getDatabase(dbName))
            if (!super.isConnected()) {
                mongo.close()
                throw MongoException("Failed to connect to MongoDB or ping was unsuccessful using URI: $uri")
            }
        } catch (e: Exception) {
            throw MongoException("Error initializing MongoDB connection with URI $uri: ${e.message}", e)
        }
    }

    /**
     * Initializes a new instance of the MongoS class connecting to a MongoDB server running on localhost:27017.
     *
     * @param dbName The name of the database to connect to.
     * @throws MongoException if the connection to MongoDB fails or the database ping is unsuccessful.
     */
    constructor(dbName: String) {
        try {
            mongo = MongoClients.create()
            super.init(mongo.getDatabase(dbName))
            if (!super.isConnected()) {
                mongo.close()
                throw MongoException("Failed to connect to default MongoDB (localhost:27017) or ping was unsuccessful.")
            }
        } catch (e: Exception) {
            throw MongoException("Error initializing MongoDB connection to default (localhost:27017): ${e.message}", e)
        }
    }

    /**
     * Gets a [MongoCollection] instance for the specified collection name from the current database.
     *
     * @param collectionName The name of the collection.
     * @return A [MongoCollection] object for the specified collection.
     */
    fun getCollection(collectionName: String): MongoCollection<Document> =
        database.getCollection(collectionName)

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