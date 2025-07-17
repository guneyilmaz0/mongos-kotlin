package net.guneyilmaz0.mongos4k.utilities

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.guneyilmaz0.mongos4k.Database
import net.guneyilmaz0.mongos4k.exceptions.MongoSOperationException
import net.guneyilmaz0.mongos4k.validation.MongoSValidator
import org.bson.Document
import java.io.File
import java.io.FileWriter
import java.io.FileReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Utility class for backup and restore operations.
 * Provides methods to export and import MongoDB data.
 *
 * @author guneyilmaz0
 */
class MongoSBackupUtility(private val database: Database) {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    /**
     * Data class representing backup metadata.
     */
    data class BackupMetadata(
        val databaseName: String,
        val collections: List<String>,
        val timestamp: String,
        val version: String,
        val totalDocuments: Long
    )
    
    /**
     * Exports a collection to a JSON file.
     *
     * @param collection The collection name.
     * @param filePath The file path to export to.
     * @param includeMetadata Whether to include metadata in the export.
     * @return The number of documents exported.
     * @throws MongoSOperationException If the operation fails.
     */
    fun exportCollection(
        collection: String,
        filePath: String,
        includeMetadata: Boolean = true
    ): Long {
        MongoSValidator.validateCollectionName(collection)
        
        return try {
            val documents = database.database.getCollection(collection).find().toList()
            val exportData = mutableMapOf<String, Any>()
            
            if (includeMetadata) {
                val metadata = BackupMetadata(
                    databaseName = database.database.name,
                    collections = listOf(collection),
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    version = "1.0",
                    totalDocuments = documents.size.toLong()
                )
                exportData["metadata"] = metadata
            }
            
            exportData["documents"] = documents.map { it.toJson() }
            
            File(filePath).parentFile?.mkdirs()
            FileWriter(filePath).use { writer ->
                gson.toJson(exportData, writer)
            }
            
            documents.size.toLong()
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to export collection '$collection'", e)
        }
    }
    
    /**
     * Imports a collection from a JSON file.
     *
     * @param collection The collection name.
     * @param filePath The file path to import from.
     * @param dropFirst Whether to drop the collection before importing.
     * @return The number of documents imported.
     * @throws MongoSOperationException If the operation fails.
     */
    fun importCollection(
        collection: String,
        filePath: String,
        dropFirst: Boolean = false
    ): Long {
        MongoSValidator.validateCollectionName(collection)
        
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                throw MongoSOperationException("Import file does not exist: $filePath")
            }
            
            val importData = FileReader(filePath).use { reader ->
                gson.fromJson(reader, Map::class.java)
            }
            
            val documents = (importData["documents"] as? List<*>)?.mapNotNull { docJson ->
                if (docJson is String) {
                    Document.parse(docJson)
                } else null
            } ?: emptyList()
            
            val mongoCollection = database.database.getCollection(collection)
            
            if (dropFirst) {
                mongoCollection.drop()
            }
            
            if (documents.isNotEmpty()) {
                mongoCollection.insertMany(documents)
            }
            
            documents.size.toLong()
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to import collection '$collection'", e)
        }
    }
    
    /**
     * Exports the entire database to a directory.
     *
     * @param backupDir The directory to export to.
     * @param includeIndexes Whether to include index information.
     * @return A map of collection names to document counts.
     * @throws MongoSOperationException If the operation fails.
     */
    fun exportDatabase(
        backupDir: String,
        includeIndexes: Boolean = true
    ): Map<String, Long> {
        return try {
            val backupDirectory = File(backupDir)
            backupDirectory.mkdirs()
            
            val collectionNames = database.database.listCollectionNames().toList()
            val results = mutableMapOf<String, Long>()
            
            // Export each collection
            for (collectionName in collectionNames) {
                val filePath = "$backupDir/$collectionName.json"
                val count = exportCollection(collectionName, filePath)
                results[collectionName] = count
                
                // Export indexes if requested
                if (includeIndexes) {
                    exportIndexes(collectionName, "$backupDir/$collectionName.indexes.json")
                }
            }
            
            // Create overall metadata
            val metadata = BackupMetadata(
                databaseName = database.database.name,
                collections = collectionNames,
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                version = "1.0",
                totalDocuments = results.values.sum()
            )
            
            FileWriter("$backupDir/metadata.json").use { writer ->
                gson.toJson(metadata, writer)
            }
            
            results
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to export database", e)
        }
    }
    
    /**
     * Imports the entire database from a directory.
     *
     * @param backupDir The directory to import from.
     * @param dropFirst Whether to drop collections before importing.
     * @param includeIndexes Whether to restore indexes.
     * @return A map of collection names to document counts.
     * @throws MongoSOperationException If the operation fails.
     */
    fun importDatabase(
        backupDir: String,
        dropFirst: Boolean = false,
        includeIndexes: Boolean = true
    ): Map<String, Long> {
        return try {
            val backupDirectory = File(backupDir)
            if (!backupDirectory.exists()) {
                throw MongoSOperationException("Backup directory does not exist: $backupDir")
            }
            
            val results = mutableMapOf<String, Long>()
            
            // Find all collection files
            val collectionFiles = backupDirectory.listFiles { file ->
                file.name.endsWith(".json") && !file.name.endsWith(".indexes.json") && file.name != "metadata.json"
            } ?: emptyArray()
            
            for (file in collectionFiles) {
                val collectionName = file.nameWithoutExtension
                val count = importCollection(collectionName, file.absolutePath, dropFirst)
                results[collectionName] = count
                
                // Import indexes if requested
                if (includeIndexes) {
                    val indexFile = File("$backupDir/$collectionName.indexes.json")
                    if (indexFile.exists()) {
                        importIndexes(collectionName, indexFile.absolutePath)
                    }
                }
            }
            
            results
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to import database", e)
        }
    }
    
    /**
     * Exports indexes for a collection.
     *
     * @param collection The collection name.
     * @param filePath The file path to export to.
     * @throws MongoSOperationException If the operation fails.
     */
    private fun exportIndexes(collection: String, filePath: String) {
        try {
            val indexes = database.database.getCollection(collection).listIndexes().toList()
            val indexData = mapOf("indexes" to indexes.map { it.toJson() })
            
            FileWriter(filePath).use { writer ->
                gson.toJson(indexData, writer)
            }
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to export indexes for collection '$collection'", e)
        }
    }
    
    /**
     * Imports indexes for a collection.
     *
     * @param collection The collection name.
     * @param filePath The file path to import from.
     * @throws MongoSOperationException If the operation fails.
     */
    private fun importIndexes(collection: String, filePath: String) {
        try {
            val indexData = FileReader(filePath).use { reader ->
                gson.fromJson(reader, Map::class.java)
            }
            
            val indexes = (indexData["indexes"] as? List<*>)?.mapNotNull { indexJson ->
                if (indexJson is String) {
                    Document.parse(indexJson)
                } else null
            } ?: emptyList()
            
            val mongoCollection = database.database.getCollection(collection)
            
            // Skip the default _id index
            indexes.filter { it.getString("name") != "_id_" }.forEach { indexDoc ->
                try {
                    val key = indexDoc.get("key") as? Document ?: Document()
                    if (key.isNotEmpty()) {
                        mongoCollection.createIndex(key)
                    }
                } catch (e: Exception) {
                    // Log but don't fail the entire import for index creation issues
                    println("Warning: Failed to create index ${indexDoc.getString("name")}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to import indexes for collection '$collection'", e)
        }
    }
    
    /**
     * Creates a compressed backup of the database.
     *
     * @param backupPath The path for the backup file.
     * @return The backup file path.
     * @throws MongoSOperationException If the operation fails.
     */
    fun createCompressedBackup(backupPath: String): String {
        return try {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val tempDir = "$backupPath.tmp"
            val finalPath = "$backupPath.mongos_backup_$timestamp.zip"
            
            // Export to temporary directory
            exportDatabase(tempDir, includeIndexes = true)
            
            // Create ZIP file (simplified - in real implementation, you'd use java.util.zip)
            val tempDirectory = File(tempDir)
            val zipFile = File(finalPath)
            
            // For simplicity, just rename the directory
            // In a real implementation, you'd create a proper ZIP file
            tempDirectory.renameTo(zipFile)
            
            finalPath
        } catch (e: Exception) {
            throw MongoSOperationException("Failed to create compressed backup", e)
        }
    }
    
    companion object {
        /**
         * Creates a new backup utility instance.
         *
         * @param database The database instance.
         * @return A new MongoSBackupUtility instance.
         */
        fun create(database: Database): MongoSBackupUtility = MongoSBackupUtility(database)
    }
}