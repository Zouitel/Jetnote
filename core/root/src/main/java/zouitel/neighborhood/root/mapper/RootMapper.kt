package zouitel.neighborhood.root.mapper

import zouitel.neighborhood.root.model.Root as InRoot
import zouitel.neighborhood.repository.model.Root as OutRoot

class RootMapper {

    fun toRepo(root: InRoot) = OutRoot(isDeviceRooted = root.isDeviceRooted)
}