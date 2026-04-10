package io.yarburart.reiproxy.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, RequestHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ReiProxyDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun requestHistoryDao(): RequestHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: ReiProxyDatabase? = null

        fun getDatabase(context: Context): ReiProxyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReiProxyDatabase::class.java,
                    "reiproxy_database",
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
