package io.yarburart.reiproxy.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()
}

@Dao
interface RequestHistoryDao {
    @Query("SELECT * FROM request_history WHERE projectId = :projectId ORDER BY requestTimestamp DESC")
    fun getHistoryByProject(projectId: Long): Flow<List<RequestHistoryEntity>>

    @Query("SELECT * FROM request_history WHERE projectId = :projectId ORDER BY requestTimestamp DESC LIMIT :limit")
    suspend fun getHistoryByProjectLimit(projectId: Long, limit: Int): List<RequestHistoryEntity>

    @Query("SELECT * FROM request_history WHERE id = :id")
    suspend fun getRequestById(id: String): RequestHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: RequestHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequests(requests: List<RequestHistoryEntity>)

    @Query("DELETE FROM request_history WHERE projectId = :projectId")
    suspend fun clearHistoryByProject(projectId: Long)

    @Query("DELETE FROM request_history WHERE id = :id")
    suspend fun deleteRequest(id: String)

    @Query("DELETE FROM request_history")
    suspend fun clearAllHistory()
}
