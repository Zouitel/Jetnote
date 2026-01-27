package zouitel.neighborhood.repository.datasource

import kotlinx.coroutines.flow.Flow
import zouitel.neighborhood.repository.model.Root as OutRoot


interface RootDataSource {
    val isDeviceRooted: Flow<Result<OutRoot>>
}
