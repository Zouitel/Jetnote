package zouitel.neighborhood.workspace.ui.workplace

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
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
    private val mapper: DataMapper
): ScreenModel {

    fun sendAction(act: Action) {
        when(act) {
            is Action.Insert<*> -> withAsync { insert(mapper.toDomain(act.data as Data)) }
            is Action.Archive -> withAsync { archive(act.uid) }
            is Action.Rollback -> withAsync { rollback(act.uid) }
            is Action.DeleteByUid -> withAsync { data(act.uid) }
            is Action.Erase -> withAsync { erase() }
            else -> throw NotImplementedError("This event is not implemented: $act")
        }
    }
}