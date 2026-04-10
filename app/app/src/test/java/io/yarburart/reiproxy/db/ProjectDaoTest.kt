package io.yarburart.reiproxy.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.yarburart.reiproxy.db.ProjectEntity
import io.yarburart.reiproxy.db.ReiProxyDatabase
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
class ProjectDaoTest {

    private lateinit var database: ReiProxyDatabase
    private lateinit var dao: io.yarburart.reiproxy.db.ProjectDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ReiProxyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.projectDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertProject_returnsGeneratedId() = runTest {
        val project = ProjectEntity(name = "Test Project", description = "A test project")
        val id = dao.insertProject(project)
        assertTrue("Generated ID should be positive", id > 0)
    }

    @Test
    fun insertAndGetProject() = runTest {
        val project = ProjectEntity(name = "My Project", description = "Description here")
        val id = dao.insertProject(project)

        val retrieved = dao.getProjectById(id)
        assertNotNull(retrieved)
        assertEquals("My Project", retrieved?.name)
        assertEquals("Description here", retrieved?.description)
        assertNotNull(retrieved?.createdAt)
    }

    @Test
    fun getAllProjects_orderedByCreatedAtDesc() = runTest {
        val p1 = ProjectEntity(name = "First", description = "Created first", createdAt = 1000L)
        val p2 = ProjectEntity(name = "Second", description = "Created second", createdAt = 2000L)
        val p3 = ProjectEntity(name = "Third", description = "Created third", createdAt = 3000L)

        dao.insertProject(p1)
        dao.insertProject(p2)
        dao.insertProject(p3)

        val allProjects = dao.getAllProjects().first()
        assertEquals(3, allProjects.size)
        assertEquals("Third", allProjects[0].name)
        assertEquals("Second", allProjects[1].name)
        assertEquals("First", allProjects[2].name)
    }

    @Test
    fun getAllProjects_emptyInitially() = runTest {
        val projects = dao.getAllProjects().first()
        assertTrue(projects.isEmpty())
    }

    @Test
    fun getProjectById_nonExistent_returnsNull() = runTest {
        val result = dao.getProjectById(999L)
        assertNull(result)
    }

    @Test
    fun updateProject_changesNameAndDescription() = runTest {
        val project = ProjectEntity(name = "Old Name", description = "Old Desc")
        val id = dao.insertProject(project)

        val updated = project.copy(name = "New Name", description = "New Desc")
        dao.updateProject(updated)

        val retrieved = dao.getProjectById(id)
        assertEquals("New Name", retrieved?.name)
        assertEquals("New Desc", retrieved?.description)
    }

    @Test
    fun deleteProject_removesFromDatabase() = runTest {
        val project = ProjectEntity(name = "ToDelete", description = "Will be deleted")
        val id = dao.insertProject(project)

        val retrieved = dao.getProjectById(id)
        assertNotNull(retrieved)

        dao.deleteProject(retrieved!!)
        val afterDelete = dao.getProjectById(id)
        assertNull(afterDelete)
    }

    @Test
    fun deleteAllProjects_clearsEverything() = runTest {
        dao.insertProject(ProjectEntity(name = "P1", description = "D1"))
        dao.insertProject(ProjectEntity(name = "P2", description = "D2"))
        dao.insertProject(ProjectEntity(name = "P3", description = "D3"))

        assertEquals(3, dao.getAllProjects().first().size)

        dao.deleteAllProjects()
        assertTrue(dao.getAllProjects().first().isEmpty())
    }

    @Test
    fun insertProjectWithConflict_replacesExisting() = runTest {
        val p1 = ProjectEntity(id = 1L, name = "Original", description = "Original desc")
        dao.insertProject(p1)

        val p2 = ProjectEntity(id = 1L, name = "Replacement", description = "New desc")
        dao.insertProject(p2)

        val allProjects = dao.getAllProjects().first()
        assertEquals(1, allProjects.size)
        assertEquals("Replacement", allProjects[0].name)
    }

    @Test
    fun flowEmitsOnInsert() = runTest {
        val flow = dao.getAllProjects()

        // Initial state
        assertEquals(0, flow.first().size)

        // Insert and verify flow emits updated list
        dao.insertProject(ProjectEntity(name = "Dynamic", description = "Test"))
        val afterInsert = flow.first()
        assertEquals(1, afterInsert.size)
        assertEquals("Dynamic", afterInsert[0].name)
    }
}
