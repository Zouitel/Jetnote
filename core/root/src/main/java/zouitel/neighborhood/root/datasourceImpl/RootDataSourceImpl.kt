package zouitel.neighborhood.root.datasourceImpl

import zouitel.neighborhood.root.RootManager
import zouitel.neighborhood.root.mapper.RootMapper
import zouitel.neighborhood.repository.datasource.RootDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import zouitel.neighborhood.root.model.Root as InRoot
import zouitel.neighborhood.repository.model.Root as OutRoot

class RootDataSourceImpl(
    private val root: RootManager,
    private val rootMapper: RootMapper
): RootDataSource {

    override val isDeviceRooted: Flow<Result<OutRoot>>
        get() = root.isDeviceRooted().map { result ->
            result.mapCatching { rootMapper.toRepo(InRoot(it)) }
        }
}