package zouitel.neighborhood.jotting.ui

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import zouitel.neighborhood.domain.usecase.DataUseCase
import zouitel.neighborhood.logic.events.UiEvent
import zouitel.neighborhood.logic.events.UiEventHandler
import zouitel.neighborhood.jotting.mapper.QuickDataMapper
import zouitel.neighborhood.jotting.model.QuickData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuickDataScreenModel(
    private val add: DataUseCase.Insert,
    private val mapper: QuickDataMapper
): ScreenModel, UiEventHandler<QuickData> {

    override fun sendUiEvent(event: UiEvent<QuickData>) {
        when(event) {
            is UiEvent.Insert -> performUiEvent { add(mapper.toDomain(event.data)) }
            else -> throw Exception("Not Implemented!")
        }
    }

    override fun performUiEvent(action: suspend () -> Unit) {
        screenModelScope.launch(Dispatchers.IO) { action.invoke() }
    }
}