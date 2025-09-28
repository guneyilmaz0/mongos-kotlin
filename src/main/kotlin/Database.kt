package net.guneyilmaz0.mongos4k

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mongodb.MongoException
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.result.InsertManyResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.guneyilmaz0.mongos4k.exceptions.MongoSConnectionException
import net.guneyilmaz0.mongos4k.exceptions.MongoSReadException
import net.guneyilmaz0.mongos4k.exceptions.MongoSTypeException
import net.guneyilmaz0.mongos4k.exceptions.MongoSWriteException
import org.bson.BsonDocument
import org.bson.BsonInt64
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.json.JsonWriterSettings
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * This class represents a MongoDB database with enhanced performance and professional features.
 * It provides methods to initialize the database, set and get values, update and remove data, and check if data exists.
 *
 * Key Features:
 * - Connection pooling and resource management
 * - Optimized queries with proper indexing
 * - Concurrent operations support
 * - Comprehensive error handling
 * - Performance monitoring and logging
 *
 * @property database the MongoDB database instance.
 * @author guneyilmaz0
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
open class Database {
    companion object {
        const val KEY_FIELD = "key"
        const val VALUE_FIELD = "value"

        private val gsonBuilder =
            GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create()

        val gson: Gson = gsonBuilder

        // Index cache to prevent duplicate index creation
        private val indexCache = ConcurrentHashMap<String, Boolean>()

        /**
         * Converts a POJO to a MongoDB Document with optimized serialization.
         * If the object is a MongoSObject, it's converted via Gson.
         * Otherwise, it's returned as is (hoping it's a type MongoDB driver can handle).
         */
        private fun Any.toBsonValue(): Any {
            return if (this is MongoSObject) {
                Document.parse(gson.toJson(this))
            } else {
                this
            }
        }
    }

    private val logger = LoggerFactory.getLogger(Database::class.java)

    lateinit var database: MongoDatabase
        private set

    /**
     * Initializes the database with the specified MongoDB database instance and sets up optimizations.
     *
     * @param database the MongoDB database instance.
     * @throws MongoException if there is an issue with database initialization.
     */
    open fun init(database: MongoDatabase) {
        this.database = database
        logger.info("Database initialized: ${database.name}")
    }

    private fun createKeyFilter(key: Any): Bson = Filters.eq(KEY_FIELD, key)

    /**
     * Ensures that the collection has the proper index on the key field for optimal performance.
     * Uses caching to avoid redundant index creation operations.
     *
     * @param collection The collection name.
     */
    private fun ensureKeyIndex(collection: String) {
        val cacheKey = "${database.name}.$collection"
        if (indexCache.putIfAbsent(cacheKey, true) == null) {
            try {
                database.getCollection(collection).createIndex(
                    Indexes.ascending(KEY_FIELD),
                    IndexOptions().background(true).name("${KEY_FIELD}_idx"),
                )
                logger.debug("Index created for collection: $collection")
            } catch (e: Exception) {
                logger.warn("Index creation failed for $collection: ${e.message}")
                indexCache.remove(cacheKey) // Remove from cache so we can retry later
            }
        }
    }

    /**
     * Sets (upserts) a value in the specified collection with the provided key and value.
     * Automatically creates optimal indexes for better query performance.
     * If a document with the key already exists, it will be replaced. Otherwise, a new document will be inserted.
     *
     * @param collection The collection name.
     * @param key The key for the value (will be used as _id or your custom key field).
     * @param value The value to set. Can be a primitive, a custom object, or a [MongoSObject].
     */
    fun set(
        collection: String,
        key: Any,
        value: Any,
    ): Unit = set(collection, key, value, async = false)

    /**
     * Sets (upserts) a value in the specified collection with the provided key and value of type [T].
     * Includes performance optimizations and proper error handling.
     * If a document with the key already exists, it will be replaced. Otherwise, a new document will be inserted.
     *
     * @param T The type of the value to set. Must be a non-nullable type.
     * @param collection The collection name.
     * @param key The key for the value (will be used as _id or your custom key field).
     * @param value The value to set. Can be a primitive, a custom object, or a [MongoSObject].
     * @param async Whether the operation should be asynchronous.
     * @throws MongoSWriteException if there is an issue during the write operation.
     */
    fun <T : Any> set(
        collection: String,
        key: Any,
        value: T,
        async: Boolean = false,
    ) {
        ensureKeyIndex(collection)

        val documentToUpsert =
            Document(KEY_FIELD, key)
                .append(VALUE_FIELD, value.toBsonValue())

        try {
            if (async) {
                CoroutineScope(Dispatchers.IO).launch {
                    upsertDocumentSuspend(collection, key, documentToUpsert)
                }
            } else {
                runBlocking(Dispatchers.IO) {
                    upsertDocumentSuspend(collection, key, documentToUpsert)
                }
            }
            logger.debug("Successfully set key={} in collection={}", key, collection)
        } catch (e: Exception) {
            logger.error("Failed to set key=$key in $collection", e)
            throw MongoSWriteException("Failed to set key=$key in $collection", e)
        }
    }

    /**
     * Suspended method to upsert a document in the specified collection with optimized write operations.
     * Replaces the document if it exists, otherwise inserts a new one.
     *
     * @param collection The collection name.
     * @param key The key for the document.
     * @param document The document to upsert.
     */
    private suspend fun upsertDocumentSuspend(
        collection: String,
        key: Any,
        document: Document,
    ) = withContext(Dispatchers.IO) {
        database.getCollection(collection).replaceOne(
            createKeyFilter(key),
            document,
            ReplaceOptions().upsert(true),
        )
    }

    /**
     * Batch inserts multiple documents into the specified collection with optimized performance.
     * This operation is performed asynchronously and includes proper error handling.
     *
     * @param collection The collection name.
     * @param documents The list of documents to insert. Each document should ideally have a [KEY_FIELD].
     * @return The result of the insert many operation.
     * @throws MongoSWriteException if there is an issue during the insert operation.
     */
    suspend fun insertMany(
        collection: String,
        documents: List<Document>,
    ): InsertManyResult =
        withContext(Dispatchers.IO) {
            try {
                ensureKeyIndex(collection)
                val result = database.getCollection(collection).insertMany(documents)
                logger.info("Successfully inserted ${documents.size} documents into $collection")
                result
            } catch (e: Exception) {
                logger.error("Failed to insertMany into $collection", e)
                throw MongoSWriteException("Failed to insertMany into $collection", e)
            }
        }

    /**
     * Removes a document from the specified collection with the provided key.
     * Includes performance logging and proper error handling.
     *
     * @param collection The collection name.
     * @param key The key for the document.
     * @return The removed document, or null if no document was found and deleted.
     * @throws MongoSWriteException if there is an issue during the delete operation.
     */
    fun remove(
        collection: String,
        key: Any,
    ): Document? =
        try {
            val result = database.getCollection(collection).findOneAndDelete(createKeyFilter(key))
            if (result != null) {
                logger.debug("Successfully removed key={} from collection={}", key, collection)
            } else {
                logger.debug("No document found with key={} in collection={}", key, collection)
            }
            result
        } catch (e: Exception) {
            logger.error("Failed to remove key=$key from $collection", e)
            throw MongoSWriteException("Failed to remove key=$key from $collection", e)
        }

    /**
     * Optimized method to check if a document exists in the specified collection with the provided key.
     * Uses count with limit for better performance on large collections.
     *
     * @param collection The collection name.
     * @param key The key for the document.
     * @return True if the document exists, false otherwise.
     * @throws MongoSReadException if there is an issue during the read operation.
     */
    fun exists(
        collection: String,
        key: Any,
    ): Boolean =
        try {
            database.getCollection(collection).countDocuments(createKeyFilter(key), com.mongodb.client.model.CountOptions().limit(1)) > 0
        } catch (e: Exception) {
            logger.error("Failed to check exists for key=$key in $collection", e)
            throw MongoSReadException("Failed to check exists for key=$key in $collection", e)
        }

    /**
     * Gets a cached list of all collection names in the current database.
     * Results are sorted for consistent ordering.
     *
     * @return A sorted list of collection names in the current database.
     * @throws MongoSReadException if there is an issue retrieving the collection names.
     */
    fun getCollections(): List<String> =
        try {
            database.listCollectionNames().toList().sorted().also { collections ->
                logger.debug("Retrieved ${collections.size} collections from database")
            }
        } catch (e: Exception) {
            logger.error("Failed to list collections", e)
            throw MongoSReadException("Failed to list collections", e)
        }

    /**
     * Gets all keys ([KEY_FIELD]) in the specified collection with performance considerations.
     * Note: This can be inefficient for very large collections. Consider using pagination for large datasets.
     *
     * @param collection The collection name.
     * @return A list of keys.
     * @throws MongoSReadException if there is an issue during the read operation.
     */
    fun getKeys(collection: String): List<String> =
        try {
            database.getCollection(collection)
                .find()
                .projection(Document(KEY_FIELD, 1))
                .map { it[KEY_FIELD].toString() }
                .toList()
                .also { keys ->
                    logger.debug("Retrieved ${keys.size} keys from collection=$collection")
                }
        } catch (e: Exception) {
            logger.error("Failed to get keys from $collection", e)
            throw MongoSReadException("Failed to get keys from $collection", e)
        }

    /**
     * Retrieves all documents from the specified collection with optimized filtering and projection.
     * Converts them to the specified type [T] with enhanced error handling.
     *
     * @param T The type of objects to retrieve.
     * @param collection The name of the collection.
     * @param filters A map where each entry represents field -> value pairs to filter by.
     * @return A list of objects of the specified type [T].
     * @throws MongoSReadException if there is an issue during the read operation.
     */
    fun <T : Any> getAllList(
        collection: String,
        clazz: Class<T>,
        filters: Map<String, Any> = emptyMap(),
    ): List<T> {
        val bsonFilters = filters.map { Filters.eq(it.key, it.value) }
        val finalFilter = if (bsonFilters.isEmpty()) BsonDocument() else Filters.and(bsonFilters)

        return try {
            database.getCollection(collection)
                .find(finalFilter)
                .mapNotNull { document ->
                    try {
                        documentToTargetType(document, clazz)
                    } catch (e: Exception) {
                        logger.warn("Failed to convert document to ${clazz.simpleName}: ${e.message}")
                        null // Skip invalid documents instead of failing entirely
                    }
                }
                .toList()
                .also { results ->
                    logger.debug("Retrieved ${results.size} documents of type ${clazz.simpleName} from $collection")
                }
        } catch (e: Exception) {
            logger.error("Failed to getAllList from $collection", e)
            throw MongoSReadException("Failed to getAllList from $collection", e)
        }
    }

    /**
     * Inline convenience method for getAllList with reified type parameter.
     */
    inline fun <reified T : Any> getAllList(
        collection: String,
        filters: Map<String, Any> = emptyMap(),
    ): List<T> = getAllList(collection, T::class.java, filters)

    /**
     * Retrieves all documents from the specified collection as a map with optimized performance.
     * Maps [KEY_FIELD] to objects of type [T] with proper error handling.
     *
     * @param T The type of objects to retrieve.
     * @param collection The name of the collection.
     * @param filters A map where each entry represents field -> value pairs to filter by.
     * @return A map where keys are from [KEY_FIELD] and values are objects of type [T].
     * @throws MongoSReadException if there is an issue during the read operation.
     */
    fun <T : Any> getAllMap(
        collection: String,
        clazz: Class<T>,
        filters: Map<String, Any> = emptyMap(),
    ): Map<String, T> {
        val bsonFilters = filters.map { Filters.eq(it.key, it.value) }
        val finalFilter = if (bsonFilters.isEmpty()) BsonDocument() else Filters.and(bsonFilters)

        return try {
            val results = mutableMapOf<String, T>()
            var processedCount = 0
            var skippedCount = 0

            database.getCollection(collection)
                .find(finalFilter)
                .forEach { doc ->
                    try {
                        val key = doc[KEY_FIELD]?.toString()
                        val value = documentToTargetType(doc, clazz)
                        if (key != null && value != null) {
                            results[key] = value
                            processedCount++
                        } else {
                            skippedCount++
                        }
                    } catch (e: Exception) {
                        logger.warn("Failed to process document in getAllMap: ${e.message}")
                        skippedCount++
                    }
                }

            logger.debug("getAllMap from $collection: processed=$processedCount, skipped=$skippedCount")
            results
        } catch (e: Exception) {
            logger.error("Failed to getAllMap from $collection", e)
            throw MongoSReadException("Failed to getAllMap from $collection", e)
        }
    }

    /**
     * Inline convenience method for getAllMap with reified type parameter.
     */
    inline fun <reified T : Any> getAllMap(
        collection: String,
        filters: Map<String, Any> = emptyMap(),
    ): Map<String, T> = getAllMap(collection, T::class.java, filters)

    /**
     * Retrieves a value from the specified collection with the provided key using optimized queries.
     * Includes comprehensive error handling and type conversion.
     *
     * @param T The type of the value to retrieve. Must be a non-nullable type.
     * @param collection The name of the collection.
     * @param key The key for the value.
     * @param defaultValue The default value to return if the key does not exist.
     * @return The value of the specified type [T], or [defaultValue] if not found.
     * @throws MongoSReadException if there is an issue during the read operation.
     * @throws MongoSTypeException if there is an issue converting the stored value to type [T].
     */
    fun <T : Any> get(
        collection: String,
        key: Any,
        clazz: Class<T>,
        defaultValue: T? = null,
    ): T? {
        val document =
            try {
                getDocument(collection, key)
            } catch (e: Exception) {
                logger.error("Failed to get key=$key from $collection", e)
                throw MongoSReadException("Failed to get key=$key from $collection", e)
            }

        return if (document != null) {
            try {
                documentToTargetType(document, clazz) ?: defaultValue
            } catch (e: Exception) {
                logger.warn("Failed to convert document to ${clazz.simpleName} for key=$key", e)
                defaultValue
            }
        } else {
            defaultValue
        }
    }

    /**
     * Inline convenience method for get with reified type parameter.
     */
    inline fun <reified T : Any> get(
        collection: String,
        key: Any,
        defaultValue: T? = null,
    ): T? = get(collection, key, T::class.java, defaultValue)

    /**
     * Enhanced helper function to convert a Document's VALUE_FIELD to the target type T.
     * Handles various scenarios with improved error handling and performance.
     *
     * @param T The target type to convert to.
     * @param document The MongoDB Document containing the VALUE_FIELD.
     * @return The value converted to type T, or null if VALUE_FIELD is absent.
     * @throws MongoSTypeException if conversion to type T fails.
     */
    fun <T : Any> documentToTargetType(
        document: Document,
        clazz: Class<T>,
    ): T? {
        val valuePart = document[VALUE_FIELD] ?: return null
        return try {
            when {
                clazz.isAssignableFrom(valuePart::class.java) -> valuePart as T
                MongoSObject::class.java.isAssignableFrom(clazz) -> {
                    val valueDoc = valuePart as? Document ?: Document.parse(gson.toJson(valuePart))
                    gson.fromJson(valueDoc.toJson(), clazz)
                }
                clazz.isInstance(valuePart) -> valuePart as T
                else -> gson.fromJson(gson.toJsonTree(valuePart), clazz)
            }
        } catch (e: Exception) {
            logger.error("Failed to convert value to ${clazz.name}", e)
            throw MongoSTypeException("Failed to convert value to ${clazz.name}", e)
        }
    }

    /**
     * Inline convenience method for documentToTargetType with reified type parameter.
     */
    inline fun <reified T : Any> documentToTargetType(document: Document): T? = documentToTargetType(document, T::class.java)

    /**
     * Gets a single document from the specified collection with optimized query performance.
     *
     * @param collection The collection name.
     * @param key The key for the document.
     * @return The [Document] if found, otherwise null.
     * @throws MongoSReadException if there is an issue during the read operation.
     */
    fun getDocument(
        collection: String,
        key: Any,
    ): Document? =
        try {
            database.getCollection(collection)
                .find(createKeyFilter(key))
                .limit(1)
                .firstOrNull()
        } catch (e: Exception) {
            logger.error("Failed to getDocument for key=$key in $collection", e)
            throw MongoSReadException("Failed to getDocument for key=$key in $collection", e)
        }

    /**
     * Retrieves documents from a collection that match the specified key with proper error handling.
     *
     * @param collection The name of the collection.
     * @param key The value of the key to filter by.
     * @return A [FindIterable<Document>] containing the documents that match the criteria.
     * @throws MongoSReadException if there is an issue during the read operation.
     */
    fun getDocuments(
        collection: String,
        key: Any,
    ): FindIterable<Document> =
        try {
            database.getCollection(collection).find(createKeyFilter(key))
        } catch (e: Exception) {
            logger.error("Failed to getDocuments for key=$key in $collection", e)
            throw MongoSReadException("Failed to getDocuments for key=$key in $collection", e)
        }

    /**
     * Retrieves documents from a collection as a list with optimized performance.
     *
     * @param collection The name of the collection.
     * @param key The value of the key to filter by.
     * @return A [List<Document>] containing the documents that match the criteria.
     * @throws MongoSReadException if there is an issue during the read operation.
     */
    fun getDocumentsAsList(
        collection: String,
        key: Any,
    ): List<Document> =
        try {
            getDocuments(collection, key).toList().also { docs ->
                logger.debug("Retrieved {} documents for key={} from {}", docs.size, key, collection)
            }
        } catch (e: Exception) {
            logger.error("Failed to getDocumentsAsList for key=$key in $collection", e)
            throw MongoSReadException("Failed to getDocumentsAsList for key=$key in $collection", e)
        }

    /**
     * Converts a [MongoSObject] to a [Document] with optimized serialization.
     * The [MongoSObject] itself becomes the content of the [VALUE_FIELD].
     *
     * @param mongoSObject The [MongoSObject] to convert.
     * @return The [Document] representation, typically for the [VALUE_FIELD].
     */
    internal fun convertMongoSObjectToDocumentValue(mongoSObject: MongoSObject): Document = Document.parse(gson.toJson(mongoSObject))

    /**
     * Converts any object to its [Document] representation using optimized Gson serialization.
     */
    fun convertToDocument(obj: Any): Document = Document.parse(gson.toJson(obj))

    /**
     * Converts a [Document] to its JSON string representation with pretty printing.
     *
     * @param document The document to convert.
     * @return The JSON string.
     */
    fun convertDocumentToJson(document: Document): String = gson.toJson(document)

    /**
     * Converts a JSON string to a [Document] with proper error handling.
     *
     * @param json The JSON string to convert.
     * @return The [Document].
     */
    fun convertJsonToDocument(json: String): Document = Document.parse(json)

    /**
     * Enhanced method to save all documents in the collection to a JSON file.
     * Includes progress logging, better error handling, and optimized memory usage for large collections.
     *
     * @param collection The name of the collection to save.
     * @param path The directory path where the JSON file should be saved.
     * @return The [File] object where the data was saved.
     * @throws MongoSWriteException if there is an issue during the file writing process.
     */
    fun saveAsJson(
        collection: String,
        path: String,
    ): File {
        try {
            val directory = File(path)
            if (!directory.exists()) {
                directory.mkdirs()
                logger.info("Created directory: ${directory.absolutePath}")
            }

            val documents = database.getCollection(collection).find()
            val file = File(directory, "${database.name}.$collection.json")

            val prettyPrintSettings = JsonWriterSettings.builder().indent(true).build()
            var documentCount = 0

            file.bufferedWriter().use { writer ->
                writer.write("[\n")
                val iterator = documents.iterator()
                while (iterator.hasNext()) {
                    val doc = iterator.next()
                    val documentJson = doc.toJson(prettyPrintSettings)
                    writer.write(documentJson)
                    if (iterator.hasNext()) writer.write(",")
                    writer.newLine()
                    documentCount++

                    // Log progress for large collections
                    if (documentCount % 1000 == 0) {
                        logger.info("Saved $documentCount documents to JSON...")
                    }
                }
                writer.write("]\n")
            }

            logger.info("Successfully saved $documentCount documents from $collection to ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            logger.error("Failed to save $collection as JSON", e)
            throw MongoSWriteException("Failed to save $collection as JSON", e)
        }
    }

    /**
     * Enhanced connection check with better error handling and logging.
     * Checks if the database is connected and reachable by sending a ping command.
     *
     * @return True if connected and ping is successful, false otherwise.
     * @throws MongoSConnectionException if there is an issue during the ping operation.
     */
    fun isConnected(): Boolean {
        return try {
            database.runCommand(BsonDocument("ping", BsonInt64(1)))
            logger.debug("Database connection check successful")
            true
        } catch (e: MongoException) {
            logger.error("MongoDB connection check failed", e)
            throw MongoSConnectionException("MongoDB connection check failed", e)
        }
    }

    /**
     * Gets performance statistics for a collection including document count and storage size.
     *
     * @param collection The collection name.
     * @return A map containing collection statistics.
     */
    fun getCollectionStats(collection: String): Map<String, Any> {
        return try {
            val stats = database.runCommand(Document("collStats", collection))
            mapOf(
                "count" to (stats.getLong("count") ?: 0L),
                "size" to (stats.getLong("size") ?: 0L),
                "storageSize" to (stats.getLong("storageSize") ?: 0L),
                "avgObjSize" to (stats.getDouble("avgObjSize") ?: 0.0),
                "indexCount" to (stats.getInteger("nindexes") ?: 0),
            ).also { collectionStats ->
                logger.debug("Collection stats for {}: {}", collection, collectionStats)
            }
        } catch (e: Exception) {
            logger.warn("Failed to get stats for collection $collection: ${e.message}")
            emptyMap()
        }
    }
}
