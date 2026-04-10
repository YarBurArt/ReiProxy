package io.yarburart.reiproxy.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.yarburart.reiproxy.db.ProjectEntity
import io.yarburart.reiproxy.db.ReiProxyDatabase
import io.yarburart.reiproxy.db.RequestHistoryEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RequestHistoryDaoTest {

    private lateinit var database: ReiProxyDatabase
    private lateinit var historyDao: io.yarburart.reiproxy.db.RequestHistoryDao
    private lateinit var projectDao: io.yarburart.reiproxy.db.ProjectDao

    private lateinit var project1Id: Long
    private lateinit var project2Id: Long

    @Before
    fun createDb() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ReiProxyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        historyDao = database.requestHistoryDao()
        projectDao = database.projectDao()

        // Create two projects to test filtering
        project1Id = projectDao.insertProject(ProjectEntity(name = "Project 1", description = "Desc 1"))
        project2Id = projectDao.insertProject(ProjectEntity(name = "Project 2", description = "Desc 2"))
    }

    @After
    fun closeDb() {
        database.close()
    }

    private fun createHistoryEntity(
        id: String,
        projectId: Long,
        method: String = "GET",
        url: String = "/api/test",
        timestamp: Long = System.currentTimeMillis(),
    ) = RequestHistoryEntity(
        id = id,
        projectId = projectId,
        method = method,
        host = "example.com",
        url = url,
        statusCode = 200,
        requestHeaders = "Host: example.com",
        requestBody = "",
        responseHeaders = "Content-Type: application/json",
        responseBody = "{\"status\": \"ok\"}",
        mimeType = "json",
        length = 15,
        requestTimestamp = timestamp,
        responseTimestamp = timestamp + 100,
        rawRequest = "GET /api/test HTTP/1.1\r\nHost: example.com",
        rawResponse = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n{\"status\": \"ok\"}",
    )

    @Test
    fun insertRequest_savesCorrectly() = runTest {
        val entity = createHistoryEntity("req_1", project1Id)
        historyDao.insertRequest(entity)

        val retrieved = historyDao.getRequestById("req_1")
        assertNotNull(retrieved)
        assertEquals("req_1", retrieved?.id)
        assertEquals(project1Id, retrieved?.projectId)
        assertEquals("GET", retrieved?.method)
        assertEquals("/api/test", retrieved?.url)
        assertEquals(200, retrieved?.statusCode)
        assertEquals(15, retrieved?.length)
    }

    @Test
    fun insertRequests_batchInsert() = runTest {
        val entities = listOf(
            createHistoryEntity("req_1", project1Id),
            createHistoryEntity("req_2", project1Id),
            createHistoryEntity("req_3", project1Id),
        )
        historyDao.insertRequests(entities)

        val all = historyDao.getHistoryByProject(project1Id).first()
        assertEquals(3, all.size)
    }

    @Test
    fun getHistoryByProject_filtersCorrectly() = runTest {
        // Insert for both projects
        historyDao.insertRequest(createHistoryEntity("req_p1_1", project1Id, url = "/api/p1"))
        historyDao.insertRequest(createHistoryEntity("req_p1_2", project1Id, url = "/api/p1b"))
        historyDao.insertRequest(createHistoryEntity("req_p2_1", project2Id, url = "/api/p2"))

        val p1History = historyDao.getHistoryByProject(project1Id).first()
        val p2History = historyDao.getHistoryByProject(project2Id).first()

        assertEquals(2, p1History.size)
        assertEquals(1, p2History.size)
        assertTrue(p1History.all { it.projectId == project1Id })
        assertTrue(p2History.all { it.projectId == project2Id })
    }

    @Test
    fun getHistoryByProject_orderedByTimestampDesc() = runTest {
        historyDao.insertRequest(createHistoryEntity("req_1", project1Id, timestamp = 1000L))
        historyDao.insertRequest(createHistoryEntity("req_2", project1Id, timestamp = 3000L))
        historyDao.insertRequest(createHistoryEntity("req_3", project1Id, timestamp = 2000L))

        val history = historyDao.getHistoryByProject(project1Id).first()
        assertEquals(3, history.size)
        assertEquals("req_2", history[0].id) // 3000
        assertEquals("req_3", history[1].id) // 2000
        assertEquals("req_1", history[2].id) // 1000
    }

    @Test
    fun getHistoryByProjectLimit_respectsLimit() = runTest {
        for (i in 1..10) {
            historyDao.insertRequest(createHistoryEntity("req_$i", project1Id, timestamp = i.toLong()))
        }

        val limited = historyDao.getHistoryByProjectLimit(project1Id, 3)
        assertEquals(3, limited.size)
        // Should be the 3 most recent (highest timestamps)
        assertEquals("req_10", limited[0].id)
        assertEquals("req_9", limited[1].id)
        assertEquals("req_8", limited[2].id)
    }

    @Test
    fun getRequestById_nonExistent_returnsNull() = runTest {
        val result = historyDao.getRequestById("non_existent")
        assertNull(result)
    }

    @Test
    fun clearHistoryByProject_onlyClearsSpecifiedProject() = runTest {
        historyDao.insertRequest(createHistoryEntity("req_p1", project1Id))
        historyDao.insertRequest(createHistoryEntity("req_p2_1", project2Id))
        historyDao.insertRequest(createHistoryEntity("req_p2_2", project2Id))

        historyDao.clearHistoryByProject(project1Id)

        val p1History = historyDao.getHistoryByProject(project1Id).first()
        val p2History = historyDao.getHistoryByProject(project2Id).first()

        assertTrue(p1History.isEmpty())
        assertEquals(2, p2History.size)
    }

    @Test
    fun deleteRequest_removesSingleEntry() = runTest {
        historyDao.insertRequest(createHistoryEntity("req_1", project1Id))
        historyDao.insertRequest(createHistoryEntity("req_2", project1Id))

        historyDao.deleteRequest("req_1")

        val remaining = historyDao.getHistoryByProject(project1Id).first()
        assertEquals(1, remaining.size)
        assertEquals("req_2", remaining[0].id)
    }

    @Test
    fun clearAllHistory_removesEverything() = runTest {
        historyDao.insertRequest(createHistoryEntity("req_1", project1Id))
        historyDao.insertRequest(createHistoryEntity("req_2", project1Id))
        historyDao.insertRequest(createHistoryEntity("req_3", project2Id))

        historyDao.clearAllHistory()

        assertTrue(historyDao.getHistoryByProject(project1Id).first().isEmpty())
        assertTrue(historyDao.getHistoryByProject(project2Id).first().isEmpty())
    }

    @Test
    fun insertRequestWithConflict_replacesExisting() = runTest {
        val original = createHistoryEntity("req_1", project1Id, method = "GET", url = "/original")
        historyDao.insertRequest(original)

        val replacement = createHistoryEntity("req_1", project1Id, method = "POST", url = "/replaced")
        historyDao.insertRequest(replacement)

        val retrieved = historyDao.getRequestById("req_1")
        assertEquals("POST", retrieved?.method)
        assertEquals("/replaced", retrieved?.url)
    }

    @Test
    fun flowEmitsOnInsert() = runTest {
        val flow = historyDao.getHistoryByProject(project1Id)

        assertEquals(0, flow.first().size)

        historyDao.insertRequest(createHistoryEntity("req_new", project1Id))
        val afterInsert = flow.first()
        assertEquals(1, afterInsert.size)
        assertEquals("req_new", afterInsert[0].id)
    }

    @Test
    fun flowEmitsOnDelete() = runTest {
        historyDao.insertRequest(createHistoryEntity("req_1", project1Id))
        val flow = historyDao.getHistoryByProject(project1Id)
        assertEquals(1, flow.first().size)

        historyDao.deleteRequest("req_1")
        val afterDelete = flow.first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun cascadeDelete_projectDeletionRemovesHistory() = runTest {
        historyDao.insertRequest(createHistoryEntity("req_1", project1Id))
        historyDao.insertRequest(createHistoryEntity("req_2", project1Id))

        // Verify history exists
        assertEquals(2, historyDao.getHistoryByProject(project1Id).first().size)

        // Delete the project — FK CASCADE should remove history
        val project = projectDao.getProjectById(project1Id)
        projectDao.deleteProject(project!!)

        // History should be gone too
        val remaining = historyDao.getHistoryByProject(project1Id).first()
        assertTrue("History should be cascade-deleted with project", remaining.isEmpty())
    }

    @Test
    fun allFieldsPreserved_roundTrip() = runTest {
        val entity = RequestHistoryEntity(
            id = "req_full",
            projectId = project1Id,
            method = "POST",
            host = "api.example.com",
            url = "/v1/users?page=1",
            statusCode = 201,
            requestHeaders = "Content-Type: application/json\nAuthorization: Bearer token",
            requestBody = "{\"name\": \"John\"}",
            responseHeaders = "Content-Type: application/json\nLocation: /v1/users/1",
            responseBody = "{\"id\": 1, \"name\": \"John\"}",
            mimeType = "json",
            length = 28,
            requestTimestamp = 1700000000000L,
            responseTimestamp = 1700000000150L,
            rawRequest = "POST /v1/users?page=1 HTTP/1.1\r\nContent-Type: application/json\r\n\r\n{\"name\": \"John\"}",
            rawResponse = "HTTP/1.1 201 Created\r\nContent-Type: application/json\r\n\r\n{\"id\": 1, \"name\": \"John\"}",
        )
        historyDao.insertRequest(entity)

        val retrieved = historyDao.getRequestById("req_full")
        assertNotNull(retrieved)
        assertEquals("POST", retrieved?.method)
        assertEquals("api.example.com", retrieved?.host)
        assertEquals("/v1/users?page=1", retrieved?.url)
        assertEquals(201, retrieved?.statusCode)
        assertEquals("Content-Type: application/json\nAuthorization: Bearer token", retrieved?.requestHeaders)
        assertEquals("{\"name\": \"John\"}", retrieved?.requestBody)
        assertEquals(28, retrieved?.length)
        assertEquals(1700000000000L, retrieved?.requestTimestamp)
        assertEquals(1700000000150L, retrieved?.responseTimestamp)
    }
}
