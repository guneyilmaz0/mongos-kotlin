package net.guneyilmaz0.mongos4k.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.ReadPreference
import com.mongodb.WriteConcern
import com.mongodb.connection.ConnectionPoolSettings
import com.mongodb.connection.ServerSettings
import com.mongodb.connection.SocketSettings
import java.time.Duration

/**
 * Configuration class for MongoDB connection settings.
 * Provides a fluent builder pattern for configuring MongoDB client settings.
 *
 * @author guneyilmaz0
 */
data class MongoSConfig(
    val host: String = "localhost",
    val port: Int = 27017,
    val uri: String? = null,
    val connectionTimeout: Duration = Duration.ofSeconds(30),
    val socketTimeout: Duration = Duration.ofSeconds(30),
    val serverSelectionTimeout: Duration = Duration.ofSeconds(30),
    val maxWaitTime: Duration = Duration.ofSeconds(120),
    val maxConnectionIdleTime: Duration = Duration.ofMinutes(10),
    val maxConnectionLifeTime: Duration = Duration.ofMinutes(30),
    val minConnectionsPerHost: Int = 5,
    val maxConnectionsPerHost: Int = 100,
    val retryWrites: Boolean = true,
    val retryReads: Boolean = true,
    val readPreference: ReadPreference = ReadPreference.primary(),
    val writeConcern: WriteConcern = WriteConcern.MAJORITY,
    val heartbeatFrequency: Duration = Duration.ofSeconds(10),
    val minHeartbeatFrequency: Duration = Duration.ofSeconds(1),
    val maxRetryAttempts: Int = 3,
    val retryDelayMs: Long = 1000,
    val enableSsl: Boolean = false,
    val sslInvalidHostnameAllowed: Boolean = false
) {
    
    /**
     * Creates MongoDB client settings from this configuration.
     *
     * @return Configured [MongoClientSettings] instance.
     */
    fun toMongoClientSettings(): MongoClientSettings {
        val builder = MongoClientSettings.builder()
        
        // Connection string or host/port
        if (uri != null) {
            builder.applyConnectionString(ConnectionString(uri))
        } else {
            builder.applyConnectionString(ConnectionString("mongodb://$host:$port"))
        }
        
        // Connection pool settings
        builder.applyToConnectionPoolSettings { poolBuilder ->
            poolBuilder.maxWaitTime(maxWaitTime.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
            poolBuilder.maxConnectionIdleTime(maxConnectionIdleTime.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
            poolBuilder.maxConnectionLifeTime(maxConnectionLifeTime.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
            poolBuilder.minSize(minConnectionsPerHost)
            poolBuilder.maxSize(maxConnectionsPerHost)
        }
        
        // Server settings
        builder.applyToServerSettings { serverBuilder ->
            serverBuilder.heartbeatFrequency(heartbeatFrequency.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
            serverBuilder.minHeartbeatFrequency(minHeartbeatFrequency.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
        }
        
        // Cluster settings
        builder.applyToClusterSettings { clusterBuilder ->
            clusterBuilder.serverSelectionTimeout(serverSelectionTimeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
        }
        
        // SSL settings
        if (enableSsl) {
            builder.applyToSslSettings { sslBuilder ->
                sslBuilder.enabled(true)
                sslBuilder.invalidHostNameAllowed(sslInvalidHostnameAllowed)
            }
        }
        
        // Read/Write preferences
        builder.readPreference(readPreference)
        builder.writeConcern(writeConcern)
        builder.retryWrites(retryWrites)
        builder.retryReads(retryReads)
        
        return builder.build()
    }
    
    companion object {
        /**
         * Creates a default configuration for local development.
         */
        fun defaultLocal(): MongoSConfig = MongoSConfig()
        
        /**
         * Creates a configuration optimized for production use.
         */
        fun production(): MongoSConfig = MongoSConfig(
            connectionTimeout = Duration.ofSeconds(10),
            socketTimeout = Duration.ofSeconds(60),
            serverSelectionTimeout = Duration.ofSeconds(10),
            maxWaitTime = Duration.ofSeconds(30),
            maxConnectionIdleTime = Duration.ofMinutes(5),
            maxConnectionLifeTime = Duration.ofMinutes(15),
            minConnectionsPerHost = 10,
            maxConnectionsPerHost = 200,
            heartbeatFrequency = Duration.ofSeconds(5),
            maxRetryAttempts = 5,
            retryDelayMs = 500
        )
        
        /**
         * Creates a configuration for testing environments.
         */
        fun testing(): MongoSConfig = MongoSConfig(
            connectionTimeout = Duration.ofSeconds(5),
            socketTimeout = Duration.ofSeconds(10),
            serverSelectionTimeout = Duration.ofSeconds(5),
            maxWaitTime = Duration.ofSeconds(10),
            maxConnectionIdleTime = Duration.ofMinutes(1),
            maxConnectionLifeTime = Duration.ofMinutes(2),
            minConnectionsPerHost = 1,
            maxConnectionsPerHost = 10,
            heartbeatFrequency = Duration.ofSeconds(2),
            maxRetryAttempts = 1,
            retryDelayMs = 100
        )
    }
}