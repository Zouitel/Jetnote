package zouitel.neighborhood.security.mapper

import zouitel.neighborhood.domain.model.Root as OutRoot
import zouitel.neighborhood.security.model.Root as InRoot
import zouitel.neighborhood.security.mapper.base.Mapper

class RootMapper: Mapper.ReadOnly<OutRoot, InRoot> {
    override fun toView(data: OutRoot): InRoot = with(data) {
        InRoot(isDeviceRooted)
    }
}