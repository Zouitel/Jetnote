package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.model.Data
import zouitel.neighborhood.domain.repository.DataRepository

sealed class DataUseCase {
    data class Insert(private val repo: DataRepository): DataUseCase() {
        suspend operator fun invoke(data: Data) = repo.insert(data)
    }

    data class InsertAll(private val repo: DataRepository): DataUseCase() {
        suspend operator fun invoke(data: List<Data>) = repo.insertAll(data)
    }

    data class CombineUser(private val repo: DataRepository): DataUseCase() {
        suspend operator fun invoke(userUid: String?) = repo.combineUser(userUid)
    }

    data class Archive(private val repo: DataRepository): DataUseCase() {
        suspend operator fun invoke(uid: String) = repo.archive(uid)
    }

    data class Rollback(private val repo: DataRepository): DataUseCase() {
        suspend operator fun invoke(uid: String) = repo.rollback(uid)
    }

    data class Delete(private val repo: DataRepository): DataUseCase() {
        suspend operator fun invoke(uid: String) = repo.delete(uid)
    }

    data class Erase(private val repo: DataRepository): DataUseCase() {
        suspend operator fun invoke() = repo.erase()
    }

    data class SweepDrafts(private val repo: DataRepository): DataUseCase() {
        suspend operator fun invoke() = repo.sweepDrafts()
    }

    class ReorderNote(private val repo: DataRepository) : DataUseCase() {
        suspend operator fun invoke(noteUid: String, position: Int) = repo.reorderNote(noteUid, position)
    }

    class ReorderNotes(private val repo: DataRepository) : DataUseCase() {
        suspend operator fun invoke(positions: Map<String, Int>) = repo.reorderNotes(positions)
    }
}