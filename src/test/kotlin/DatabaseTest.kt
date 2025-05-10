package net.guneyilmaz0.mongos4k

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.FindIterable
import org.bson.Document
import org.bson.conversions.Bson
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import kotlin.jvm.java

class DatabaseTest {

    private lateinit var mockDatabase: MongoDatabase
    private lateinit var mockCollection: MongoCollection<Document>
    private lateinit var mockFindIterable: FindIterable<Document>
    private lateinit var databaseInstance: Database

    @BeforeEach
    fun setup() {
        mockDatabase = mock<MongoDatabase>()
        mockCollection = mock<MongoCollection<Document>>()
        mockFindIterable = mock<FindIterable<Document>>()
        databaseInstance = Database()
        databaseInstance.init(mockDatabase)

        whenever(mockDatabase.getCollection("testCollection")).thenReturn(mockCollection)
        whenever(mockCollection.find(any<Bson>())).thenReturn(mockFindIterable)
    }

    @Test
    fun `test init assigns database correctly`() {
        databaseInstance.init(mockDatabase)
        assertEquals(mockDatabase, databaseInstance.database)
    }

//    @Test
//    fun `test isConnected returns true`() {
//        databaseInstance.init(mockDatabase)
//        assertEquals(true, databaseInstance.isConnected())
//    }

    @Test
    fun `test get returns correct value when document exists`() {
        val document = Document("key", "testKey").append("value", "testValue")
        whenever(mockFindIterable.first()).thenReturn(document)

        val result = databaseInstance.get<String>("testCollection", "testKey", "default")
        assertEquals("testValue", result)
    }


    @Test
    fun `test getList returns correct value`() {
        val document = Document("key", "testKey").append("value", listOf("testValue1", "testValue2"))
        whenever(mockFindIterable.first()).thenReturn(document)

        val result = databaseInstance.getList("testCollection", "testKey", String::class.java)
        assertEquals(listOf("testValue1", "testValue2"), result)
    }

    data class TestObject(val value: String) : MongoSObject()

    @Test
    fun `test getObject returns correct value`() {
        val document = Document("key", "testKey").append("value", databaseInstance.convertDocument(TestObject("testValue")))
        whenever(mockFindIterable.first()).thenReturn(document)

        val result = databaseInstance.get<TestObject>("testCollection", "testKey")
        assertEquals("testValue", result!!.value)
    }

}
