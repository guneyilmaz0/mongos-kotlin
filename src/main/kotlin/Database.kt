package net.guneyilmaz0.mongos4k

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mongodb.MongoException
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.CountOptions
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.result.InsertManyResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
 * Lightweight key-value style wrapper around a MongoDB database.
 *
 * Design goals:
 * - **Non-nullable reads:** `get` returns `T` directly. If the key does not exist,
 *   it returns `null` at runtime (like Java's `Map.get`), so Kotlin callers never
 *   need `!!`. Use [getOrNull] for explicit null handling or `get` with a default.
 * - **Performance:** collection handles and index creation are cached, reads use
 *   projections to fetch only the value field, and writes go straight to the sync
 *   driver (no coroutine overhead unless `async = true`).
 * - **Interop:** every generic method has a `Class<T>` overload for Java and a
 *   reified inline overload for Kotlin (`db.get<String>("users", "name")`).
 *
 * @author guneyilmaz0
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
open class Database {

    companion object {
        /** Document field that stores the key. */
        @JvmStatic
        val KEY_FIELD = "key"

        /** Document field that stores the value. */
        @JvmStatic
        val VALUE_FIELD = "value"

        /** Shared Gson instance used for POJO <-> Document conversion. */
        @JvmStatic
        val gson: Gson = GsonBuilder().serializeNulls().create()

        /**
         * Converts a [Number] to the requested numeric type.
         * MongoDB may store an Int as Long (or vice versa); this bridges the gap.
         *
         * @return the converted number, or null if [clazz] is not a supported numeric type.
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> convertNumber(number: Number, clazz: Class<T>): T? =
            when (clazz) {
                Int::class.java, java.lang.Integer::class.java -> number.toInt() as T
                Long::class.java, java.lang.Long::class.java -> number.toLong() as T
                Double::class.java, java.lang.Double::class.java -> number.toDouble() as T
                Float::class.java, java.lang.Float::class.java -> number.toFloat() as T
                Short::class.java, java.lang.Short::class.java -> number.toShort() as T
                Byte::class.java, java.lang.Byte::class.java -> number.toByte() as T
                else -> null
            }
    }

    private val logger = LoggerFactory.getLogger(Database::class.java)

    /** Coroutine scope for fire-and-forget async writes. */
    private val asyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Caches collection handles so repeated access avoids re-resolution. */
    private val collectionCache = ConcurrentHashMap<String, MongoCollection<Document>>()

    /** Tracks collections whose key index has already been created. */
    private val indexedCollections = ConcurrentHashMap.newKeySet<String>()

    lateinit var database: MongoDatabase
        private set

    // ---------------------------------------------------------------------
    // Initialization
    // ---------------------------------------------------------------------

    /**
     * Binds this instance to the given MongoDB database and resets internal caches.
     *
     * @param database the MongoDB database instance.
     */
    open fun init(database: MongoDatabase) {
        this.database = database
        collectionCache.clear()
        indexedCollections.clear()
        logger.info("Database initialized: {}", database.name)
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    /** Builds the standard key filter used by all key-based operations. */
    private fun keyFilter(key: Any): Bson = Filters.eq(KEY_FIELD, key)

    /** Returns a cached collection handle, ensuring the key index exists. */
    private fun collection(name: String): MongoCollection<Document> {
        val coll = collectionCache.computeIfAbsent(name) { database.getCollection(it) }
        if (indexedCollections.add(name)) {
            try {
                coll.createIndex(
                    Indexes.ascending(KEY_FIELD),
                    IndexOptions().background(true).name("${KEY_FIELD}_idx"),
                )
            } catch (e: Exception) {
                // Retry on next access instead of failing the actual operation.
                indexedCollections.remove(name)
                logger.warn("Index creation failed for {}: {}", name, e.message)
            }
        }
        return coll
    }

    /** Serializes a value into a form the MongoDB driver can store. */
    private fun Any.toStoredValue(): Any =
        if (this is MongoSObject) Document.parse(gson.toJson(this)) else this

    // ---------------------------------------------------------------------
    // Write operations
    // ---------------------------------------------------------------------

    /**
     * Sets (upserts) a value for the given key.
     * Existing documents with the same key are replaced.
     *
     * @param collection the collection name.
     * @param key the key to store the value under.
     * @param value the value: a primitive, a POJO, or a [MongoSObject].
     * @param async if true, the write runs in the background and errors are only logged.
     * @throws MongoSWriteException if a synchronous write fails.
     */
    @JvmOverloads
    fun <T : Any> set(collection: String, key: Any, value: T, async: Boolean = false) {
        val document = Document(KEY_FIELD, key).append(VALUE_FIELD, value.toStoredValue())
        val options = ReplaceOptions().upsert(true)

        if (async) {
            asyncScope.launch {
                try {
                    collection(collection).replaceOne(keyFilter(key), document, options)
                } catch (e: Exception) {
                    logger.error("Async set failed for key=$key in $collection", e)
                }
            }
        } else {
            try {
                collection(collection).replaceOne(keyFilter(key), document, options)
            } catch (e: Exception) {
                throw MongoSWriteException("Failed to set key=$key in $collection", e)
            }
        }
    }

    /**
     * Inserts multiple documents in a single batch.
     * Each document should ideally contain a [KEY_FIELD].
     *
     * @param collection the collection name.
     * @param documents the documents to insert.
     * @return the driver's insert result.
     * @throws MongoSWriteException if the insert fails.
     */
    fun insertMany(collection: String, documents: List<Document>): InsertManyResult =
        try {
            collection(collection).insertMany(documents)
        } catch (e: Exception) {
            throw MongoSWriteException("Failed to insertMany into $collection", e)
        }

    /**
     * Removes the document with the given key.
     *
     * @param collection the collection name.
     * @param key the key to remove.
     * @return the removed document, or null if no document matched.
     * @throws MongoSWriteException if the delete fails.
     */
    fun remove(collection: String, key: Any): Document? =
        try {
            collection(collection).findOneAndDelete(keyFilter(key))
        } catch (e: Exception) {
            throw MongoSWriteException("Failed to remove key=$key from $collection", e)
        }

    // ---------------------------------------------------------------------
    // Read operations — the get family
    // ---------------------------------------------------------------------

    /**
     * Gets the value for the given key.
     *
     * The return type is non-nullable so Kotlin callers never need `!!`,
     * but — like Java's `Map.get` — this returns `null` at runtime when the
     * key does not exist. Prefer [getOrNull] or the default-value overload
     * when absence is expected.
     *
     * @param collection the collection name.
     * @param key the key to look up.
     * @param clazz the target type.
     * @return the stored value, or null (typed as [T]) if the key does not exist.
     * @throws MongoSReadException if the query fails.
     * @throws MongoSTypeException if the stored value cannot be converted to [T].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(collection: String, key: Any, clazz: Class<T>): T =
        getOrNull(collection, key, clazz) as T

    /**
     * Kotlin convenience: `db.get<String>("users", "name")`.
     * Same semantics as the `Class<T>` overload.
     */
    inline fun <reified T : Any> get(collection: String, key: Any): T =
        get(collection, key, T::class.java)

    /**
     * Gets the value for the given key, or [default] if the key does not exist.
     * This overload is always truly non-null.
     *
     * @param collection the collection name.
     * @param key the key to look up.
     * @param clazz the target type.
     * @param default the value returned when the key is absent.
     * @throws MongoSReadException if the query fails.
     * @throws MongoSTypeException if the stored value cannot be converted to [T].
     */
    fun <T : Any> get(collection: String, key: Any, clazz: Class<T>, default: T): T =
        getOrNull(collection, key, clazz) ?: default

    /**
     * Kotlin convenience: `db.get("users", "name", "fallback")`.
     */
    inline fun <reified T : Any> get(collection: String, key: Any, default: T): T =
        get(collection, key, T::class.java, default)

    /**
     * Gets the value for the given key with an explicitly nullable result.
     *
     * @param collection the collection name.
     * @param key the key to look up.
     * @param clazz the target type.
     * @return the stored value, or null if the key does not exist.
     * @throws MongoSReadException if the query fails.
     * @throws MongoSTypeException if the stored value cannot be converted to [T].
     */
    fun <T : Any> getOrNull(collection: String, key: Any, clazz: Class<T>): T? {
        val document =
            try {
                collection(collection)
                    .find(keyFilter(key))
                    .projection(Document(VALUE_FIELD, 1))
                    .limit(1)
                    .firstOrNull()
            } catch (e: Exception) {
                throw MongoSReadException("Failed to get key=$key from $collection", e)
            }
        return document?.let { documentToTargetType(it, clazz) }
    }

    /**
     * Kotlin convenience: `db.getOrNull<String>("users", "name")`.
     */
    inline fun <reified T : Any> getOrNull(collection: String, key: Any): T? =
        getOrNull(collection, key, T::class.java)

    /**
     * Checks whether a document with the given key exists.
     * Uses a count with `limit(1)` so it stays fast on large collections.
     *
     * @throws MongoSReadException if the query fails.
     */
    fun exists(collection: String, key: Any): Boolean =
        try {
            collection(collection).countDocuments(keyFilter(key), CountOptions().limit(1)) > 0
        } catch (e: Exception) {
            throw MongoSReadException("Failed to check exists for key=$key in $collection", e)
        }

    // ---------------------------------------------------------------------
    // Bulk read operations
    // ---------------------------------------------------------------------

    /**
     * Retrieves all values in a collection as a list of [T].
     * Documents that cannot be converted are skipped with a warning.
     *
     * @param collection the collection name.
     * @param clazz the target type.
     * @param filters optional field -> value equality filters.
     * @throws MongoSReadException if the query fails.
     */
    @JvmOverloads
    fun <T : Any> getAllList(
        collection: String,
        clazz: Class<T>,
        filters: Map<String, Any> = emptyMap(),
    ): List<T> =
        try {
            collection(collection)
                .find(buildFilter(filters))
                .mapNotNull { convertOrSkip(it, clazz) }
                .toList()
        } catch (e: Exception) {
            throw MongoSReadException("Failed to getAllList from $collection", e)
        }

    /** Kotlin convenience for [getAllList] with a reified type. */
    inline fun <reified T : Any> getAllList(
        collection: String,
        filters: Map<String, Any> = emptyMap(),
    ): List<T> = getAllList(collection, T::class.java, filters)

    /**
     * Retrieves all values in a collection as a map of key -> [T].
     * Documents without a key or that cannot be converted are skipped.
     *
     * @param collection the collection name.
     * @param clazz the target type.
     * @param filters optional field -> value equality filters.
     * @throws MongoSReadException if the query fails.
     */
    @JvmOverloads
    fun <T : Any> getAllMap(
        collection: String,
        clazz: Class<T>,
        filters: Map<String, Any> = emptyMap(),
    ): Map<String, T> =
        try {
            val results = LinkedHashMap<String, T>()
            collection(collection).find(buildFilter(filters)).forEach { doc ->
                val key = doc[KEY_FIELD]?.toString() ?: return@forEach
                convertOrSkip(doc, clazz)?.let { results[key] = it }
            }
            results
        } catch (e: Exception) {
            throw MongoSReadException("Failed to getAllMap from $collection", e)
        }

    /** Kotlin convenience for [getAllMap] with a reified type. */
    inline fun <reified T : Any> getAllMap(
        collection: String,
        filters: Map<String, Any> = emptyMap(),
    ): Map<String, T> = getAllMap(collection, T::class.java, filters)

    /** Builds a combined equality filter from a map, or an empty filter. */
    private fun buildFilter(filters: Map<String, Any>): Bson =
        if (filters.isEmpty()) BsonDocument()
        else Filters.and(filters.map { Filters.eq(it.key, it.value) })

    /** Converts a document to [T], returning null (and logging) on failure. */
    private fun <T : Any> convertOrSkip(document: Document, clazz: Class<T>): T? =
        try {
            documentToTargetType(document, clazz)
        } catch (e: Exception) {
            logger.warn("Skipping document, cannot convert to {}: {}", clazz.simpleName, e.message)
            null
        }

    // ---------------------------------------------------------------------
    // Raw document access
    // ---------------------------------------------------------------------

    /**
     * Gets the raw document for the given key.
     *
     * @return the full [Document], or null if not found.
     * @throws MongoSReadException if the query fails.
     */
    fun getDocument(collection: String, key: Any): Document? =
        try {
            collection(collection).find(keyFilter(key)).limit(1).firstOrNull()
        } catch (e: Exception) {
            throw MongoSReadException("Failed to getDocument for key=$key in $collection", e)
        }

    /**
     * Gets all documents matching the given key as a lazy iterable.
     *
     * @throws MongoSReadException if the query fails.
     */
    fun getDocuments(collection: String, key: Any): FindIterable<Document> =
        try {
            collection(collection).find(keyFilter(key))
        } catch (e: Exception) {
            throw MongoSReadException("Failed to getDocuments for key=$key in $collection", e)
        }

    /**
     * Gets all documents matching the given key as a list.
     *
     * @throws MongoSReadException if the query fails.
     */
    fun getDocumentsAsList(collection: String, key: Any): List<Document> =
        getDocuments(collection, key).toList()

    /**
     * Gets all keys in a collection.
     * Note: may be expensive on very large collections.
     *
     * @throws MongoSReadException if the query fails.
     */
    fun getKeys(collection: String): List<String> =
        try {
            collection(collection)
                .find()
                .projection(Document(KEY_FIELD, 1))
                .mapNotNull { it[KEY_FIELD]?.toString() }
                .toList()
        } catch (e: Exception) {
            throw MongoSReadException("Failed to get keys from $collection", e)
        }

    /**
     * Gets a sorted list of all collection names in this database.
     *
     * @throws MongoSReadException if the query fails.
     */
    fun getCollections(): List<String> =
        try {
            database.listCollectionNames().toList().sorted()
        } catch (e: Exception) {
            throw MongoSReadException("Failed to list collections", e)
        }

    // ---------------------------------------------------------------------
    // Type conversion
    // ---------------------------------------------------------------------

    /**
     * Converts a document's [VALUE_FIELD] to the target type [T].
     * Handles direct assignment, numeric bridging, [MongoSObject] subclasses,
     * and arbitrary POJOs via Gson.
     *
     * @return the converted value, or null if [VALUE_FIELD] is absent.
     * @throws MongoSTypeException if conversion fails.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> documentToTargetType(document: Document, clazz: Class<T>): T? {
        val value = document[VALUE_FIELD] ?: return null
        return try {
            when {
                // Value already has the requested type.
                clazz.isAssignableFrom(value::class.java) -> value as T
                // Numeric bridging (e.g. stored Long, requested Int).
                value is Number && Number::class.java.isAssignableFrom(clazz) ->
                    convertNumber(value, clazz)
                        ?: throw MongoSTypeException(
                            "Unsupported number conversion: ${value::class.java.name} -> ${clazz.name}",
                        )
                // MongoSObject subclasses are deserialized via Gson.
                MongoSObject::class.java.isAssignableFrom(clazz) -> {
                    val doc = value as? Document ?: Document.parse(gson.toJson(value))
                    gson.fromJson(doc.toJson(), clazz)
                }
                // Fallback: generic POJO conversion via Gson.
                else -> gson.fromJson(gson.toJsonTree(value), clazz)
            }
        } catch (e: MongoSTypeException) {
            throw e
        } catch (e: Exception) {
            throw MongoSTypeException("Failed to convert value to ${clazz.name}", e)
        }
    }

    /** Kotlin convenience for [documentToTargetType] with a reified type. */
    inline fun <reified T : Any> documentToTargetType(document: Document): T? =
        documentToTargetType(document, T::class.java)

    // ---------------------------------------------------------------------
    // Conversion utilities
    // ---------------------------------------------------------------------

    /** Converts any object to a MongoDB [Document] via Gson. */
    fun convertToDocument(obj: Any): Document = Document.parse(gson.toJson(obj))

    /** Converts a [Document] to its JSON string representation. */
    fun convertDocumentToJson(document: Document): String = gson.toJson(document)

    /** Parses a JSON string into a [Document]. */
    fun convertJsonToDocument(json: String): Document = Document.parse(json)

    // ---------------------------------------------------------------------
    // Maintenance
    // ---------------------------------------------------------------------

    /**
     * Exports every document in a collection to a pretty-printed JSON file.
     * Documents are streamed so memory usage stays flat on large collections.
     *
     * @param collection the collection to export.
     * @param path the directory the file is written into.
     * @return the written file: `<db>.<collection>.json`.
     * @throws MongoSWriteException if writing fails.
     */
    fun saveAsJson(collection: String, path: String): File {
        try {
            val directory = File(path).apply { if (!exists()) mkdirs() }
            val file = File(directory, "${database.name}.$collection.json")
            val settings = JsonWriterSettings.builder().indent(true).build()

            file.bufferedWriter().use { writer ->
                writer.write("[\n")
                val iterator = collection(collection).find().iterator()
                while (iterator.hasNext()) {
                    writer.write(iterator.next().toJson(settings))
                    if (iterator.hasNext()) writer.write(",")
                    writer.newLine()
                }
                writer.write("]\n")
            }

            logger.info("Exported {} to {}", collection, file.absolutePath)
            return file
        } catch (e: Exception) {
            throw MongoSWriteException("Failed to save $collection as JSON", e)
        }
    }

    /**
     * Checks connectivity by sending a ping command.
     *
     * @return true if the ping succeeds.
     * @throws MongoSConnectionException if the ping fails.
     */
    fun isConnected(): Boolean =
        try {
            database.runCommand(BsonDocument("ping", BsonInt64(1)))
            true
        } catch (e: MongoException) {
            throw MongoSConnectionException("MongoDB connection check failed", e)
        }

    /**
     * Gets basic statistics for a collection (count, sizes, index count).
     * Returns an empty map if the stats command fails.
     */
    fun getCollectionStats(collection: String): Map<String, Any> =
        try {
            val stats = database.runCommand(Document("collStats", collection))
            mapOf(
                "count" to (stats.getLong("count") ?: 0L),
                "size" to (stats.getLong("size") ?: 0L),
                "storageSize" to (stats.getLong("storageSize") ?: 0L),
                "avgObjSize" to (stats.getDouble("avgObjSize") ?: 0.0),
                "indexCount" to (stats.getInteger("nindexes") ?: 0),
            )
        } catch (e: Exception) {
            logger.warn("Failed to get stats for collection {}: {}", collection, e.message)
            emptyMap()
        }
}
