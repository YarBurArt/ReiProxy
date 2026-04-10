package io.yarburart.reiproxy.db

import io.yarburart.reiproxy.core.ProxyRequestRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapperTest {

    @Test
    fun proxyRequestRecord_toEntity_mapsAllFields() {
        val record = ProxyRequestRecord(
            id = "req_test_1",
            method = "POST",
            host = "api.example.com",
            url = "/v1/data",
            statusCode = 201,
            requestHeaders = "Content-Type: application/json",
            requestBody = "{\"key\": \"value\"}",
            responseHeaders = "Content-Type: application/json",
            responseBody = "{\"id\": 1}",
            mimeType = "json",
            length = 11,
            timestamp = 1700000000000L,
            rawRequest = "POST /v1/data HTTP/1.1\r\nContent-Type: application/json\r\n\r\n{\"key\": \"value\"}",
            rawResponse = "HTTP/1.1 201 Created\r\nContent-Type: application/json\r\n\r\n{\"id\": 1}",
        )

        val entity = record.toEntity(projectId = 42L)

        assertEquals("req_test_1", entity.id)
        assertEquals(42L, entity.projectId)
        assertEquals("POST", entity.method)
        assertEquals("api.example.com", entity.host)
        assertEquals("/v1/data", entity.url)
        assertEquals(201, entity.statusCode)
        assertEquals("Content-Type: application/json", entity.requestHeaders)
        assertEquals("{\"key\": \"value\"}", entity.requestBody)
        assertEquals(1700000000000L, entity.requestTimestamp)
        assertEquals(1700000000000L, entity.responseTimestamp)
    }

    @Test
    fun entity_toDomain_mapsAllFields() {
        val entity = RequestHistoryEntity(
            id = "req_test_2",
            projectId = 99L,
            method = "DELETE",
            host = "api.example.com",
            url = "/v1/resource/5",
            statusCode = 204,
            requestHeaders = "Authorization: Bearer xyz",
            requestBody = "",
            responseHeaders = "",
            responseBody = "",
            mimeType = "unknown",
            length = 0,
            requestTimestamp = 1700000001000L,
            responseTimestamp = 1700000001200L,
            rawRequest = "DELETE /v1/resource/5 HTTP/1.1\r\nAuthorization: Bearer xyz",
            rawResponse = "HTTP/1.1 204 No Content",
        )

        val record = entity.toDomain()

        assertEquals("req_test_2", record.id)
        assertEquals("DELETE", record.method)
        assertEquals("api.example.com", record.host)
        assertEquals("/v1/resource/5", record.url)
        assertEquals(204, record.statusCode)
        assertEquals("Authorization: Bearer xyz", record.requestHeaders)
        assertEquals("", record.requestBody)
        assertEquals(0, record.length)
        assertEquals(1700000001000L, record.timestamp)
        assertEquals("DELETE /v1/resource/5 HTTP/1.1\r\nAuthorization: Bearer xyz", record.rawRequest)
    }

    @Test
    fun roundTrip_preservesData() {
        val original = ProxyRequestRecord(
            id = "req_roundtrip",
            method = "PUT",
            host = "test.local",
            url = "/api/update",
            statusCode = 200,
            requestHeaders = "X-Custom: header",
            requestBody = "{\"updated\": true}",
            responseHeaders = "X-Response: ok",
            responseBody = "{\"result\": \"success\"}",
            mimeType = "json",
            length = 21,
            timestamp = 1700000005000L,
            rawRequest = "PUT /api/update HTTP/1.1\r\nX-Custom: header\r\n\r\n{\"updated\": true}",
            rawResponse = "HTTP/1.1 200 OK\r\nX-Response: ok\r\n\r\n{\"result\": \"success\"}",
        )

        val entity = original.toEntity(projectId = 7L)
        val restored = entity.toDomain()

        assertEquals(original.id, restored.id)
        assertEquals(original.method, restored.method)
        assertEquals(original.host, restored.host)
        assertEquals(original.url, restored.url)
        assertEquals(original.statusCode, restored.statusCode)
        assertEquals(original.requestHeaders, restored.requestHeaders)
        assertEquals(original.requestBody, restored.requestBody)
        assertEquals(original.responseHeaders, restored.responseHeaders)
        assertEquals(original.responseBody, restored.responseBody)
        assertEquals(original.mimeType, restored.mimeType)
        assertEquals(original.length, restored.length)
        assertEquals(original.timestamp, restored.timestamp)
        assertEquals(original.rawRequest, restored.rawRequest)
        assertEquals(original.rawResponse, restored.rawResponse)
    }

    @Test
    fun toEntity_usesProvidedProjectId_notFromRecord() {
        val record = ProxyRequestRecord(
            id = "req_1",
            method = "GET",
            host = "x.com",
            url = "/",
            statusCode = 200,
            requestHeaders = "",
            requestBody = "",
            responseHeaders = "",
            responseBody = "",
            mimeType = "unknown",
            length = 0,
            timestamp = 0L,
            rawRequest = "",
            rawResponse = "",
        )

        val entity = record.toEntity(projectId = 123L)
        assertEquals(123L, entity.projectId)
    }

    @Test
    fun toDomain_emptyFields_handledCorrectly() {
        val entity = RequestHistoryEntity(
            id = "req_empty",
            projectId = 1L,
            method = "GET",
            host = "",
            url = "",
            statusCode = 0,
            requestHeaders = "",
            requestBody = "",
            responseHeaders = "",
            responseBody = "",
            mimeType = "",
            length = 0,
            requestTimestamp = 0L,
            responseTimestamp = 0L,
            rawRequest = "",
            rawResponse = "",
        )

        val record = entity.toDomain()
        assertTrue(record.host.isBlank())
        assertTrue(record.url.isBlank())
        assertEquals(0, record.statusCode)
        assertEquals(0L, record.timestamp)
    }
}
