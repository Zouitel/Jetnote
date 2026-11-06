package zouitel.neighborhood.workspace.ui.workplace

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import com.google.firebase.auth.FirebaseAuth
import zouitel.neighborhood.domain.usecase.DataUseCase
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.domain.utils.withAsync
import zouitel.neighborhood.workspace.mapper.DataMapper
import zouitel.neighborhood.workspace.model.Data

@Stable
@Suppress("DeferredResultUnused")
class DataScreenModel(
    private val insert: DataUseCase.Insert,
    private val archive: DataUseCase.Archive,
    private val rollback: DataUseCase.Rollback,
    private val data: DataUseCase.Delete,
    private val erase: DataUseCase.Erase,
    private val mapper: DataMapper,
    private val firebaseAuth: FirebaseAuth
): ScreenModel {
    fun sendAction(act: Action) {
        val userUid = firebaseAuth.currentUser?.uid?.takeIf(String::isNotBlank) ?: return
        when(act) {
            is Action.Insert<*> -> withAsync {
                val source = act.data as Data
                if (source.userUid.isNotBlank() && source.userUid != "null" && source.userUid != userUid) {
                    return@withAsync
                }
                insert(mapper.toDomain(source.copy(userUid = userUid)))
            }
            is Action.Archive -> withAsync { archive(userUid, act.uid) }
            is Action.Rollback -> withAsync { rollback(userUid, act.uid) }
            is Action.DeleteByUid -> withAsync { data(userUid, act.uid) }
            is Action.Erase -> withAsync { erase(userUid) }
            else -> throw NotImplementedError("This event is not implemented: $act")
        }
    }
}