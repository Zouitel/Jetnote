package zouitel.neighborhood.search.utils

import android.content.Context
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.ui.workplace.DataScreenModel
import zouitel.neighborhood.ui.CommonConstants
import java.io.File

fun doubletNote(
    ctx: Context,
    dataModel: DataScreenModel,
    note: Data,
    uid: String,
    onAction : () -> Unit
) {
    dataModel.sendAction(Action.Insert(note.copy(uid = uid)))

    "${ctx.filesDir.path}/${CommonConstants.REC_DIR}/".apply {
        File("$this${note.uid}.${CommonConstants.MP3}").let {
            if (it.exists()) it.copyTo(File("$this${uid}.${CommonConstants.MP3}"))
        }
    }
    onAction.invoke()
}
