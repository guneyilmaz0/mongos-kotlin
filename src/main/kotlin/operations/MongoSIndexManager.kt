package net.guneyilmaz0.mongos4k.operations

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import net.guneyilmaz0.mongos4k.Database
import net.guneyilmaz0.mongos4k.exceptions.MongoSOperationException
import org.bson.Document
import java.util.concurrent.TimeUnit

/**
 * Utility class for managing MongoDB indexes.
 * Provides methods to create, drop, and manage indexes for better query performance.
 *
 * @author guneyilmaz0
 */
class MongoSIndexManager(private val database: Database) {

    /**
     * Creates a single field ascending index.
     *
     * @param collection The collection name.
     * @param field The field to index.
     * @param unique Whether the index should enforce uniqueness (default: false).
     * @param background Whether to create the index in the background (default: true).
     * @return The name of the created index.
     * @throws MongoSOperationException If the operation fails.
     */
    fun createIndex(
        collection: String,
        field: String,
        unique: Boolean = false,
        background: Boolean = true
    ): String {
        return try {
            val options = IndexOptions()
                .unique(unique)
                .background(background)
            database.database.getCollection(collection).createIndex(Indexes.ascending(field), options)
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to create index on field '$field'", e)
        }
    }

    /**
     * Creates a single field descending index.
     *
     * @param collection The collection name.
     * @param field The field to index.
     * @param unique Whether the index should enforce uniqueness (default: false).
     * @param background Whether to create the index in the background (default: true).
     * @return The name of the created index.
     * @throws MongoSOperationException If the operation fails.
     */
    fun createDescendingIndex(
        collection: String,
        field: String,
        unique: Boolean = false,
        background: Boolean = true
    ): String {
        return try {
            val options = IndexOptions()
                .unique(unique)
                .background(background)
            database.database.getCollection(collection).createIndex(Indexes.descending(field), options)
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to create descending index on field '$field'", e)
        }
    }

    /**
     * Creates a compound index on multiple fields.
     *
     * @param collection The collection name.
     * @param fields The list of fields with their sort directions (field to ascending/descending).
     * @param unique Whether the index should enforce uniqueness (default: false).
     * @param background Whether to create the index in the background (default: true).
     * @return The name of the created index.
     * @throws MongoSOperationException If the operation fails.
     */
    fun createCompoundIndex(
        collection: String,
        fields: List<Pair<String, Boolean>>, // field to ascending (true) or descending (false)
        unique: Boolean = false,
        background: Boolean = true
    ): String {
        if (fields.isEmpty()) {
            throw MongoSOperationException("Fields list cannot be empty for compound index")
        }

        return try {
            val indexKeys = fields.map { (field, ascending) ->
                if (ascending) Indexes.ascending(field) else Indexes.descending(field)
            }
            val compoundIndex = Indexes.compoundIndex(indexKeys)
            val options = IndexOptions()
                .unique(unique)
                .background(background)
            database.database.getCollection(collection).createIndex(compoundIndex, options)
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to create compound index", e)
        }
    }

    /**
     * Creates a text index for full-text search.
     *
     * @param collection The collection name.
     * @param fields The fields to include in the text index.
     * @param language The language for text indexing (default: "english").
     * @param background Whether to create the index in the background (default: true).
     * @return The name of the created index.
     * @throws MongoSOperationException If the operation fails.
     */
    fun createTextIndex(
        collection: String,
        fields: List<String>,
        language: String = "english",
        background: Boolean = true
    ): String {
        if (fields.isEmpty()) {
            throw MongoSOperationException("Fields list cannot be empty for text index")
        }

        return try {
            val textIndex = if (fields.size == 1) {
                Indexes.text(fields.first())
            } else {
                Indexes.compoundIndex(fields.map { Indexes.text(it) })
            }
            val options = IndexOptions()
                .defaultLanguage(language)
                .background(background)
            database.database.getCollection(collection).createIndex(textIndex, options)
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to create text index", e)
        }
    }

    /**
     * Creates a TTL (Time To Live) index that automatically removes documents after a specified time.
     *
     * @param collection The collection name.
     * @param field The date field to use for TTL.
     * @param expireAfterSeconds The time in seconds after which documents expire.
     * @param background Whether to create the index in the background (default: true).
     * @return The name of the created index.
     * @throws MongoSOperationException If the operation fails.
     */
    fun createTTLIndex(
        collection: String,
        field: String,
        expireAfterSeconds: Long,
        background: Boolean = true
    ): String {
        return try {
            val options = IndexOptions()
                .expireAfter(expireAfterSeconds, TimeUnit.SECONDS)
                .background(background)
            database.database.getCollection(collection).createIndex(Indexes.ascending(field), options)
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to create TTL index on field '$field'", e)
        }
    }

    /**
     * Creates a sparse index that only indexes documents that contain the indexed field.
     *
     * @param collection The collection name.
     * @param field The field to index.
     * @param unique Whether the index should enforce uniqueness (default: false).
     * @param background Whether to create the index in the background (default: true).
     * @return The name of the created index.
     * @throws MongoSOperationException If the operation fails.
     */
    fun createSparseIndex(
        collection: String,
        field: String,
        unique: Boolean = false,
        background: Boolean = true
    ): String {
        return try {
            val options = IndexOptions()
                .sparse(true)
                .unique(unique)
                .background(background)
            database.database.getCollection(collection).createIndex(Indexes.ascending(field), options)
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to create sparse index on field '$field'", e)
        }
    }

    /**
     * Drops an index by name.
     *
     * @param collection The collection name.
     * @param indexName The name of the index to drop.
     * @throws MongoSOperationException If the operation fails.
     */
    fun dropIndex(collection: String, indexName: String) {
        try {
            database.database.getCollection(collection).dropIndex(indexName)
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to drop index '$indexName'", e)
        }
    }

    /**
     * Drops an index by field name.
     *
     * @param collection The collection name.
     * @param field The field of the index to drop.
     * @throws MongoSOperationException If the operation fails.
     */
    fun dropIndexByField(collection: String, field: String) {
        try {
            database.database.getCollection(collection).dropIndex(Indexes.ascending(field))
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to drop index on field '$field'", e)
        }
    }

    /**
     * Drops all indexes in a collection except the _id index.
     *
     * @param collection The collection name.
     * @throws MongoSOperationException If the operation fails.
     */
    fun dropAllIndexes(collection: String) {
        try {
            database.database.getCollection(collection).dropIndexes()
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to drop all indexes", e)
        }
    }

    /**
     * Lists all indexes in a collection.
     *
     * @param collection The collection name.
     * @return A list of index documents.
     * @throws MongoSOperationException If the operation fails.
     */
    fun listIndexes(collection: String): List<Document> {
        return try {
            database.database.getCollection(collection).listIndexes().toList()
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to list indexes", e)
        }
    }

    /**
     * Checks if an index exists by name.
     *
     * @param collection The collection name.
     * @param indexName The name of the index to check.
     * @return True if the index exists, false otherwise.
     * @throws MongoSOperationException If the operation fails.
     */
    fun indexExists(collection: String, indexName: String): Boolean {
        return try {
            listIndexes(collection).any { it.getString("name") == indexName }
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to check if index exists", e)
        }
    }

    /**
     * Gets statistics about an index.
     *
     * @param collection The collection name.
     * @param indexName The name of the index.
     * @return Index statistics as a Document.
     * @throws MongoSOperationException If the operation fails.
     */
    fun getIndexStats(collection: String, indexName: String): Document {
        return try {
            val command = Document("collStats", collection)
                .append("indexDetails", Document(indexName, 1))
            database.database.runCommand(command)
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to get index statistics", e)
        }
    }

    /**
     * Creates optimized indexes for common operations.
     *
     * @param collection The collection name.
     * @throws MongoSOperationException If any operation fails.
     */
    fun createOptimizedIndexes(collection: String) {
        try {
            // Create index on key field for faster lookups
            createIndex(collection, Database.KEY_FIELD, unique = true)
            
            // Create compound index for key-value operations
            createCompoundIndex(collection, listOf(
                Database.KEY_FIELD to true,
                Database.VALUE_FIELD to true
            ))
            
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to create optimized indexes", e)
        }
    }

    companion object {
        /**
         * Creates a new index manager instance.
         *
         * @param database The database instance.
         * @return A new MongoSIndexManager instance.
         */
        fun create(database: Database): MongoSIndexManager = MongoSIndexManager(database)
    }
}