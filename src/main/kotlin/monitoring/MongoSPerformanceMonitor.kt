package net.guneyilmaz0.mongos4k.monitoring

import net.guneyilmaz0.mongos4k.Database
import net.guneyilmaz0.mongos4k.exceptions.MongoSOperationException
import org.bson.Document
import java.time.Instant
import java.time.Duration

/**
 * Performance monitoring utilities for MongoDB operations.
 * Provides methods to measure and track operation performance.
 *
 * @author guneyilmaz0
 */
class MongoSPerformanceMonitor(private val database: Database) {
    
    /**
     * Data class representing operation metrics.
     */
    data class OperationMetrics(
        val operationName: String,
        val collection: String,
        val duration: Duration,
        val timestamp: Instant,
        val success: Boolean,
        val error: String? = null,
        val documentsProcessed: Long = 0,
        val memoryUsed: Long = 0
    )
    
    /**
     * Data class representing database statistics.
     */
    data class DatabaseStats(
        val databaseName: String,
        val collections: Int,
        val documents: Long,
        val avgObjSize: Double,
        val dataSize: Long,
        val storageSize: Long,
        val indexes: Int,
        val indexSize: Long
    )
    
    /**
     * Data class representing collection statistics.
     */
    data class CollectionStats(
        val name: String,
        val documents: Long,
        val size: Long,
        val avgObjSize: Double,
        val storageSize: Long,
        val indexes: Int,
        val totalIndexSize: Long
    )
    
    /**
     * Measures the execution time of an operation.
     *
     * @param T The return type of the operation.
     * @param operationName The name of the operation for tracking.
     * @param collection The collection name.
     * @param operation The operation to measure.
     * @return A pair containing the result and the metrics.
     * @throws MongoSOperationException If the operation fails.
     */
    inline fun <T> measureOperation(
        operationName: String,
        collection: String,
        operation: () -> T
    ): Pair<T, OperationMetrics> {
        val startTime = Instant.now()
        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        return try {
            val result = operation()
            val endTime = Instant.now()
            val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            
            val metrics = OperationMetrics(
                operationName = operationName,
                collection = collection,
                duration = Duration.between(startTime, endTime),
                timestamp = startTime,
                success = true,
                memoryUsed = endMemory - startMemory
            )
            
            Pair(result, metrics)
        } catch (e: Exception) {
            val endTime = Instant.now()
            val metrics = OperationMetrics(
                operationName = operationName,
                collection = collection,
                duration = Duration.between(startTime, endTime),
                timestamp = startTime,
                success = false,
                error = e.message
            )
            
            throw MongoSOperationException("Operation '$operationName' failed", e).also {
                // Log metrics even on failure
                println("Operation failed: $metrics")
            }
        }
    }
    
    /**
     * Gets database statistics.
     *
     * @return Database statistics.
     * @throws MongoSOperationException If the operation fails.
     */
    fun getDatabaseStats(): DatabaseStats {
        return try {
            val stats = database.database.runCommand(Document("dbStats", 1))
            
            DatabaseStats(
                databaseName = database.database.name,
                collections = stats.getInteger("collections", 0),
                documents = stats.getLong("objects"),
                avgObjSize = stats.getDouble("avgObjSize"),
                dataSize = stats.getLong("dataSize"),
                storageSize = stats.getLong("storageSize"),
                indexes = stats.getInteger("indexes", 0),
                indexSize = stats.getLong("indexSize")
            )
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to get database statistics", e)
        }
    }
    
    /**
     * Gets collection statistics.
     *
     * @param collection The collection name.
     * @return Collection statistics.
     * @throws MongoSOperationException If the operation fails.
     */
    fun getCollectionStats(collection: String): CollectionStats {
        return try {
            val stats = database.database.runCommand(Document("collStats", collection))
            
            CollectionStats(
                name = collection,
                documents = stats.getLong("count"),
                size = stats.getLong("size"),
                avgObjSize = stats.getDouble("avgObjSize"),
                storageSize = stats.getLong("storageSize"),
                indexes = stats.getInteger("nindexes", 0),
                totalIndexSize = stats.getLong("totalIndexSize")
            )
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to get collection statistics for '$collection'", e)
        }
    }
    
    /**
     * Gets server status information.
     *
     * @return Server status as a document.
     * @throws MongoSOperationException If the operation fails.
     */
    fun getServerStatus(): Document {
        return try {
            database.database.runCommand(Document("serverStatus", 1))
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to get server status", e)
        }
    }
    
    /**
     * Gets connection statistics.
     *
     * @return Connection statistics.
     * @throws MongoSOperationException If the operation fails.
     */
    fun getConnectionStats(): Map<String, Any> {
        return try {
            val serverStatus = getServerStatus()
            val connections = serverStatus.get("connections") as? Document ?: Document()
            
            mapOf(
                "current" to connections.getInteger("current", 0),
                "available" to connections.getInteger("available", 0),
                "totalCreated" to connections.getLong("totalCreated"),
                "active" to connections.getInteger("active", 0),
                "threaded" to connections.getInteger("threaded", 0),
                "exhaustIsMaster" to connections.getInteger("exhaustIsMaster", 0),
                "exhaustHello" to connections.getInteger("exhaustHello", 0),
                "awaitingTopologyChanges" to connections.getInteger("awaitingTopologyChanges", 0)
            )
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to get connection statistics", e)
        }
    }
    
    /**
     * Gets memory usage statistics.
     *
     * @return Memory usage statistics.
     * @throws MongoSOperationException If the operation fails.
     */
    fun getMemoryStats(): Map<String, Any> {
        return try {
            val serverStatus = getServerStatus()
            val mem = serverStatus.get("mem") as? Document ?: Document()
            
            mapOf(
                "resident" to mem.getInteger("resident", 0),
                "virtual" to mem.getInteger("virtual", 0),
                "supported" to mem.getBoolean("supported", false),
                "mapped" to mem.getInteger("mapped", 0),
                "mappedWithJournal" to mem.getInteger("mappedWithJournal", 0)
            )
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to get memory statistics", e)
        }
    }
    
    /**
     * Gets network statistics.
     *
     * @return Network statistics.
     * @throws MongoSOperationException If the operation fails.
     */
    fun getNetworkStats(): Map<String, Any> {
        return try {
            val serverStatus = getServerStatus()
            val network = serverStatus.get("network") as? Document ?: Document()
            
            mapOf(
                "bytesIn" to (network.getLong("bytesIn") ?: 0L),
                "bytesOut" to (network.getLong("bytesOut") ?: 0L),
                "physicalBytesIn" to (network.getLong("physicalBytesIn") ?: 0L),
                "physicalBytesOut" to (network.getLong("physicalBytesOut") ?: 0L),
                "numRequests" to (network.getLong("numRequests") ?: 0L),
                "compression" to (network.get("compression") ?: "unknown")
            )
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to get network statistics", e)
        }
    }
    
    /**
     * Gets operation counters.
     *
     * @return Operation counters.
     * @throws MongoSOperationException If the operation fails.
     */
    fun getOperationCounters(): Map<String, Any> {
        return try {
            val serverStatus = getServerStatus()
            val opcounters = serverStatus.get("opcounters") as? Document ?: Document()
            
            mapOf(
                "insert" to opcounters.getLong("insert"),
                "query" to opcounters.getLong("query"),
                "update" to opcounters.getLong("update"),
                "delete" to opcounters.getLong("delete"),
                "getmore" to opcounters.getLong("getmore"),
                "command" to opcounters.getLong("command")
            )
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to get operation counters", e)
        }
    }
    
    /**
     * Performs a simple health check.
     *
     * @return Health check result.
     * @throws MongoSOperationException If the operation fails.
     */
    fun performHealthCheck(): Map<String, Any> {
        return try {
            val startTime = Instant.now()
            val pingResult = database.database.runCommand(Document("ping", 1))
            val endTime = Instant.now()
            val pingTime = Duration.between(startTime, endTime)
            
            val serverStatus = getServerStatus()
            val uptime = serverStatus.getInteger("uptime", 0)
            val version = serverStatus.getString("version")
            
            mapOf(
                "healthy" to true,
                "pingTime" to pingTime.toMillis(),
                "uptime" to uptime,
                "version" to version,
                "timestamp" to Instant.now().toString()
            )
        } catch (e: Exception) {
            mapOf(
                "healthy" to false,
                "error" to (e.message ?: "Unknown error"),
                "timestamp" to Instant.now().toString()
            )
        }
    }
    
    /**
     * Gets a comprehensive performance summary.
     *
     * @return Performance summary.
     * @throws MongoSOperationException If the operation fails.
     */
    fun getPerformanceSummary(): Map<String, Any> {
        return try {
            mapOf(
                "database" to getDatabaseStats(),
                "connections" to getConnectionStats(),
                "memory" to getMemoryStats(),
                "network" to getNetworkStats(),
                "operations" to getOperationCounters(),
                "health" to performHealthCheck()
            )
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to get performance summary", e)
        }
    }
    
    companion object {
        /**
         * Creates a new performance monitor instance.
         *
         * @param database The database instance.
         * @return A new MongoSPerformanceMonitor instance.
         */
        fun create(database: Database): MongoSPerformanceMonitor = MongoSPerformanceMonitor(database)
    }
}