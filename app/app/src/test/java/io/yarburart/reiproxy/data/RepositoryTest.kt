package io.yarburart.reiproxy.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.yarburart.reiproxy.core.ProxyRequestRecord
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
class RepositoryTest {

    private lateinit var database: ReiProxyDatabase
    private lateinit var projectRepo: ProjectRepository
    private lateinit var historyRepo: HistoryRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ReiProxyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        projectRepo = ProjectRepository(database.projectDao())
        historyRepo = HistoryRepository(database.requestHistoryDao())
    }

    @After
    fun closeDb() {
        database.close()
    }

    private fun createProxyRecord(id: String, method: String = "GET", url: String = "/test") =
        ProxyRequestRecord(
            id = id,
            method = method,
            host = "example.com",
            url = url,
            statusCode = 200,
            requestHeaders = "Host: example.com",
            requestBody = "",
            responseHeaders = "Content-Type: text/plain",
            responseBody = "OK",
            mimeType = "text",
            length = 2,
            timestamp = System.currentTimeMillis(),
            rawRequest = "$method $url HTTP/1.1",
            rawResponse = "HTTP/1.1 200 OK",
        )

    // ---------- ProjectRepository tests ----------

    @Test
    fun insertProject_returnsId() = runTest {
        val id = projectRepo.insertProject("Test Project", "A description")
        assertTrue(id > 0)
    }

    @Test
    fun getAllProjects_returnsInsertedProjects() = runTest {
        projectRepo.insertProject("P1", "Desc 1")
        projectRepo.insertProject("P2", "Desc 2")

        val projects = projectRepo.getAllProjects().first()
        assertEquals(2, projects.size)
        assertTrue(projects.all { it.id > 0 })
    }

    @Test
    fun getProjectById_returnsCorrectProject() = runTest {
        val id = projectRepo.insertProject("Lookup Test", "Find me")

        val project = projectRepo.getProjectById(id)
        assertNotNull(project)
        assertEquals("Lookup Test", project?.name)
    }

    @Test
    fun updateProject_changesFields() = runTest {
        val id = projectRepo.insertProject("Original", "Original desc")
        val project = projectRepo.getProjectById(id)!!

        projectRepo.updateProject(project.copy(name = "Updated", description = "New desc"))
        val updated = projectRepo.getProjectById(id)

        assertEquals("Updated", updated?.name)
        assertEquals("New desc", updated?.description)
    }

    @Test
    fun deleteProject_removesFromDb() = runTest {
        val id = projectRepo.insertProject("ToDelete", "Will be gone")
        val project = projectRepo.getProjectById(id)!!

        projectRepo.deleteProject(project)
        val afterDelete = projectRepo.getProjectById(id)
        assertNull(afterDelete)
    }

    @Test
    fun getAllProjects_flowEmitsOnInsert() = runTest {
        val flow = projectRepo.getAllProjects()
        assertEquals(0, flow.first().size)

        projectRepo.insertProject("Dynamic Project", "Test")
        val afterInsert = flow.first()
        assertEquals(1, afterInsert.size)
        assertEquals("Dynamic Project", afterInsert[0].name)
    }

    // ---------- HistoryRepository tests ----------

    @Test
    fun insertRequest_savesWithProjectId() = runTest {
        val projectId = projectRepo.insertProject("History Project", "Test")
        val record = createProxyRecord("req_hist_1")

        historyRepo.insertRequest(projectId, record)

        val history = historyRepo.getHistoryByProject(projectId).first()
        assertEquals(1, history.size)
        assertEquals("req_hist_1", history[0].id)
        assertEquals("GET", history[0].method)
    }

    @Test
    fun insertRequests_batchSaves() = runTest {
        val projectId = projectRepo.insertProject("Batch Project", "Test")
        val records = listOf(
            createProxyRecord("req_b1", "GET", "/api/1"),
            createProxyRecord("req_b2", "POST", "/api/2"),
            createProxyRecord("req_b3", "PUT", "/api/3"),
        )

        historyRepo.insertRequests(projectId, records)

        val history = historyRepo.getHistoryByProject(projectId).first()
        assertEquals(3, history.size)
    }

    @Test
    fun getRequestById_returnsCorrectRecord() = runTest {
        val projectId = projectRepo.insertProject("Lookup Project", "Test")
        val record = createProxyRecord("req_lookup", "DELETE", "/api/resource/1")
        historyRepo.insertRequest(projectId, record)

        val retrieved = historyRepo.getRequestById("req_lookup")
        assertNotNull(retrieved)
        assertEquals("DELETE", retrieved?.method)
        assertEquals("/api/resource/1", retrieved?.url)
    }

    @Test
    fun getRequestById_nonExistent_returnsNull() = runTest {
        val result = historyRepo.getRequestById("does_not_exist")
        assertNull(result)
    }

    @Test
    fun getHistoryByProject_filtersByProject() = runTest {
        val p1 = projectRepo.insertProject("Project A", "Test")
        val p2 = projectRepo.insertProject("Project B", "Test")

        historyRepo.insertRequest(p1, createProxyRecord("req_a1", "GET", "/a"))
        historyRepo.insertRequest(p1, createProxyRecord("req_a2", "POST", "/a2"))
        historyRepo.insertRequest(p2, createProxyRecord("req_b1", "GET", "/b"))

        val historyA = historyRepo.getHistoryByProject(p1).first()
        val historyB = historyRepo.getHistoryByProject(p2).first()

        assertEquals(2, historyA.size)
        assertEquals(1, historyB.size)
        assertTrue(historyA.all { it.url.startsWith("/a") })
        assertEquals("/b", historyB[0].url)
    }

    @Test
    fun clearHistoryByProject_onlyClearsSpecified() = runTest {
        val p1 = projectRepo.insertProject("Clear A", "Test")
        val p2 = projectRepo.insertProject("Clear B", "Test")

        historyRepo.insertRequest(p1, createProxyRecord("req_a"))
        historyRepo.insertRequest(p2, createProxyRecord("req_b1"))
        historyRepo.insertRequest(p2, createProxyRecord("req_b2"))

        historyRepo.clearHistoryByProject(p1)

        assertTrue(historyRepo.getHistoryByProject(p1).first().isEmpty())
        assertEquals(2, historyRepo.getHistoryByProject(p2).first().size)
    }

    @Test
    fun deleteRequest_removesSingleEntry() = runTest {
        val projectId = projectRepo.insertProject("Delete Project", "Test")
        historyRepo.insertRequest(projectId, createProxyRecord("req_del_1"))
        historyRepo.insertRequest(projectId, createProxyRecord("req_del_2"))

        historyRepo.deleteRequest("req_del_1")

        val remaining = historyRepo.getHistoryByProject(projectId).first()
        assertEquals(1, remaining.size)
        assertEquals("req_del_2", remaining[0].id)
    }

    @Test
    fun clearAllHistory_removesAllProjects() = runTest {
        val p1 = projectRepo.insertProject("All Clear A", "Test")
        val p2 = projectRepo.insertProject("All Clear B", "Test")

        historyRepo.insertRequest(p1, createProxyRecord("req_a"))
        historyRepo.insertRequest(p2, createProxyRecord("req_b"))

        historyRepo.clearAllHistory()

        assertTrue(historyRepo.getHistoryByProject(p1).first().isEmpty())
        assertTrue(historyRepo.getHistoryByProject(p2).first().isEmpty())
    }

    @Test
    fun cascadeDelete_projectRemovalClearsHistory() = runTest {
        val projectId = projectRepo.insertProject("Cascade Project", "Will cascade")
        historyRepo.insertRequest(projectId, createProxyRecord("req_c1"))
        historyRepo.insertRequest(projectId, createProxyRecord("req_c2"))

        assertEquals(2, historyRepo.getHistoryByProject(projectId).first().size)

        val project = projectRepo.getProjectById(projectId)!!
        projectRepo.deleteProject(project)

        val remaining = historyRepo.getHistoryByProject(projectId).first()
        assertTrue("History should be cascade-deleted", remaining.isEmpty())
    }

    @Test
    fun historyFlow_emitsOnInsert() = runTest {
        val projectId = projectRepo.insertProject("Flow Project", "Test")
        val flow = historyRepo.getHistoryByProject(projectId)

        assertEquals(0, flow.first().size)

        historyRepo.insertRequest(projectId, createProxyRecord("req_flow"))
        val afterInsert = flow.first()
        assertEquals(1, afterInsert.size)
        assertEquals("req_flow", afterInsert[0].id)
    }

    @Test
    fun domainRecords_roundTripViaRepository() = runTest {
        val projectId = projectRepo.insertProject("RoundTrip Project", "Test")
        val original = ProxyRequestRecord(
            id = "req_rt",
            method = "PATCH",
            host = "api.roundtrip.dev",
            url = "/v2/items/42",
            statusCode = 200,
            requestHeaders = "Content-Type: application/json\nX-Request-Id: abc-123",
            requestBody = "{\"action\": \"update\"}",
            responseHeaders = "Content-Type: application/json",
            responseBody = "{\"id\": 42, \"status\": \"updated\"}",
            mimeType = "json",
            length = 32,
            timestamp = 1700000099000L,
            rawRequest = "PATCH /v2/items/42 HTTP/1.1\r\nContent-Type: application/json\r\n\r\n{\"action\": \"update\"}",
            rawResponse = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n{\"id\": 42, \"status\": \"updated\"}",
        )

        historyRepo.insertRequest(projectId, original)
        val history = historyRepo.getHistoryByProject(projectId).first()
        val retrieved = history[0]

        assertEquals(original.id, retrieved.id)
        assertEquals(original.method, retrieved.method)
        assertEquals(original.host, retrieved.host)
        assertEquals(original.url, retrieved.url)
        assertEquals(original.statusCode, retrieved.statusCode)
        assertEquals(original.requestHeaders, retrieved.requestHeaders)
        assertEquals(original.requestBody, retrieved.requestBody)
        assertEquals(original.responseHeaders, retrieved.responseHeaders)
        assertEquals(original.responseBody, retrieved.responseBody)
        assertEquals(original.mimeType, retrieved.mimeType)
        assertEquals(original.length, retrieved.length)
        assertEquals(original.timestamp, retrieved.timestamp)
        assertEquals(original.rawRequest, retrieved.rawRequest)
        assertEquals(original.rawResponse, retrieved.rawResponse)
    }
}
