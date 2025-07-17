package net.guneyilmaz0.mongos4k.exceptions

/**
 * Base exception class for all MongoS-related exceptions.
 *
 * @param message The detail message.
 * @param cause The cause of this exception.
 * @author guneyilmaz0
 */
open class MongoSException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when there are issues with MongoDB connection.
 *
 * @param message The detail message.
 * @param cause The cause of this exception.
 */
class MongoSConnectionException(message: String, cause: Throwable? = null) : MongoSException(message, cause)

/**
 * Exception thrown when there are issues with database operations.
 *
 * @param message The detail message.
 * @param cause The cause of this exception.
 */
class MongoSOperationException(message: String, cause: Throwable? = null) : MongoSException(message, cause)

/**
 * Exception thrown when there are issues with data validation.
 *
 * @param message The detail message.
 * @param cause The cause of this exception.
 */
class MongoSValidationException(message: String, cause: Throwable? = null) : MongoSException(message, cause)

/**
 * Exception thrown when there are issues with data serialization/deserialization.
 *
 * @param message The detail message.
 * @param cause The cause of this exception.
 */
class MongoSSerializationException(message: String, cause: Throwable? = null) : MongoSException(message, cause)

/**
 * Exception thrown when a requested document or resource is not found.
 *
 * @param message The detail message.
 * @param cause The cause of this exception.
 */
class MongoSNotFoundException(message: String, cause: Throwable? = null) : MongoSException(message, cause)