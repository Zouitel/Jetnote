package zouitel.neighborhood.database.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import zouitel.neighborhood.database.dao.DataDao
import zouitel.neighborhood.database.dao.LinkDao
import zouitel.neighborhood.database.dao.NoteAndTagDao
import zouitel.neighborhood.database.dao.NoteDao
import zouitel.neighborhood.database.dao.TagDao
import zouitel.neighborhood.database.model.Assistant
import zouitel.neighborhood.database.model.Conversation
import zouitel.neighborhood.database.model.Data
import zouitel.neighborhood.database.model.Link
import zouitel.neighborhood.database.model.Model
import zouitel.neighborhood.database.model.NoteAndTag
import zouitel.neighborhood.database.model.NoteFts
import zouitel.neighborhood.database.model.Provider
import zouitel.neighborhood.database.model.Tag
import zouitel.neighborhood.database.dao.AssistantDao
import zouitel.neighborhood.database.dao.ConversationDao
import zouitel.neighborhood.database.dao.ModelDao
import zouitel.neighborhood.database.dao.ProviderDao

@Database(
    version = 13,
    entities = [
        Data::class,
        NoteFts::class,
        Tag::class,
        NoteAndTag::class,
        Link::class,
        Assistant::class,
        Conversation::class,
        Model::class,
        Provider::class
    ],
    exportSchema = true
)
abstract class Database: RoomDatabase() {
    abstract fun getNoteDao(): DataDao
    abstract fun getLabelDao(): TagDao
    abstract fun getNoteAndLabelDao(): NoteAndTagDao
    abstract fun getEntityDao(): NoteDao
    abstract fun getLinkDao(): LinkDao
    abstract fun assistantDao(): AssistantDao
    abstract fun conversationDao(): ConversationDao
    abstract fun modelDao(): ModelDao
    abstract fun providerDao(): ProviderDao

    companion object {
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS NoteFts USING FTS4(uid, title, blocks, notindexed=uid)"
                )
                db.execSQL(
                    "INSERT INTO NoteFts(uid, title, blocks) " +
                        "SELECT uuid, title, blocks FROM note_data_table"
                )
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS note_fts_insert
                    AFTER INSERT ON note_data_table BEGIN
                        INSERT INTO NoteFts(uid, title, blocks)
                        VALUES (new.uuid, new.title, new.blocks);
                    END
                """.trimIndent())
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS note_fts_delete
                    AFTER DELETE ON note_data_table BEGIN
                        DELETE FROM NoteFts WHERE uid = old.uuid;
                    END
                """.trimIndent())
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS note_fts_update
                    AFTER UPDATE OF uuid, title, blocks ON note_data_table BEGIN
                        DELETE FROM NoteFts WHERE uid = old.uuid;
                        INSERT INTO NoteFts(uid, title, blocks)
                        VALUES (new.uuid, new.title, new.blocks);
                    END
                """.trimIndent())
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE note_data_table ADD COLUMN deleted_at INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_data_table_deleted_at ON note_data_table(deleted_at)")
            }
        }
    }
}
