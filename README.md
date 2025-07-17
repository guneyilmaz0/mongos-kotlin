# MongoDB Kotlin Wrapper

A comprehensive, feature-rich, and easy-to-use Kotlin wrapper for MongoDB operations with advanced functionality.

## 🚀 Features

### Core Features
- **Simple connection setup** with multiple configuration options
- **Fluent API** for common MongoDB operations
- **Type-safe operations** with Kotlin generics
- **Comprehensive error handling** with custom exceptions
- **Input validation** for all operations
- **Async support** for non-blocking operations

### Advanced Features
- **Advanced Query Builder** with fluent API
- **Aggregation Pipeline Support** with builder pattern
- **Batch Operations** for efficient bulk operations
- **Index Management** utilities
- **Transaction Support** with ACID compliance
- **Performance Monitoring** and health checks
- **Backup and Restore** utilities
- **Connection Management** with pooling and retry logic
- **Pagination Support** with built-in pagination utilities

## 📋 Requirements

- **Kotlin 1.9+** or **Java 17+**
- **MongoDB 4.0+** server
- **Gradle 8.0+** (for building)

## 📦 Installation

### Gradle (Kotlin DSL)
```kotlin
dependencies {
    implementation("com.github.guneyilmaz0:mongos-kotlin:VERSION")
}
```

### Gradle (Groovy)
```gradle
dependencies {
    implementation 'com.github.guneyilmaz0:mongos-kotlin:VERSION'
}
```

### Maven
```xml
<dependency>
    <groupId>com.github.guneyilmaz0</groupId>
    <artifactId>mongos-kotlin</artifactId>
    <version>VERSION</version>
</dependency>
```

## 🎯 Quick Start

### Basic Connection and Operations
```kotlin
import net.guneyilmaz0.mongos4k.MongoS

// Connect to MongoDB
val mongoS = MongoS("myDatabase")

// Store data
data class User(val name: String, val age: Int) : MongoSObject()
val user = User("John Doe", 30)
mongoS.set("users", "user1", user)

// Retrieve data
val retrievedUser = mongoS.get<User>("users", "user1")
println("Retrieved: ${retrievedUser.name}, ${retrievedUser.age}")

// Check existence
val exists = mongoS.exists("users", "user1")

// Close connection
mongoS.close()
```

### Advanced Configuration
```kotlin
import net.guneyilmaz0.mongos4k.config.MongoSConfig

// Production configuration
val config = MongoSConfig.production()
val mongoS = MongoS(config, "myDatabase")

// Custom configuration
val customConfig = MongoSConfig(
    host = "mongodb.example.com",
    port = 27017,
    maxConnectionsPerHost = 100,
    connectionTimeout = Duration.ofSeconds(30)
)
val customMongoS = MongoS(customConfig, "myDatabase")
```

## 🔧 Advanced Usage

### Query Builder
```kotlin
import net.guneyilmaz0.mongos4k.query.MongoSQueryBuilder

val queryBuilder = MongoSQueryBuilder.build {
    eq("active", true)
    gte("age", 18)
    `in`("department", listOf("IT", "HR", "Finance"))
    regex("email", ".*@company.com")
    sortDesc("createdAt")
    limit(50)
}

val results = mongoS.find<User>("users", queryBuilder)
```

### Pagination
```kotlin
val paginatedResult = mongoS.findPaginated<User>(
    collection = "users",
    queryBuilder = MongoSQueryBuilder.build { eq("active", true) },
    page = 1,
    pageSize = 20
)

println("Page ${paginatedResult.page} of ${paginatedResult.totalPages}")
println("Total items: ${paginatedResult.totalCount}")
paginatedResult.items.forEach { user -> println(user.name) }
```

### Aggregation Pipeline
```kotlin
import net.guneyilmaz0.mongos4k.aggregation.MongoSAggregationBuilder

val aggregation = MongoSAggregationBuilder.build {
    match("active", true)
    groupWithCount("$department", "employeeCount")
    sortDesc("employeeCount")
    limit(10)
}

val results = mongoS.aggregate("users", aggregation)
```

### Batch Operations
```kotlin
// Bulk insert
val documents = listOf(
    Document("name", "User1").append("age", 25),
    Document("name", "User2").append("age", 30)
)
mongoS.batchOperations.bulkInsert("users", documents)

// Bulk upsert
val updates = mapOf(
    "user1" to User("John Updated", 31),
    "user2" to User("Jane Updated", 26)
)
mongoS.batchOperations.bulkUpsert("users", updates)
```

### Index Management
```kotlin
// Create indexes
mongoS.indexManager.createIndex("users", "email", unique = true)
mongoS.indexManager.createCompoundIndex("users", listOf(
    "department" to true,
    "salary" to false
))

// Text index for search
mongoS.indexManager.createTextIndex("users", listOf("name", "email"))

// TTL index for automatic cleanup
mongoS.indexManager.createTTLIndex("sessions", "expiresAt", 3600)

// List indexes
val indexes = mongoS.indexManager.listIndexes("users")
```

### Transaction Support
```kotlin
val result = mongoS.transactionManager.withTransaction { session ->
    // All operations within this block are transactional
    val user = mongoS.get<User>("users", "user1")
    val updatedUser = user.copy(salary = user.salary + 1000)
    mongoS.set("users", "user1", updatedUser)
    
    // Log the transaction
    mongoS.set("audit_log", UUID.randomUUID().toString(), 
        mapOf("action" to "salary_update", "userId" to "user1"))
    
    "Transaction completed successfully"
}
```

### Performance Monitoring
```kotlin
// Measure operation performance
val (result, metrics) = mongoS.performanceMonitor.measureOperation(
    "find_active_users",
    "users"
) {
    mongoS.find<User>("users", MongoSQueryBuilder.build { eq("active", true) })
}

println("Operation took: ${metrics.duration.toMillis()}ms")

// Health check
val healthCheck = mongoS.performanceMonitor.performHealthCheck()
println("Database healthy: ${healthCheck["healthy"]}")

// Database statistics
val dbStats = mongoS.performanceMonitor.getDatabaseStats()
println("Total documents: ${dbStats.documents}")
println("Database size: ${dbStats.dataSize} bytes")
```

### Backup and Restore
```kotlin
// Export collection
val exportedCount = mongoS.backupUtility.exportCollection(
    "users", 
    "/backup/users_backup.json"
)

// Import collection
val importedCount = mongoS.backupUtility.importCollection(
    "users", 
    "/backup/users_backup.json"
)

// Full database backup
val backupResults = mongoS.backupUtility.exportDatabase("/backup/full_backup")
```

## 🛡️ Error Handling

The library provides comprehensive error handling with custom exception types:

```kotlin
import net.guneyilmaz0.mongos4k.exceptions.*

try {
    mongoS.get<User>("users", "nonexistent")
} catch (e: MongoSNotFoundException) {
    println("User not found: ${e.message}")
} catch (e: MongoSValidationException) {
    println("Validation error: ${e.message}")
} catch (e: MongoSOperationException) {
    println("Operation failed: ${e.message}")
} catch (e: MongoSConnectionException) {
    println("Connection error: ${e.message}")
} catch (e: MongoSSerializationException) {
    println("Serialization error: ${e.message}")
}
```

## 📊 Configuration Options

### Connection Configurations
```kotlin
// Development (default)
val devConfig = MongoSConfig.defaultLocal()

// Production optimized
val prodConfig = MongoSConfig.production()

// Testing optimized
val testConfig = MongoSConfig.testing()

// Custom configuration
val customConfig = MongoSConfig(
    host = "localhost",
    port = 27017,
    connectionTimeout = Duration.ofSeconds(30),
    maxConnectionsPerHost = 100,
    minConnectionsPerHost = 10,
    maxConnectionIdleTime = Duration.ofMinutes(10),
    retryWrites = true,
    retryReads = true
)
```

## 🔍 Validation

All operations include comprehensive input validation:

```kotlin
// These will throw MongoSValidationException
mongoS.set("", "key", "value")           // Empty collection name
mongoS.set("users", null, "value")       // Null key
mongoS.set("users", "key", null)         // Null value
mongoS.findPaginated<User>("users", query, 0, 20)  // Invalid page number
```

## 📚 API Reference

### Core Classes

#### MongoS
Main client class with connection management and database operations.

**Key Methods:**
- `set(collection, key, value)` - Store a document
- `get<T>(collection, key)` - Retrieve a document
- `exists(collection, key)` - Check document existence
- `find<T>(collection, queryBuilder)` - Advanced queries
- `findPaginated<T>(collection, queryBuilder, page, pageSize)` - Paginated queries
- `aggregate(collection, aggregationBuilder)` - Aggregation pipelines
- `count(collection, queryBuilder)` - Count documents

**Utility Properties:**
- `batchOperations` - Batch operation utilities
- `indexManager` - Index management utilities
- `transactionManager` - Transaction management
- `performanceMonitor` - Performance monitoring
- `backupUtility` - Backup and restore utilities

#### MongoSQueryBuilder
Fluent query builder for complex MongoDB queries.

**Key Methods:**
- `eq(field, value)` - Equality filter
- `ne(field, value)` - Not equal filter
- `gt(field, value)` - Greater than filter
- `gte(field, value)` - Greater than or equal filter
- `lt(field, value)` - Less than filter
- `lte(field, value)` - Less than or equal filter
- `in(field, values)` - In filter
- `regex(field, pattern)` - Regular expression filter
- `and { ... }` - AND condition
- `or { ... }` - OR condition
- `sortAsc(field)` - Ascending sort
- `sortDesc(field)` - Descending sort
- `limit(count)` - Limit results
- `skip(count)` - Skip results

#### MongoSAggregationBuilder
Builder for MongoDB aggregation pipelines.

**Key Methods:**
- `match(filter)` - Match stage
- `project(projection)` - Project stage
- `sort(sort)` - Sort stage
- `limit(limit)` - Limit stage
- `group(id, accumulators)` - Group stage
- `groupWithCount(id, countField)` - Group with count
- `groupWithSum(id, sumField, field)` - Group with sum
- `groupWithAvg(id, avgField, field)` - Group with average
- `unwind(field)` - Unwind array field
- `lookup(from, localField, foreignField, as)` - Left outer join

## 🚀 Performance Tips

1. **Use batch operations** for multiple documents
2. **Create appropriate indexes** for your queries
3. **Use pagination** for large result sets
4. **Monitor performance** with the built-in monitoring tools
5. **Use transactions** only when necessary
6. **Configure connection pooling** for production

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

- **GitHub**: [guneyilmaz0](https://github.com/guneyilmaz0)

## 📝 Changelog

### Version 2.0.0
- Added comprehensive query builder with fluent API
- Added aggregation pipeline support
- Added batch operations for efficient bulk operations
- Added index management utilities
- Added transaction support with ACID compliance
- Added performance monitoring and health checks
- Added backup and restore utilities
- Added comprehensive error handling with custom exceptions
- Added input validation for all operations
- Added pagination support
- Enhanced connection management with advanced configuration options
- Improved documentation with comprehensive examples

### Version 1.x.x
- Basic CRUD operations
- Simple connection setup
- JSON serialization support