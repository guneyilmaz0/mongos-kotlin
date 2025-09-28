package net.guneyilmaz0.mongos4k

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Simplified unit tests for the Database class.
 * Tests core functionality without complex mocking.
 */
class DatabaseTest {
    @Nested
    @DisplayName("Database Configuration Tests")
    inner class DatabaseConfigurationTests {
        @Test
        fun `should have correct constants`() {
            assertEquals("key", Database.KEY_FIELD)
            assertEquals("value", Database.VALUE_FIELD)
        }

        @Test
        fun `should have gson instance`() {
            assertNotNull(Database.gson)
        }
    }

    @Nested
    @DisplayName("Utility Methods Tests")
    inner class UtilityMethodsTests {
        private lateinit var database: Database

        @BeforeEach
        fun setUp() {
            database = Database()
        }

        @Test
        fun `should convert object to document`() {
            data class TestObject(val name: String, val age: Int)
            val testObj = TestObject("John", 25)

            val document = database.convertToDocument(testObj)

            assertNotNull(document)
            assertTrue(document.toJson().contains("John"))
            assertTrue(document.toJson().contains("25"))
        }

        @Test
        fun `should convert document to json`() {
            val document = org.bson.Document().append("name", "John").append("age", 25)

            val json = database.convertDocumentToJson(document)

            assertNotNull(json)
            assertTrue(json.contains("John"))
            assertTrue(json.contains("25"))
        }

        @Test
        fun `should convert json to document`() {
            val json = """{"name": "John", "age": 25}"""

            val document = database.convertJsonToDocument(json)

            assertNotNull(document)
            assertEquals("John", document.getString("name"))
            assertEquals(25, document.getInteger("age"))
        }
    }
}
