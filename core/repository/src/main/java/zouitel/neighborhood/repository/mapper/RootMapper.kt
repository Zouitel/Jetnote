package zouitel.neighborhood.repository.mapper

import zouitel.neighborhood.domain.model.Root as OutRoot
import zouitel.neighborhood.repository.model.Root as InRoot

class RootMapper {
    fun toDomain(data: InRoot): OutRoot = with(data) {
        OutRoot(isDeviceRooted)
    }
}