package net.guneyilmaz0.mongos4k.validation

import net.guneyilmaz0.mongos4k.exceptions.MongoSValidationException

/**
 * Validation utilities for MongoDB operations.
 * Provides methods to validate inputs and ensure data integrity.
 *
 * @author guneyilmaz0
 */
object MongoSValidator {

    /**
     * Validates that a collection name is not null or empty.
     *
     * @param collection The collection name to validate.
     * @throws MongoSValidationException If the collection name is invalid.
     */
    fun validateCollectionName(collection: String) {
        if (collection.isBlank()) {
            throw MongoSValidationException("Collection name cannot be null or empty")
        }
        if (collection.length > 120) {
            throw MongoSValidationException("Collection name cannot exceed 120 characters")
        }
        if (collection.contains('\$')) {
            throw MongoSValidationException("Collection name cannot contain '$' character")
        }
    }

    /**
     * Validates that a key is not null.
     *
     * @param key The key to validate.
     * @throws MongoSValidationException If the key is invalid.
     */
    fun validateKey(key: Any?) {
        if (key == null) {
            throw MongoSValidationException("Key cannot be null")
        }
        if (key is String && key.isBlank()) {
            throw MongoSValidationException("Key cannot be empty string")
        }
    }

    /**
     * Validates that a value is not null.
     *
     * @param value The value to validate.
     * @throws MongoSValidationException If the value is invalid.
     */
    fun validateValue(value: Any?) {
        if (value == null) {
            throw MongoSValidationException("Value cannot be null")
        }
    }

    /**
     * Validates pagination parameters.
     *
     * @param page The page number (1-based).
     * @param pageSize The number of items per page.
     * @throws MongoSValidationException If the parameters are invalid.
     */
    fun validatePagination(page: Int, pageSize: Int) {
        if (page < 1) {
            throw MongoSValidationException("Page number must be greater than 0")
        }
        if (pageSize < 1) {
            throw MongoSValidationException("Page size must be greater than 0")
        }
        if (pageSize > 1000) {
            throw MongoSValidationException("Page size cannot exceed 1000")
        }
    }

    /**
     * Validates that a database name is valid.
     *
     * @param dbName The database name to validate.
     * @throws MongoSValidationException If the database name is invalid.
     */
    fun validateDatabaseName(dbName: String) {
        if (dbName.isBlank()) {
            throw MongoSValidationException("Database name cannot be null or empty")
        }
        if (dbName.length > 64) {
            throw MongoSValidationException("Database name cannot exceed 64 characters")
        }
        val invalidChars = charArrayOf('/', '\\', '.', '"', '$', '*', '<', '>', ':', '|', '?')
        if (dbName.any { it in invalidChars }) {
            throw MongoSValidationException("Database name contains invalid characters")
        }
    }

    /**
     * Validates that a field name is valid for MongoDB.
     *
     * @param fieldName The field name to validate.
     * @throws MongoSValidationException If the field name is invalid.
     */
    fun validateFieldName(fieldName: String) {
        if (fieldName.isBlank()) {
            throw MongoSValidationException("Field name cannot be null or empty")
        }
        if (fieldName.startsWith('$')) {
            throw MongoSValidationException("Field name cannot start with '$'")
        }
        if (fieldName.contains('.')) {
            throw MongoSValidationException("Field name cannot contain '.' character")
        }
    }

    /**
     * Validates that a list is not null or empty.
     *
     * @param list The list to validate.
     * @param paramName The parameter name for error messages.
     * @throws MongoSValidationException If the list is invalid.
     */
    fun validateNonEmptyList(list: List<*>?, paramName: String) {
        if (list == null || list.isEmpty()) {
            throw MongoSValidationException("$paramName cannot be null or empty")
        }
    }

    /**
     * Validates that a MongoDB connection URI is properly formatted.
     *
     * @param uri The URI to validate.
     * @throws MongoSValidationException If the URI is invalid.
     */
    fun validateConnectionUri(uri: String) {
        if (uri.isBlank()) {
            throw MongoSValidationException("Connection URI cannot be null or empty")
        }
        if (!uri.startsWith("mongodb://") && !uri.startsWith("mongodb+srv://")) {
            throw MongoSValidationException("Connection URI must start with 'mongodb://' or 'mongodb+srv://'")
        }
    }

    /**
     * Validates host and port for MongoDB connection.
     *
     * @param host The host to validate.
     * @param port The port to validate.
     * @throws MongoSValidationException If the host or port is invalid.
     */
    fun validateHostPort(host: String, port: Int) {
        if (host.isBlank()) {
            throw MongoSValidationException("Host cannot be null or empty")
        }
        if (port < 1 || port > 65535) {
            throw MongoSValidationException("Port must be between 1 and 65535")
        }
    }

    /**
     * Validates that a timeout value is positive.
     *
     * @param timeout The timeout value in milliseconds.
     * @param paramName The parameter name for error messages.
     * @throws MongoSValidationException If the timeout is invalid.
     */
    fun validateTimeout(timeout: Long, paramName: String) {
        if (timeout <= 0) {
            throw MongoSValidationException("$paramName must be positive")
        }
        if (timeout > 300000) { // 5 minutes
            throw MongoSValidationException("$paramName cannot exceed 5 minutes (300000ms)")
        }
    }

    /**
     * Validates that a limit value is within acceptable bounds.
     *
     * @param limit The limit value.
     * @throws MongoSValidationException If the limit is invalid.
     */
    fun validateLimit(limit: Int) {
        if (limit < 1) {
            throw MongoSValidationException("Limit must be greater than 0")
        }
        if (limit > 10000) {
            throw MongoSValidationException("Limit cannot exceed 10000")
        }
    }

    /**
     * Validates that a skip value is non-negative.
     *
     * @param skip The skip value.
     * @throws MongoSValidationException If the skip is invalid.
     */
    fun validateSkip(skip: Int) {
        if (skip < 0) {
            throw MongoSValidationException("Skip must be non-negative")
        }
    }

    /**
     * Validates that a regex pattern is valid.
     *
     * @param pattern The regex pattern to validate.
     * @throws MongoSValidationException If the pattern is invalid.
     */
    fun validateRegexPattern(pattern: String) {
        if (pattern.isBlank()) {
            throw MongoSValidationException("Regex pattern cannot be null or empty")
        }
        try {
            java.util.regex.Pattern.compile(pattern)
        } catch (e: Exception) {
            throw MongoSValidationException("Invalid regex pattern: ${e.message}", e)
        }
    }

    /**
     * Validates that an index name is valid.
     *
     * @param indexName The index name to validate.
     * @throws MongoSValidationException If the index name is invalid.
     */
    fun validateIndexName(indexName: String) {
        if (indexName.isBlank()) {
            throw MongoSValidationException("Index name cannot be null or empty")
        }
        if (indexName.length > 127) {
            throw MongoSValidationException("Index name cannot exceed 127 characters")
        }
        if (indexName.startsWith('_') && indexName != "_id_") {
            throw MongoSValidationException("Index name cannot start with '_' (except '_id_')")
        }
    }

    /**
     * Validates that a TTL expiration time is valid.
     *
     * @param expireAfterSeconds The TTL expiration time in seconds.
     * @throws MongoSValidationException If the TTL is invalid.
     */
    fun validateTTL(expireAfterSeconds: Long) {
        if (expireAfterSeconds <= 0) {
            throw MongoSValidationException("TTL expiration time must be positive")
        }
        if (expireAfterSeconds > 2147483647) { // Max int value
            throw MongoSValidationException("TTL expiration time is too large")
        }
    }

    /**
     * Validates that a batch size is within acceptable bounds.
     *
     * @param batchSize The batch size.
     * @throws MongoSValidationException If the batch size is invalid.
     */
    fun validateBatchSize(batchSize: Int) {
        if (batchSize < 1) {
            throw MongoSValidationException("Batch size must be greater than 0")
        }
        if (batchSize > 1000) {
            throw MongoSValidationException("Batch size cannot exceed 1000")
        }
    }
}