package net.guneyilmaz0.mongos4k

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.ChangeStreamIterable
import org.bson.Document

/**
 * This class represents a MongoDB client.
 * It provides methods to get a collection, watch a collection, and get another database.
 *
 * @property mongo the MongoDB client instance.
 * @author guneyilmaz0
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class MongoS : Database {
    private var mongo: MongoClient

    /**
     * Initializes a new instance of the MongoS class with the specified host, port, and database name.
     *
     * @param host the host of the MongoDB server.
     * @param port the port of the MongoDB server.
     * @param dbName the name of the database.
     */
    constructor(host: String, port: Int, dbName: String) {
        mongo = MongoClients.create("mongodb://$host:$port")
        init(mongo.getDatabase(dbName))
        isConnected()
    }

    /**
     * Initializes a new instance of the MongoS class with the specified URI and database name.
     *
     * @param uri the URI of the MongoDB server.
     * @param dbName the name of the database.
     */
    constructor(uri: String, dbName: String) {
        mongo = MongoClients.create(uri)
        init(mongo.getDatabase(dbName))
        isConnected()
    }

    /**
     * Initializes a new instance of the MongoS class with the specified database name.
     *
     * @param dbName the name of the database.
     */
    constructor(dbName: String) {
        mongo = MongoClients.create()
        init(mongo.getDatabase(dbName))
        isConnected()
    }

    /**
     * Gets a collection from the database.
     *
     * @param collection the name of the collection.
     * @return a MongoCollection object.
     */
    fun getCollection(collection: String): MongoCollection<Document> =
        database.getCollection(collection)

    /**
     * Gets another database.
     *
     * @param dataBase the name of the database.
     * @return a Database object.
     */
    fun getAnotherDatabase(dataBase: String): Database = Database().apply {
        init(mongo.getDatabase(dataBase))
    }

    /**
     * Watches a collection for changes.
     *
     * @param collection the name of the collection.
     * @return a ChangeStreamIterable object.
     */
    fun watchCollection(collection: String): ChangeStreamIterable<Document> = getCollection(collection).watch()

}