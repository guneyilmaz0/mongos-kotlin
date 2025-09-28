package net.guneyilmaz0.mongos4k

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive unit tests for the MongoSObject class.
 * Tests all functionality including timestamps, validation, serialization, and utilities.
 */
class MongoSObjectTest {

    private lateinit var testObject: TestMongoSObject

    // Test implementation of MongoSObject for testing purposes
    private class TestMongoSObject(val name: String = "", val value: Int = 0) : MongoSObject() {
        
        override fun validate(): List<String> {
            val errors = super.validate().toMutableList()
            
            if (name.isBlank()) {
                errors.add("Name cannot be blank")
            }
            
            if (value < 0) {
                errors.add("Value must be non-negative")
            }
            
            return errors
        }
        
        override fun copy(): MongoSObject {
            val copy = TestMongoSObject(name, value)
            copy.mongoId = this.mongoId
            copy.createdAt = this.createdAt
            copy.updatedAt = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            copy.version = this.version + 1
            return copy
        }
    }

    @BeforeEach
    fun setUp() {
        testObject = TestMongoSObject("Test Object", 42)
    }

    @Nested
    @DisplayName("Initialization Tests")
    inner class InitializationTests {

        @Test
        fun `should initialize with default values`() {
            val obj = TestMongoSObject()
            
            assertNotNull(obj.mongoId)
            assertTrue(obj.mongoId.isNotEmpty())
            assertNotNull(obj.createdAt)
            assertNotNull(obj.updatedAt)
            assertEquals(1, obj.version)
        }

        @Test
        fun `should initialize with unique IDs`() {
            val obj1 = TestMongoSObject()
            val obj2 = TestMongoSObject()
            
            assertNotEquals(obj1.mongoId, obj2.mongoId)
        }

        @Test
        fun `should set creation and update timestamps`() {
            val obj = TestMongoSObject()
            
            assertNotNull(obj.createdAt)
            assertNotNull(obj.updatedAt)
            assertTrue(obj.createdAt.isNotEmpty())
            assertTrue(obj.updatedAt.isNotEmpty())
        }
    }

    @Nested
    @DisplayName("ID Management Tests")
    inner class IdManagementTests {

        @Test
        fun `should set custom ID`() {
            val customId = "custom-test-id"
            
            testObject.updateId(customId)
            
            assertEquals(customId, testObject.mongoId)
        }

        @Test
        fun `should update timestamp when setting custom ID`() {
            val originalUpdatedAt = testObject.updatedAt
            Thread.sleep(100) // Small delay to ensure timestamp difference
            
            testObject.updateId("new-id")
            
            assertNotEquals(originalUpdatedAt, testObject.updatedAt)
        }

        @Test
        fun `should increment version when setting custom ID`() {
            val originalVersion = testObject.version
            
            testObject.updateId("new-id")
            
            assertEquals(originalVersion + 1, testObject.version)
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    inner class ValidationTests {

        @Test
        fun `should validate successfully with valid data`() {
            val validObj = TestMongoSObject("Valid Name", 42)
            
            assertTrue(validObj.isValid())
            assertTrue(validObj.validate().isEmpty())
        }

        @Test
        fun `should fail validation with blank name`() {
            val invalidObj = TestMongoSObject("", 42)
            
            assertFalse(invalidObj.isValid())
            val errors = invalidObj.validate()
            assertTrue(errors.any { it.contains("Name cannot be blank") })
        }

        @Test
        fun `should fail validation with negative value`() {
            val invalidObj = TestMongoSObject("Valid Name", -1)
            
            assertFalse(invalidObj.isValid())
            val errors = invalidObj.validate()
            assertTrue(errors.any { it.contains("Value must be non-negative") })
        }

        @Test
        fun `should fail validation with blank ID`() {
            testObject.updateId("")
            
            assertFalse(testObject.isValid())
            val errors = testObject.validate()
            assertTrue(errors.any { it.contains("ID cannot be blank") })
        }
    }

    @Nested
    @DisplayName("Serialization Tests")
    inner class SerializationTests {

        @Test
        fun `should convert to JSON`() {
            val json = testObject.toJson()
            
            assertNotNull(json)
            assertTrue(json.contains("Test Object"))
            assertTrue(json.contains("42"))
            assertTrue(json.contains(testObject.mongoId))
        }

        @Test
        fun `should convert to Document`() {
            val document = testObject.toDocument()
            
            assertNotNull(document)
            assertEquals("Test Object", document.getString("name"))
            assertEquals(42, document.getInteger("value"))
            assertEquals(testObject.mongoId, document.getString("mongoId"))
        }

        @Test
        fun `should include timestamps in JSON`() {
            val json = testObject.toJson()
            
            assertTrue(json.contains("createdAt"))
            assertTrue(json.contains("updatedAt"))
            assertTrue(json.contains("version"))
        }
    }

    @Nested
    @DisplayName("Copy Operations Tests")
    inner class CopyOperationsTests {

        @Test
        fun `should create copy with updated metadata`() {
            val originalUpdatedAt = testObject.updatedAt
            val originalVersion = testObject.version
            
            Thread.sleep(100) // Small delay to ensure timestamp difference
            val copy = testObject.copy() as TestMongoSObject
            
            assertEquals(testObject.mongoId, copy.mongoId)
            assertEquals(testObject.createdAt, copy.createdAt)
            assertNotEquals(originalUpdatedAt, copy.updatedAt)
            assertEquals(originalVersion + 1, copy.version)
            assertEquals(testObject.name, copy.name)
            assertEquals(testObject.value, copy.value)
        }

        @Test
        fun `should create independent copy`() {
            val copy = testObject.copy() as TestMongoSObject
            val originalVersion = testObject.version
            
            // Modify original
            testObject.updateId("modified-id")
            
            // Copy should not be affected (different versions)
            assertEquals(originalVersion + 1, copy.version) // Copy got incremented version
            assertEquals(originalVersion + 1, testObject.version) // Original got incremented when modified
        }
    }

    @Nested
    @DisplayName("Utility Methods Tests")
    inner class UtilityMethodsTests {

        @Test
        fun `should provide comprehensive summary`() {
            val summary = testObject.getSummary()
            
            assertEquals(testObject.mongoId, summary["id"])
            assertEquals("TestMongoSObject", summary["type"])
            assertEquals(testObject.createdAt, summary["createdAt"])
            assertEquals(testObject.updatedAt, summary["updatedAt"])
            assertEquals(testObject.version, summary["version"])
            assertEquals(testObject.isValid(), summary["isValid"])
        }

        @Test
        fun `should provide meaningful toString`() {
            val str = testObject.toString()
            
            assertTrue(str.contains("TestMongoSObject"))
            assertTrue(str.contains(testObject.mongoId))
            assertTrue(str.contains(testObject.version.toString()))
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    inner class EqualityTests {

        @Test
        fun `should be equal when IDs match`() {
            val obj1 = TestMongoSObject("Name1", 1)
            val obj2 = TestMongoSObject("Name2", 2)
            
            obj2.updateId(obj1.mongoId)
            
            assertEquals(obj1, obj2)
            assertEquals(obj1.hashCode(), obj2.hashCode())
        }

        @Test
        fun `should not be equal when IDs differ`() {
            val obj1 = TestMongoSObject("Same", 42)
            val obj2 = TestMongoSObject("Same", 42)
            
            // Different IDs (generated automatically)
            assertNotEquals(obj1, obj2)
            assertNotEquals(obj1.hashCode(), obj2.hashCode())
        }

        @Test
        fun `should be equal to itself`() {
            assertEquals(testObject, testObject)
        }

        @Test
        fun `should not be equal to null or different type`() {
            assertNotEquals(testObject, null)
            assertNotEquals(testObject, "string")
            assertNotEquals(testObject, 42)
        }
    }

    @Nested
    @DisplayName("Timestamp Management Tests")
    inner class TimestampManagementTests {

        @Test
        fun `should maintain creation timestamp`() {
            val originalCreatedAt = testObject.createdAt
            
            // Simulate some operation that updates the object
            testObject.updateId("new-id")
            
            // Creation timestamp should remain unchanged
            assertEquals(originalCreatedAt, testObject.createdAt)
        }

        @Test
        fun `should update timestamp on modifications`() {
            val originalUpdatedAt = testObject.updatedAt
            Thread.sleep(100) // Small delay to ensure timestamp difference
            
            testObject.updateId("modified-id")
            
            assertNotEquals(originalUpdatedAt, testObject.updatedAt)
        }

        @Test
        fun `should increment version on modifications`() {
            val originalVersion = testObject.version
            
            testObject.updateId("modified-id")
            
            assertEquals(originalVersion + 1, testObject.version)
        }
    }
}