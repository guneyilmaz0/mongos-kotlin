package net.guneyilmaz0.mongos4k.exceptions

sealed class MongoSException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
