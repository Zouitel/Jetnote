package zouitel.neighborhood.database.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
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
import zouitel.neighborhood.database.model.Provider
import zouitel.neighborhood.database.model.Tag
import zouitel.neighborhood.database.dao.AssistantDao
import zouitel.neighborhood.database.dao.ConversationDao
import zouitel.neighborhood.database.dao.ModelDao
import zouitel.neighborhood.database.dao.ProviderDao

@Database(
    version = 11,
    entities = [
        Data::class,
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
}