package zouitel.neighborhood.database.utils

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

internal class MIGRATION_8_9 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)
        db.execSQL("ALTER TABLE note_data_table ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
    }
}