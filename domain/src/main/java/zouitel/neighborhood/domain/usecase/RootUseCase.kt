package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.repository.RootRepository

sealed class RootUseCase {

    class RootUseCase(private val repository: RootRepository) {
        operator fun invoke() = repository.isDeviceRooted
    }
}