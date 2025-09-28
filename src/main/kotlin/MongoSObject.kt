package net.guneyilmaz0.mongos4k

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import org.bson.Document
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Enhanced abstract base class for MongoDB-related objects with professional features.
 * This class provides common functionality for objects intended to be stored in MongoDB
 * using the MongoS4K library, particularly when custom object mapping is involved.
 *
 * Key Features:
 * - Automatic timestamp management
 * - Built-in serialization/deserialization
 * - UUID generation
 * - Validation support
 * - Debugging utilities
 *
 * @author guneyilmaz0
 */
@Suppress("unused")
abstract class MongoSObject {
    companion object {
        private val gson: Gson =
            GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .registerTypeAdapter(
                    LocalDateTime::class.java,
                    JsonSerializer<LocalDateTime> { src, _, _ ->
                        JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    },
                )
                .registerTypeAdapter(
                    LocalDateTime::class.java,
                    JsonDeserializer { json, _, _ ->
                        LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    },
                )
                .create()
    }

    /**
     * Unique identifier for the object. Automatically generated if not set.
     */
    var mongoId: String = UUID.randomUUID().toString()
        protected set

    /**
     * Timestamp when the object was created. Automatically set on instantiation.
     */
    var createdAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        protected set

    /**
     * Timestamp when the object was last updated. Updated automatically on modifications.
     */
    var updatedAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        protected set

    /**
     * Version field for optimistic locking and conflict detection.
     */
    var version: Int = 1
        protected set

    init {
        // Initialize timestamps
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        createdAt = now
        updatedAt = now
    }

    /**
     * Updates the updatedAt timestamp and increments the version.
     * Call this method whenever the object is modified.
     */
    protected fun markAsUpdated() {
        updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        version++
    }

    /**
     * Sets a custom ID for the object.
     *
     * @param customId The custom ID to set.
     */
    fun updateId(customId: String) {
        this.mongoId = customId
        markAsUpdated()
    }

    /**
     * Converts the object to its JSON representation using optimized Gson serialization.
     *
     * @return JSON string representation of the object.
     */
    fun toJson(): String = gson.toJson(this)

    /**
     * Converts the object to a MongoDB Document for storage.
     *
     * @return MongoDB Document representation.
     */
    fun toDocument(): Document = Document.parse(toJson())

    /**
     * Creates a copy of this object with updated timestamp and incremented version.
     * Subclasses should override this method to provide proper deep copying.
     *
     * @return A copy of this object with updated metadata.
     */
    open fun copy(): MongoSObject {
        val copy = this.javaClass.getDeclaredConstructor().newInstance()
        copy.mongoId = this.mongoId
        copy.createdAt = this.createdAt
        copy.updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        copy.version = this.version + 1
        return copy
    }

    /**
     * Validates the object's data integrity.
     * Subclasses should override this method to implement custom validation logic.
     *
     * @return List of validation error messages. Empty list means valid.
     */
    open fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (mongoId.isBlank()) {
            errors.add("ID cannot be blank")
        }

        if (version < 1) {
            errors.add("Version must be positive")
        }

        return errors
    }

    /**
     * Checks if the object is valid (no validation errors).
     *
     * @return True if the object passes validation, false otherwise.
     */
    fun isValid(): Boolean = validate().isEmpty()

    /**
     * Gets a summary of the object for debugging and logging purposes.
     *
     * @return A map containing key object information.
     */
    fun getSummary(): Map<String, Any> =
        mapOf(
            "id" to mongoId,
            "type" to this::class.java.simpleName,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "version" to version,
            "isValid" to isValid(),
        )

    /**
     * Compares this object with another MongoSObject based on ID.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MongoSObject) return false
        return mongoId == other.mongoId
    }

    /**
     * Hash code based on the object's ID.
     */
    override fun hashCode(): Int = mongoId.hashCode()

    /**
     * String representation showing key object information.
     */
    override fun toString(): String {
        return "${this::class.java.simpleName}(id='$mongoId', version=$version, updatedAt=$updatedAt)"
    }
}
