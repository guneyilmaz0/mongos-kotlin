package net.guneyilmaz0.mongos4k.operations

import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.InsertOneModel
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.DeleteOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Filters
import net.guneyilmaz0.mongos4k.Database
import net.guneyilmaz0.mongos4k.exceptions.MongoSOperationException
import org.bson.Document
import org.bson.conversions.Bson

/**
 * Utility class for batch operations on MongoDB collections.
 * Provides efficient ways to perform bulk inserts, updates, and deletes.
 *
 * @author guneyilmaz0
 */
class MongoSBatchOperations(private val database: Database) {

    /**
     * Performs a bulk insert operation.
     *
     * @param collection The collection name.
     * @param documents The documents to insert.
     * @param ordered Whether the operations should be ordered (default: false).
     * @throws MongoSOperationException If the operation fails.
     */
    fun bulkInsert(
        collection: String,
        documents: List<Document>,
        ordered: Boolean = false
    ) {
        if (documents.isEmpty()) {
            throw MongoSOperationException("Document list cannot be empty")
        }

        try {
            val insertModels = documents.map { InsertOneModel(it) }
            val options = BulkWriteOptions().ordered(ordered)
            database.database.getCollection(collection).bulkWrite(insertModels, options)
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to perform bulk insert operation", e)
        }
    }

    /**
     * Performs a bulk upsert operation using key-value pairs.
     *
     * @param collection The collection name.
     * @param keyValuePairs The key-value pairs to upsert.
     * @param ordered Whether the operations should be ordered (default: false).
     * @throws MongoSOperationException If the operation fails.
     */
    fun bulkUpsert(
        collection: String,
        keyValuePairs: Map<Any, Any>,
        ordered: Boolean = false
    ) {
        if (keyValuePairs.isEmpty()) {
            throw MongoSOperationException("Key-value pairs cannot be empty")
        }

        try {
            val replaceModels = keyValuePairs.map { (key, value) ->
                val filter = Filters.eq(Database.KEY_FIELD, key)
                val document = Document(Database.KEY_FIELD, key)
                    .append(Database.VALUE_FIELD, value)
                ReplaceOneModel(filter, document, ReplaceOptions().upsert(true))
            }
            val options = BulkWriteOptions().ordered(ordered)
            database.database.getCollection(collection).bulkWrite(replaceModels, options)
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to perform bulk upsert operation", e)
        }
    }

    /**
     * Performs a bulk delete operation by keys.
     *
     * @param collection The collection name.
     * @param keys The list of keys for documents to delete.
     * @param ordered Whether the operations should be ordered (default: false).
     * @throws MongoSOperationException If the operation fails.
     */
    fun bulkDeleteByKeys(
        collection: String,
        keys: List<Any>,
        ordered: Boolean = false
    ) {
        if (keys.isEmpty()) {
            throw MongoSOperationException("Keys list cannot be empty")
        }

        try {
            val deleteModels = keys.map { key ->
                DeleteOneModel<Document>(Filters.eq(Database.KEY_FIELD, key))
            }
            val options = BulkWriteOptions().ordered(ordered)
            database.database.getCollection(collection).bulkWrite(deleteModels, options)
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to perform bulk delete operation", e)
        }
    }

    companion object {
        /**
         * Creates a new batch operations instance.
         *
         * @param database The database instance.
         * @return A new MongoSBatchOperations instance.
         */
        fun create(database: Database): MongoSBatchOperations = MongoSBatchOperations(database)
    }
}