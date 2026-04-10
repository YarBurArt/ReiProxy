package io.yarburart.reiproxy.data

import io.yarburart.reiproxy.core.ProxyRequestRecord
import io.yarburart.reiproxy.db.ProjectDao
import io.yarburart.reiproxy.db.ProjectEntity
import io.yarburart.reiproxy.db.RequestHistoryDao
import io.yarburart.reiproxy.db.RequestHistoryEntity
import io.yarburart.reiproxy.db.toDomain
import io.yarburart.reiproxy.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Project(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
)

class ProjectRepository(private val projectDao: ProjectDao) {

    fun getAllProjects(): Flow<List<Project>> {
        return projectDao.getAllProjects().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getProjectById(id: Long): Project? {
        return projectDao.getProjectById(id)?.toDomain()
    }

    suspend fun insertProject(name: String, description: String): Long {
        return projectDao.insertProject(ProjectEntity(name = name, description = description))
    }

    suspend fun updateProject(project: Project) {
        projectDao.updateProject(project.toEntity())
    }

    suspend fun deleteProject(project: Project) {
        projectDao.deleteProject(project.toEntity())
    }

    private fun ProjectEntity.toDomain() = Project(
        id = this.id,
        name = this.name,
        description = this.description,
        createdAt = this.createdAt,
    )

    private fun Project.toEntity() = ProjectEntity(
        id = this.id,
        name = this.name,
        description = this.description,
        createdAt = this.createdAt,
    )
}

class HistoryRepository(private val requestHistoryDao: RequestHistoryDao) {

    fun getHistoryByProject(projectId: Long): Flow<List<ProxyRequestRecord>> {
        return requestHistoryDao.getHistoryByProject(projectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getRequestById(id: String): ProxyRequestRecord? {
        return requestHistoryDao.getRequestById(id)?.toDomain()
    }

    suspend fun insertRequest(projectId: Long, record: ProxyRequestRecord) {
        requestHistoryDao.insertRequest(record.toEntity(projectId))
    }

    suspend fun insertRequests(projectId: Long, records: List<ProxyRequestRecord>) {
        requestHistoryDao.insertRequests(records.map { it.toEntity(projectId) })
    }

    suspend fun clearHistoryByProject(projectId: Long) {
        requestHistoryDao.clearHistoryByProject(projectId)
    }

    suspend fun deleteRequest(id: String) {
        requestHistoryDao.deleteRequest(id)
    }

    suspend fun clearAllHistory() {
        requestHistoryDao.clearAllHistory()
    }
}
