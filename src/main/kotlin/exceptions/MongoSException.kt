package com.swadedev.mongos4k.exceptions

sealed class MongoSException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
