package net.guneyilmaz0.mongos4k

import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.NoSuchElementException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MongoSIntegrationTest {

    private lateinit var mongoS: MongoS
    private val testDatabaseName = "mongos4k_integration_test_db"
    private val testCollectionName = "myTestCollection"

    data class User(val name: String, val age: Int) : MongoSObject()
    data class Product(val productName: String, val price: Double, val stock: Int) : MongoSObject()

    @BeforeAll
    fun setupAll() {
        try {
            mongoS = MongoS(testDatabaseName)
            assertTrue(mongoS.isConnected(), "MongoDB connection could not be established for tests.")
            println("MongoDB connection established for integration tests on database: $testDatabaseName")
        } catch (e: Exception) {
            println("Failed to connect to MongoDB for integration tests. Ensure MongoDB is running.")
            e.printStackTrace()
            fail("MongoDB connection failed, aborting tests.", e)
        }
    }

    @BeforeEach
    fun setupEach() {
        try {
            if (mongoS.database.listCollectionNames().contains(testCollectionName)) {
                mongoS.getCollection(testCollectionName).drop()
            }
            mongoS.getCollection("users").drop()
            mongoS.getCollection("products").drop()
        } catch (e: Exception) {
            println("Error cleaning up collection $testCollectionName before test: ${e.message}")
        }
    }

    @AfterAll
    fun tearDownAll() {
        if (::mongoS.isInitialized) {
            try {
                mongoS.database.drop()
                println("Dropped test database: $testDatabaseName")
            } catch (e: Exception) {
                println("Error dropping test database $testDatabaseName: ${e.message}")
            } finally {
                mongoS.close()
                println("MongoDB connection closed.")
            }
        }
    }

    @Test
    fun `set and get String value`() {
        val key = "greeting"
        val value = "Hello, MongoS4K!"
        val defaultValue = "Default"

        mongoS.set(testCollectionName, key, value)

        val retrievedValue = mongoS.get<String>(testCollectionName, key, defaultValue)
        assertEquals(value, retrievedValue)

        val nonExistent = mongoS.get<String>(testCollectionName, "nonexistent", defaultValue)
        assertEquals(defaultValue, nonExistent)

        assertThrows<NoSuchElementException>("Should throw for non-existent key without default") {
            mongoS.get<String>(testCollectionName, "anotherNonExistent")
        }
    }

    @Test
    fun `set and get MongoSObject`() {
        val userKey = "user123"
        val user = User("John Doe", 30)

        mongoS.set(testCollectionName, userKey, user)

        val retrievedUser = mongoS.get<User>(testCollectionName, userKey)
        assertNotNull(retrievedUser)
        assertEquals(user.name, retrievedUser.name)
        assertEquals(user.age, retrievedUser.age)
    }

    @Test
    fun `update existing value by setting with same key`() {
        val key = "counter"
        mongoS.set(testCollectionName, key, 10)
        var retrieved = mongoS.get<Int>(testCollectionName, key)
        assertEquals(10, retrieved)

        mongoS.set(testCollectionName, key, 20)
        retrieved = mongoS.get<Int>(testCollectionName, key)
        assertEquals(20, retrieved, "Value should be updated to 20.")
    }

    @Test
    fun `remove existing document`() {
        val key = "toBeRemoved"
        val value = "data"
        mongoS.set(testCollectionName, key, value)
        assertTrue(mongoS.exists(testCollectionName, key), "Document should exist before removal.")

        val removedDoc = mongoS.remove(testCollectionName, key)
        assertNotNull(removedDoc, "Removed document should not be null.")
        assertEquals(key, removedDoc?.get(Database.KEY_FIELD), "Removed document key mismatch.")


        assertFalse(mongoS.exists(testCollectionName, key), "Document should not exist after removal.")
    }

    @Test
    fun `remove non-existing document returns null`() {
        val removedDoc = mongoS.remove(testCollectionName, "nonExistentKey")
        assertNull(removedDoc, "Removing a non-existent document should return null.")
    }

    @Test
    fun `exists returns true for existing document and false for non-existing`() {
        val key = "checkerKey"
        mongoS.set(testCollectionName, key, "checkValue")
        assertTrue(mongoS.exists(testCollectionName, key), "Exists should return true for existing document.")
        assertFalse(mongoS.exists(testCollectionName, "nonCheckerKey"), "Exists should return false for non-existing document.")
    }

    @Test
    fun `getKeys returns list of keys`() {
        mongoS.set(testCollectionName, "key1", "value1")
        mongoS.set(testCollectionName, "key2", User("Jane", 25))
        mongoS.set(testCollectionName, 3, "value3")

        val keys = mongoS.getKeys(testCollectionName)
        assertEquals(3, keys.size, "Should retrieve 3 keys.")
        assertTrue(keys.contains("key1"))
        assertTrue(keys.contains("key2"))
    }

    @Test
    fun `getAllList retrieves all matching objects`() {
        val usersCollection = "users"
        val user1 = User("Alice", 28)
        val user2 = User("Bob", 32)
        val user3 = User("Alice", 22)

        mongoS.set(usersCollection, "alice1", user1)
        mongoS.set(usersCollection, "bob1", user2)
        mongoS.set(usersCollection, "alice2", user3)


        val allUsers = mongoS.getAllList<User>(usersCollection)
        assertEquals(3, allUsers.size, "Should retrieve 3 User objects.")
        assertTrue(allUsers.any { it.name == "Alice" && it.age == 28 })
        assertTrue(allUsers.any { it.name == "Bob" && it.age == 32 })
        assertTrue(allUsers.any { it.name == "Alice" && it.age == 22 })
    }

    @Test
    fun `getAllMap retrieves all matching objects as map`() {
        val productsCollection = "products"
        val prod1 = Product("Keyboard", 75.0, 50)
        val prod2 = Product("Mouse", 25.0, 100)

        mongoS.set(productsCollection, "P1001", prod1)
        mongoS.set(productsCollection, "P1002", prod2)

        val productMap = mongoS.getAllMap<Product>(productsCollection)
        assertEquals(2, productMap.size)
        assertNotNull(productMap["P1001"])
        assertEquals("Keyboard", productMap["P1001"]?.productName)
        assertNotNull(productMap["P1002"])
        assertEquals(25.0, productMap["P1002"]?.price)
    }

    @Test
    fun `insertMany and retrieve them`(): Unit = runBlocking {
        val userForDoc2 = User("Multi", 55)

        val userDocumentForDoc2 = mongoS.convertMongoSObjectToDocumentValue(userForDoc2)

        val documentsToInsert = listOf(
            Document(Database.KEY_FIELD, "doc1").append(Database.VALUE_FIELD, "Value 1"),
            Document(Database.KEY_FIELD, "doc2").append(Database.VALUE_FIELD, userDocumentForDoc2)
        )

        mongoS.insertMany(testCollectionName, documentsToInsert)

        val val1 = mongoS.get<String>(testCollectionName, "doc1")
        assertEquals("Value 1", val1, "Value for doc1 should be 'Value 1'")

        val retrievedUser = mongoS.get<User>(testCollectionName, "doc2")
        assertNotNull(retrievedUser, "Retrieved user should not be null")
        assertEquals(userForDoc2.name, retrievedUser.name, "User name should match")
        assertEquals(userForDoc2.age, retrievedUser.age, "User age should match")

        assertEquals(2, mongoS.getKeys(testCollectionName).size, "Should have 2 keys after insertMany")
    }

    @Test
    fun `getAnotherDatabase should return a new Database instance for different db`() {
        val anotherDbName = "mongos4k_another_test_db"
        val anotherDbInstance = mongoS.getAnotherDatabase(anotherDbName)
        assertNotNull(anotherDbInstance)
        assertEquals(anotherDbName, anotherDbInstance.database.name)

        anotherDbInstance.set("sampleCollection", "sampleKey", "sampleValue")
        val value = anotherDbInstance.get<String>("sampleCollection", "sampleKey")
        assertEquals("sampleValue", value)

        assertEquals(testDatabaseName, mongoS.database.name)

        try {
            anotherDbInstance.database.drop()
            println("Dropped 'another' test database: $anotherDbName")
        } catch (e: Exception) {
            println("Error dropping 'another' test database $anotherDbName: ${e.message}")
        }

    }

    @Test
    fun `isConnected should return true for active connection`() {
        assertTrue(mongoS.isConnected(), "isConnected should return true for an active connection.")
    }
}
