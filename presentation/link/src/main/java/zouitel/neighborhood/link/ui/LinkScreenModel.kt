package zouitel.neighborhood.link.ui

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.ScreenModel
import zouitel.neighborhood.domain.usecase.LinkUseCase
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.domain.utils.withAsync
import zouitel.neighborhood.link.mapper.LinkMapper
import zouitel.neighborhood.link.model.Link
import zouitel.neighborhood.link.utils.Constants.LINK_TAG
import zouitel.neighborhood.link.utils.Constants.UID
import zouitel.neighborhood.link.worker.LinkWorker
import zouitel.neighborhood.logic.withFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
class LinkScreenModel(
    application: Application,
    private val _observeAll_: LinkUseCase.ObserveAll,
    private val _observeByUid_: LinkUseCase.ObserveByUid,
    private val insert: LinkUseCase.Insert,
    private val delete: LinkUseCase.Delete,
    private val mapper: LinkMapper
) : ScreenModel {

    private val _observeAll: MutableStateFlow<List<Link>> = MutableStateFlow(emptyList())
    val observeAll: StateFlow<List<Link>> = _observeAll
        .withFlow(emptyList()) {
            withAsync {
                _observeAll_.invoke().collect { _observeAll.value = mapper.fromDomain(it) }
            }
        }

    private val _observeByUid: MutableStateFlow<List<Link>> = MutableStateFlow(emptyList())
    val observeByUid: StateFlow<List<Link>> = _observeByUid
        .withFlow(emptyList())

    private val workManager = WorkManager.getInstance(application)

    fun initializeUid(uid: String) = withAsync {
        _observeByUid_(uid).collect {
            _observeByUid.value = mapper.fromDomain(it)
        }
    }

    fun urlsProcessing(uid: String, text: String) {
        if (uid.isBlank() || text.isBlank()) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val worker = OneTimeWorkRequest.Builder(LinkWorker::class.java)
            .addTag(LINK_TAG)
            .setInputData(
                Data.Builder()
                    .putString(UID, uid)
                    .putString(TEXT, text)
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(constraints)
            .build()

        // Each request has its own WorkRequest ID. Unique work with KEEP could silently
        // discard a second note's URLs while the first request is still pending.
        workManager.enqueue(worker)
    }

    suspend fun sendAction(act: Action) {
        when (act) {
            is Action.Insert<*> -> insert(mapper.toDomain(act.data as Link))
            is Action.Delete<*> -> delete(mapper.toDomain(act.data as Link))
            else -> throw NotImplementedError("Action not implemented: $act")
        }
    }

    private companion object {
        const val TEXT = "text"
    }
}
