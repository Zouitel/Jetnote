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

    data class Archive(private val repo: DataRepository): DataUseCase() {
        suspend operator fun invoke(userUid: String, noteUid: String) = repo.archive(userUid, noteUid)
    }

    data class Rollback(private val repo: DataRepository): DataUseCase() {
        suspend operator fun invoke(userUid: String, noteUid: String) = repo.rollback(userUid, noteUid)
    }

    data class Delete(private val repo: DataRepository): DataUseCase() {
        suspend operator fun invoke(userUid: String, noteUid: String) = repo.delete(userUid, noteUid)
    }

    data class Erase(private val repo: DataRepository): DataUseCase() {
        suspend operator fun invoke(userUid: String) = repo.erase(userUid)
    }

    data class SweepDrafts(private val repo: DataRepository): DataUseCase() {
        suspend operator fun invoke(userUid: String) = repo.sweepDrafts(userUid)
    }

    class ReorderNote(private val repo: DataRepository) : DataUseCase() {
        suspend operator fun invoke(userUid: String, noteUid: String, position: Int) =
            repo.reorderNote(userUid, noteUid, position)
    }

    class ReorderNotes(private val repo: DataRepository) : DataUseCase() {
        suspend operator fun invoke(userUid: String, positions: Map<String, Int>) =
            repo.reorderNotes(userUid, positions)
    }
}