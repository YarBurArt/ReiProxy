package io.yarburart.reiproxy.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.yarburart.reiproxy.core.ProxyRequestRecord

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "request_history",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class RequestHistoryEntity(
    @PrimaryKey
    val id: String,
    val projectId: Long,
    val method: String,
    val host: String,
    val url: String,
    val statusCode: Int,
    val requestHeaders: String,
    val requestBody: String,
    val responseHeaders: String,
    val responseBody: String,
    val mimeType: String,
    val length: Int,
    val requestTimestamp: Long,
    val responseTimestamp: Long,
    val rawRequest: String,
    val rawResponse: String,
)

// ---------- Mappers ----------

fun ProxyRequestRecord.toEntity(projectId: Long): RequestHistoryEntity = RequestHistoryEntity(
    id = this.id,
    projectId = projectId,
    method = this.method,
    host = this.host,
    url = this.url,
    statusCode = this.statusCode,
    requestHeaders = this.requestHeaders,
    requestBody = this.requestBody,
    responseHeaders = this.responseHeaders,
    responseBody = this.responseBody,
    mimeType = this.mimeType,
    length = this.length,
    requestTimestamp = this.timestamp,
    responseTimestamp = this.timestamp,
    rawRequest = this.rawRequest,
    rawResponse = this.rawResponse,
)

fun RequestHistoryEntity.toDomain(): ProxyRequestRecord = ProxyRequestRecord(
    id = this.id,
    method = this.method,
    host = this.host,
    url = this.url,
    statusCode = this.statusCode,
    requestHeaders = this.requestHeaders,
    requestBody = this.requestBody,
    responseHeaders = this.responseHeaders,
    responseBody = this.responseBody,
    mimeType = this.mimeType,
    length = this.length,
    timestamp = this.requestTimestamp,
    rawRequest = this.rawRequest,
    rawResponse = this.rawResponse,
)
