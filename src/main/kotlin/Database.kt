package net.guneyilmaz0.mongos4k

import com.google.gson.Gson
import com.mongodb.MongoException
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.result.InsertManyResult
import kotlinx.coroutines.*
import org.bson.BsonDocument
import org.bson.BsonInt64
import org.bson.Document
import org.bson.conversions.Bson
import java.util.NoSuchElementException

/**
 * This class represents a MongoDB database.
 * It provides methods to initialize the database, set and get values, update and remove data, and check if data exists.
 *
 * @property database the MongoDB database instance.
 * @author guneyilmaz0
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
open class Database {

    companion object {
        const val KEY_FIELD = "key"
        const val VALUE_FIELD = "value"
        val gson: Gson = Gson()

        /**
         * Converts a POJO to a MongoDB Document.
         * If the object is a MongoSObject, it's converted via Gson.
         * Otherwise, it's returned as is (hoping it's a type MongoDB driver can handle).
         */
        private fun Any.toBsonValue(): Any {
            return if (this is MongoSObject) Document.parse(gson.toJson(this))
            else this
        }
    }

    lateinit var database: MongoDatabase
        private set

    /**
     * Initializes the database with the specified MongoDB database instance.
     *
     * @param database the MongoDB database instance.
     * @throws MongoException if there is an issue with database initialization (though unlikely with just assignment).
     */
    open fun init(database: MongoDatabase) {
        this.database = database
    }

    private fun createKeyFilter(key: Any): Bson = Filters.eq(KEY_FIELD, key)

    /**
     * Sets (upserts) a value in the specified collection with the provided key and value.
     * If a document with the key already exists, it will be replaced. Otherwise, a new document will be inserted.
     *
     * @param collection The collection name.
     * @param key The key for the value (will be used as _id or your custom key field).
     * @param value The value to set. Can be a primitive, a custom object, or a [MongoSObject].
     */
    fun set(collection: String, key: Any, value: Any): Unit = set(collection, key, value, async = false)

    /**
     * Sets (upserts) a value in the specified collection with the provided key and value.
     *
     * @param collection The collection name.
     * @param key The key for the value.
     * @param value The value to set.
     * @param async Whether the operation should be asynchronous.
     */
    fun set(collection: String, key: Any, value: Any, async: Boolean = false) {
        val documentToUpsert = Document(KEY_FIELD, key)
            .append(VALUE_FIELD, value.toBsonValue())

        if (async) {
            CoroutineScope(Dispatchers.IO).launch {
                upsertDocumentSuspend(collection, key, documentToUpsert)
            }
        } else {
            runBlocking(Dispatchers.IO) {
                upsertDocumentSuspend(collection, key, documentToUpsert)
            }
        }
    }

    /**
     * Suspended method to upsert a document in the specified collection.
     * Replaces the document if it exists, otherwise inserts a new one.
     *
     * @param collection The collection name.
     * @param key The key for the document.
     * @param document The document to upsert.
     */
    private suspend fun upsertDocumentSuspend(collection: String, key: Any, document: Document) = coroutineScope {
        database.getCollection(collection).replaceOne(createKeyFilter(key), document, ReplaceOptions().upsert(true))
    }

    /**
     * Inserts multiple documents into the specified collection.
     * This operation is performed asynchronously.
     *
     * @param collection The collection name.
     * @param documents The list of documents to insert. Each document should ideally have a [KEY_FIELD].
     * @return The result of the insert many operation.
     */
    suspend fun insertMany(collection: String, documents: List<Document>): InsertManyResult = coroutineScope {
        database.getCollection(collection).insertMany(documents)
    }


    /**
     * Removes a document from the specified collection with the provided key.
     *
     * @param collection The collection name.
     * @param key The key for the document.
     * @return The removed document, or null if no document was found and deleted.
     */
    fun remove(collection: String, key: Any): Document? =
        database.getCollection(collection).findOneAndDelete(createKeyFilter(key))

    /**
     * Checks if a document exists in the specified collection with the provided key.
     *
     * @param collection The collection name.
     * @param key The key for the document.
     * @return True if the document exists, false otherwise.
     */
    fun exists(collection: String, key: Any): Boolean =
        database.getCollection(collection).countDocuments(createKeyFilter(key)) > 0


    /**
     * Gets all keys ([KEY_FIELD]) in the specified collection.
     *
     * @param collection The collection name.
     * @return A list of keys. Note: This can be inefficient for very large collections.
     */
    fun getKeys(collection: String): List<String> =
        database.getCollection(collection).find().map { it[KEY_FIELD].toString() }.toList()


    /**
     * Retrieves all documents from the specified collection, converting them to the specified type [T].
     *
     * @param T The type of objects to retrieve.
     * @param collection The name of the collection.
     * @param filters A map where each entry represents field -> value pairs to filter by.
     * @return A list of objects of the specified type [T].
     */
    inline fun <reified T : Any> getAllList(collection: String, filters: Map<String, Any> = emptyMap()): List<T?>? {
        val bsonFilters = filters.map { Filters.eq(it.key, it.value) }
        val finalFilter = if (bsonFilters.isEmpty()) BsonDocument() else Filters.and(bsonFilters)

        return database.getCollection(collection)
            .find(finalFilter)
            .mapNotNull { documentToTargetType<T>(it) }
            .toList()
    }

    /**
     * Retrieves all documents from the specified collection as a map of [KEY_FIELD] to objects of type [T].
     *
     * @param T The type of objects to retrieve.
     * @param collection The name of the collection.
     * @param filters A map where each entry represents field -> value pairs to filter by.
     * @return A map where keys are from [KEY_FIELD] and values are objects of type [T].
     */
    inline fun <reified T : Any> getAllMap(collection: String, filters: Map<String, Any> = emptyMap()): Map<String, T?>? {
        val bsonFilters = filters.map { Filters.eq(it.key, it.value) }
        val finalFilter = if (bsonFilters.isEmpty()) BsonDocument() else Filters.and(bsonFilters)

        val results = mutableMapOf<String, T>()
        database.getCollection(collection)
            .find(finalFilter)
            .forEach { doc ->
                val key = doc[KEY_FIELD]?.toString()
                val value = documentToTargetType<T>(doc)
                if (key != null && value != null) {
                    results[key] = value
                }
            }
        return results
    }


    /**
     * Retrieves a value from the specified collection with the provided key.
     *
     * @param T The type of the value to retrieve. Must be a non-nullable type.
     * @param collection The name of the collection.
     * @param key The key for the value.
     * @param defaultValue The default value to return if the key does not exist.
     * @return The value of the specified type [T], or [defaultValue] if not found. If no value is found and no defaultValue is provided, returns null.
     */
    inline fun <reified T : Any> get(collection: String, key: Any, defaultValue: T? = null): T? {
        val document = getDocument(collection, key)
        return if (document != null) documentToTargetType<T>(document) ?: defaultValue
        else defaultValue
    }

    /**
     * Helper function to convert a Document's VALUE_FIELD to the target type T.
     */
    inline fun <reified T : Any> documentToTargetType(document: Document): T? {
        val valuePart = document[VALUE_FIELD] ?: return null

        return when {
            T::class.java.isAssignableFrom(valuePart::class.java) -> valuePart as T
            MongoSObject::class.java.isAssignableFrom(T::class.java) -> {
                val valueDoc = valuePart as? Document ?: Document.parse(gson.toJson(valuePart))
                gson.fromJson(valueDoc.toJson(), T::class.java)
            }

            valuePart is T -> valuePart
            else -> {
                try {
                    gson.fromJson(gson.toJsonTree(valuePart), T::class.java)
                } catch (e: Exception) {
                    System.err.println("Failed to convert value to ${T::class.java.name}: $e")
                    null
                }
            }
        }
    }

    /**
     * Gets a single document from the specified collection with the provided key.
     *
     * @param collection The collection name.
     * @param key The key for the document.
     * @return The [Document] if found, otherwise null.
     */
    fun getDocument(collection: String, key: Any): Document? =
        database.getCollection(collection).find(createKeyFilter(key)).firstOrNull()

    /**
     * Retrieves documents from a collection that match the specified key.
     *
     * @param collection The name of the collection.
     * @param key The value of the key to filter by.
     * @return A [FindIterable<Document>] containing the documents that match the criteria.
     */
    fun getDocuments(collection: String, key: Any): FindIterable<Document> =
        database.getCollection(collection).find(createKeyFilter(key))


    /**
     * Retrieves documents from a collection as a list that match the specified key.
     *
     * @param collection The name of the collection.
     * @param key The value of the key to filter by.
     * @return An [List<Document>] containing the documents that match the criteria.
     */
    fun getDocumentsAsList(collection: String, key: Any): List<Document> =
        getDocuments(collection, key).toList()


    /**
     * Converts a [MongoSObject] to a [Document] for storing in MongoDB.
     * The [MongoSObject] itself becomes the content of the [VALUE_FIELD].
     * The key should be set separately.
     *
     * @param mongoSObject The [MongoSObject] to convert.
     * @return The [Document] representation, typically for the [VALUE_FIELD].
     */
    internal fun convertMongoSObjectToDocumentValue(mongoSObject: MongoSObject): Document =
        Document.parse(gson.toJson(mongoSObject))

    /**
     * Converts any object to its [Document] representation using Gson.
     * This is a general-purpose utility.
     */
    fun convertToDocument(obj: Any): Document = Document.parse(gson.toJson(obj))


    /**
     * Converts a [Document] to its JSON string representation.
     *
     * @param document The document to convert.
     * @return The JSON string.
     */
    fun convertDocumentToJson(document: Document): String = gson.toJson(document)

    /**
     * Converts a JSON string to a [Document].
     *
     * @param json The JSON string to convert.
     * @return The [Document].
     */
    fun convertJsonToDocument(json: String): Document = Document.parse(json)

    /**
     * Checks if the database is connected and reachable by sending a ping command.
     *
     * @return True if connected and ping is successful, false otherwise.
     */
    fun isConnected(): Boolean {
        return try {
            database.runCommand(BsonDocument("ping", BsonInt64(1)))
            true
        } catch (e: MongoException) {
            System.err.println("MongoDB connection check failed: ${e.message}")
            false
        } catch (e: Exception) {
            System.err.println("An unexpected error occurred during MongoDB connection check: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}
