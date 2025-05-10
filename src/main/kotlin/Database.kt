package net.guneyilmaz0.mongos4k

import com.google.gson.Gson
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.result.InsertManyResult
import kotlinx.coroutines.*
import org.bson.BsonDocument
import org.bson.BsonInt64
import org.bson.Document
import org.bson.conversions.Bson

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
    }

    lateinit var database: MongoDatabase

    /**
     * Initializes the database with the specified MongoDB database instance.
     *
     * @param database the MongoDB database instance.
     */
    open fun init(database: MongoDatabase) {
        try {
            this.database = database
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createDBObject(key: Any): DBObject =
        BasicDBObject().append(KEY_FIELD, key)

    /**
     * Sets a value in the specified collection with the provided key and value.
     *
     * @param collection the collection name.
     * @param key the key for the value.
     * @param value the value to set.
     */
    fun set(collection: String, key: Any, value: Any) = set(collection, key, value, false)

    /**
     * Sets a value in the specified collection with the provided key and value.
     *
     * @param collection the collection name.
     * @param key the key for the value.
     * @param value the value to set.
     * @param async whether the operation should be asynchronous.
     */
    fun set(collection: String, key: Any, value: Any, async: Boolean = false) {
        val keyDocument = Document(KEY_FIELD, key)
        val finalDocument =
            keyDocument.append(VALUE_FIELD, if (value is MongoSObject) convertDocument(value) else value)
        setFinal(collection, key, finalDocument, async)
    }

    /**
     * Final method to set a document in the specified collection with the provided key and document.
     *
     * @param collection the collection name.
     * @param key the key for the document.
     * @param document the document to set.
     * @param async whether the operation should be asynchronous.
     */
    fun setFinal(collection: String, key: Any, document: Document, async: Boolean = false) {
        if (async) CoroutineScope(Dispatchers.IO).launch { setFinalSuspend(collection, key, document) }
        else runBlocking(Dispatchers.IO) { setFinalSuspend(collection, key, document) }
    }

    /**
     * Suspended method to set a document in the specified collection with the provided key and document asynchronously.
     *
     * @param collection the collection name.
     * @param key the key for the document.
     * @param document the document to set.
     */
    private suspend fun setFinalSuspend(collection: String, key: Any, document: Document) = coroutineScope {
        val removed = remove(collection, key)
        removed?.let { document[KEY_FIELD] = it[KEY_FIELD] }
        database.getCollection(collection).insertOne(document)
    }

    /**
     * Sets multiple documents in the specified collection.
     *
     * @param collection the collection name.
     * @param documents the list of documents to set.
     * @return the result of the operation.
     */
    suspend fun setMany(collection: String, documents: List<Document>): InsertManyResult = coroutineScope {
        database.getCollection(collection).insertMany(documents)
    }

    /**
     * Removes a document from the specified collection with the provided key.
     *
     * @param collection the collection name.
     * @param key the key for the document.
     * @return the removed document.
     */
    fun remove(collection: String, key: Any): Document? =
        database.getCollection(collection).findOneAndDelete(createDBObject(key) as Bson)

    /**
     * Checks if a document exists in the specified collection with the provided key.
     * Supports CaseInsensitiveString for key matching.
     *
     * @param collection the collection name.
     * @param key the key for the document.
     * @return true if the document exists, false otherwise.
     */
    fun exists(collection: String, key: Any): Boolean =
        database.getCollection(collection).find(createDBObject(key) as Bson).first() != null

    /**
     * Gets all keys in the specified collection.
     *
     * @param collection the collection name.
     * @return a list of keys.
     */
    fun getKeys(collection: String): List<String> =
        database.getCollection(collection).find().map { it[KEY_FIELD].toString() }.toList()

    /**
     * Gets all documents in the specified collection as a map.
     *
     * @param collection the collection name.
     * @return a map of keys and values.
     */
    fun getAll(collection: String): Map<String, Any> =
        database.getCollection(collection).find().associate { it[KEY_FIELD].toString() to it[VALUE_FIELD]!! }

    /**
     * Gets filtered documents in the specified collection as a map.
     *
     * @param collection the collection name.
     * @param filters a map where each entry represents key1 -> key2 pairs to filter.
     * @return a map of keys and values matching the filters.
     */
    fun getAll(collection: String, filters: Map<String, String>): Map<String, Any> =
        database.getCollection(collection)
            .find(Filters.and(filters.map { Filters.eq(it.key, it.value) }))
            .associate { it[KEY_FIELD].toString() to it[VALUE_FIELD]!! }

    /**
     * Retrieves a value from the specified collection with the provided key.
     *
     * @param T The type of the value to retrieve.
     * @param collection The name of the collection.
     * @param key The key for the value.
     * @return The value of the specified type, or null if not found.
     */
    inline fun <reified T> get(collection: String, key: Any): T? {
        return get(collection, key, null)
    }

    /**
     * Retrieves a value from the specified collection with the provided key.
     * If the key does not exist, returns the provided default value or throws an exception.
     *
     * @param T The type of the value to retrieve.
     * @param collection The name of the collection.
     * @param key The key for the value.
     * @param defaultValue The default value to return if the key does not exist. Defaults to null.
     * @return The value of the specified type, or the default value if not found.
     * @throws NoSuchElementException If the key does not exist and no default value is provided.
     */
    inline fun <reified T> get(collection: String, key: Any, defaultValue: T? = null): T? {
        val document = getDocument(collection, key)
            ?: return defaultValue ?: throw NoSuchElementException(
                "No document found in collection '$collection' with key '$key'."
            )

        return if (MongoSObject::class.java.isAssignableFrom(T::class.java))
            documentToObject(document, T::class.java)
        else document[VALUE_FIELD] as? T
    }

    /**
     * Converts a Document to an object of the specified type.
     *
     * @param T The type of the object to convert to.
     * @param document The Document to convert.
     * @param classOff The class type of the object.
     * @return The object of the specified type.
     */
    fun <T> documentToObject(document: Document, classOff: Class<T>): T? {
        val value = document["value"]
        val json = Gson().toJson(value)
        return Gson().fromJson(json, classOff)
    }

    /**
     * Retrieves a list of objects from a collection based on a specified key, key name, and value.
     *
     * @param T The type of objects to retrieve.
     * @param collection The name of the collection.
     * @param key The value of the key to filter by.
     * @param classOff The class type of the objects to retrieve.
     * @return A list of objects of the specified type, or null if the key does not exist in the collection.
     */
    fun <T> getList(collection: String, key: Any, classOff: Class<T>): List<T>? {
        if (this.exists(collection, key)) {
            val doc = this.getDocument(collection, key)
            return doc!!.getList(VALUE_FIELD, classOff)
        } else return null
    }

    /**
     * Gets a document from the specified collection with the provided key.
     *
     * @param collection the collection name.
     * @param key the key for the document.
     * @return the document.
     */
    fun getDocument(collection: String, key: Any): Document? =
        database.getCollection(collection).find(createDBObject(key) as Bson).first()

    /**
     * Retrieves documents from a collection that match the specified key.
     *
     * @param collection The name of the collection.
     * @param key The value of the key to filter by.
     * @return A `FindIterable<Document>` containing the documents that match the criteria.
     */
    fun getDocuments(collection: String, key: Any): FindIterable<Document> =
        database.getCollection(collection).find(createDBObject(key) as Bson)

    /**
     * Retrieves documents from a collection as a list that match the specified key.
     *
     * @param collection The name of the collection.
     * @param key The value of the key to filter by.
     * @return An `List<Document>` containing the documents that match the criteria.
     */
    fun getDocumentsAsList(collection: String, key: Any): List<Document> =
        database.getCollection(collection).find(createDBObject(key) as Bson).toList()

    /**
     * Gets an iterable of documents from the specified collection with the provided key.
     *
     * @param collection the collection name.
     * @param key the key for the documents.
     * @return the iterable of documents.
     */
    fun getIterable(collection: String, key: Any): FindIterable<Document> =
        database.getCollection(collection).find(createDBObject(key) as Bson)

    fun convertDocument(mongoSObject: MongoSObject): Document = convertJsonToDocument(gson.toJson(mongoSObject))

    /**
     * Converts a document to a JSON string.
     *
     * @param document the document to convert.
     * @return the JSON string.
     */
    fun convertDocumentToJson(document: Document): String = gson.toJson(document)

    /**
     * Converts a JSON string to a document.
     *
     * @param json the JSON string to convert.
     * @return the document.
     */
    fun convertJsonToDocument(json: String): Document = Document.parse(json)

    fun isConnected(): Boolean {
        try {
            database.runCommand(BsonDocument("ping", BsonInt64(1)))
            true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}