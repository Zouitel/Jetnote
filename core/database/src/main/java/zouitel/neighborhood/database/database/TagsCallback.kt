package zouitel.neighborhood.database.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import zouitel.neighborhood.database.database.TagsCallback.Companion.DEFAULT_TAGS
import zouitel.neighborhood.database.model.Tag

internal class TagsCallback(
    private val databaseProvider: () -> Database
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        CoroutineScope(Dispatchers.IO).launch {
            databaseProvider().getLabelDao().insertAll(DEFAULT_TAGS)
        }
    }

    companion object {
        /** Tags pre-loaded into every fresh installation. */
        private val DEFAULT_TAGS = listOf(
            Tag(1, "Project", null),
            Tag(2, "Family", null),
            Tag(3, "Home", null)
        )
    }
}