package net.guneyilmaz0.mongos4k.examples

import net.guneyilmaz0.mongos4k.MongoS
import net.guneyilmaz0.mongos4k.config.MongoSConfig
import net.guneyilmaz0.mongos4k.query.MongoSQueryBuilder
import net.guneyilmaz0.mongos4k.aggregation.MongoSAggregationBuilder
import net.guneyilmaz0.mongos4k.operations.MongoSTransactionManager
import net.guneyilmaz0.mongos4k.MongoSObject

// Example data class
data class User(
    val id: String,
    val name: String,
    val email: String,
    val age: Int,
    val active: Boolean
) : MongoSObject()

/**
 * Comprehensive example demonstrating all the new features of MongoS Kotlin API.
 */
fun main() {
    // 1. Enhanced Connection Management
    val config = MongoSConfig.production()
    val mongoS = MongoS(config, "myapp")
    
    println("Connection Stats: ${mongoS.getConnectionStats()}")
    
    // 2. Basic Operations with Validation
    val users = "users"
    val user1 = User("1", "John Doe", "john@example.com", 30, true)
    val user2 = User("2", "Jane Smith", "jane@example.com", 25, true)
    val user3 = User("3", "Bob Johnson", "bob@example.com", 35, false)
    
    // Store users
    mongoS.set(users, user1.id, user1)
    mongoS.set(users, user2.id, user2)
    mongoS.set(users, user3.id, user3)
    
    // 3. Advanced Query Builder
    val queryBuilder = MongoSQueryBuilder.build {
        eq("active", true)
        gte("age", 25)
        sortDesc("age")
        limit(10)
    }
    
    val activeUsers = mongoS.find<User>(users, queryBuilder)
    println("Active users: ${activeUsers.size}")
    
    // 4. Pagination
    val paginatedUsers = mongoS.findPaginated<User>(users, 
        MongoSQueryBuilder.build { eq("active", true) }, 
        page = 1, 
        pageSize = 2
    )
    println("Paginated users: ${paginatedUsers.items.size} of ${paginatedUsers.totalCount}")
    
    // 5. Aggregation Pipeline
    val aggregation = MongoSAggregationBuilder.build {
        match("active", true)
        groupWithCount(true, "userCount")
        sortDesc("userCount")
    }
    
    val aggregationResult = mongoS.aggregate(users, aggregation)
    println("Aggregation result: ${aggregationResult.size}")
    
    // 6. Batch Operations
    val batchData: Map<Any, Any> = mapOf(
        "4" to User("4", "Alice Brown", "alice@example.com", 28, true),
        "5" to User("5", "Charlie Davis", "charlie@example.com", 32, true)
    )
    
    mongoS.batchOperations.bulkUpsert(users, batchData)
    println("Batch operations completed")
    
    // 7. Index Management
    mongoS.indexManager.createIndex(users, "email", unique = true)
    mongoS.indexManager.createCompoundIndex(users, listOf("age" to true, "active" to true))
    
    val indexes = mongoS.indexManager.listIndexes(users)
    println("Created indexes: ${indexes.size}")
    
    // 8. Transaction Support
    val result = mongoS.transactionManager.withTransaction(
        { session ->
            // Perform multiple operations within a transaction
            val user = mongoS.get<User>(users, "1")
            val updatedUser = user.copy(age = user.age + 1)
            mongoS.set(users, user.id, updatedUser)
            "Transaction completed successfully"
        }
    )
    println("Transaction result: $result")
    
    // 9. Performance Monitoring
    val (userCount, metrics) = mongoS.performanceMonitor.measureOperation(
        "count_users", 
        users
    ) {
        mongoS.count(users, MongoSQueryBuilder.build { eq("active", true) })
    }
    
    println("User count: $userCount, Operation took: ${metrics.duration.toMillis()}ms")
    
    // 10. Health Check
    val healthCheck = mongoS.performanceMonitor.performHealthCheck()
    println("Database health: ${healthCheck["healthy"]}")
    
    // 11. Database Statistics
    val dbStats = mongoS.performanceMonitor.getDatabaseStats()
    println("Database stats: ${dbStats.documents} documents, ${dbStats.collections} collections")
    
    // 12. Backup and Restore
    val backupResult = mongoS.backupUtility.exportCollection(users, "/tmp/users_backup.json")
    println("Backup completed: $backupResult documents exported")
    
    // 13. Advanced Error Handling
    try {
        mongoS.get<User>("nonexistent", "invalid")
    } catch (e: Exception) {
        println("Caught exception: ${e.message}")
    }
    
    // 14. Configuration Information
    mongoS.getConfig()?.let { config ->
        println("Connection timeout: ${config.connectionTimeout}")
        println("Max connections: ${config.maxConnectionsPerHost}")
    }
    
    // Clean up
    mongoS.close()
    println("Connection closed")
}

/**
 * Example demonstrating different configuration options.
 */
fun configurationExamples() {
    // Development configuration
    val devConfig = MongoSConfig.defaultLocal()
    val devDb = MongoS(devConfig, "dev_db")
    
    // Production configuration
    val prodConfig = MongoSConfig.production()
    val prodDb = MongoS(prodConfig, "prod_db")
    
    // Testing configuration
    val testConfig = MongoSConfig.testing()
    val testDb = MongoS(testConfig, "test_db")
    
    // Custom configuration
    val customConfig = MongoSConfig(
        host = "custom-host",
        port = 27017,
        maxConnectionsPerHost = 50,
        connectionTimeout = java.time.Duration.ofSeconds(10)
    )
    val customDb = MongoS(customConfig, "custom_db")
    
    // Clean up
    devDb.close()
    prodDb.close()
    testDb.close()
    customDb.close()
}

/**
 * Example demonstrating complex queries and aggregations.
 */
fun complexQueryExamples() {
    val mongoS = MongoS("complex_queries")
    val collection = "products"
    
    // Complex query with multiple conditions
    val complexQuery = MongoSQueryBuilder.build {
        or {
            and {
                eq("category", "electronics")
                gte("price", 100)
            }
            and {
                eq("category", "books")
                lte("price", 50)
            }
        }
        sortAsc("price")
        limit(20)
    }
    
    val results = mongoS.find<Map<String, Any>>(collection, complexQuery)
    println("Complex query results: ${results.size}")
    
    // Advanced aggregation
    val advancedAggregation = MongoSAggregationBuilder.build {
        match("active", true)
        groupWithAvg("electronics", "avgPrice", "price")
        sortDesc("avgPrice")
        limit(10)
        projectInclude("_id", "avgPrice")
    }
    
    val aggregationResults = mongoS.aggregate(collection, advancedAggregation)
    println("Advanced aggregation results: ${aggregationResults.size}")
    
    mongoS.close()
}

/**
 * Example demonstrating error handling and validation.
 */
fun errorHandlingExamples() {
    val mongoS = MongoS("error_handling")
    
    try {
        // This will throw a validation exception
        mongoS.set("", "key", "value")
    } catch (e: net.guneyilmaz0.mongos4k.exceptions.MongoSValidationException) {
        println("Validation error: ${e.message}")
    }
    
    try {
        // This will throw a not found exception
        mongoS.get<String>("collection", "nonexistent")
    } catch (e: net.guneyilmaz0.mongos4k.exceptions.MongoSNotFoundException) {
        println("Not found error: ${e.message}")
    }
    
    try {
        // This will throw an operation exception (simplified for example)
        val invalidQuery = MongoSQueryBuilder.build { eq("invalid_field", "value") }
        mongoS.count("collection", invalidQuery)
    } catch (e: net.guneyilmaz0.mongos4k.exceptions.MongoSOperationException) {
        println("Operation error: ${e.message}")
    }
    
    mongoS.close()
}