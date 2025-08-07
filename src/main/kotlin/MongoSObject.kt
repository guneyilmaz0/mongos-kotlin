package net.guneyilmaz0.mongos4k

import com.google.gson.Gson
import org.bson.Document

/**
 * Abstract base class for MongoDB-related objects.
 * This class can be extended to define specific MongoDB object behaviors.
 * It serves as a common type for objects intended to be stored in MongoDB
 * using the MongoS4K library, particularly when custom object mapping is involved.
 *
 * @author guneyilmaz0
 */
@Suppress("unused")
abstract class MongoSObject {

    /**
     * Converts the object to a JSON string representation.
     * This method uses Gson to serialize the object into JSON format.
     *
     * @return A JSON string representing the object.
     */
    fun toJson(): String = Gson().toJson(this)

    fun toDocument(): Document = Document.parse(toJson())
}