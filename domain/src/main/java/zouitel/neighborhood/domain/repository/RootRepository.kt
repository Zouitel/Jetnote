package zouitel.neighborhood.domain.repository

import kotlinx.coroutines.flow.Flow
import zouitel.neighborhood.domain.model.Root

interface RootRepository {

    val isDeviceRooted: Flow<Result<Root>>
}
