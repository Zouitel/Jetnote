package zouitel.neighborhood.repository.di

import zouitel.neighborhood.domain.repository.AssistantRepo
import zouitel.neighborhood.domain.repository.ConversationRepo
import zouitel.neighborhood.domain.repository.DataRepository
import zouitel.neighborhood.domain.repository.LinkRepository
import zouitel.neighborhood.domain.repository.ModelRepo
import zouitel.neighborhood.domain.repository.NoteAndTagRepository
import zouitel.neighborhood.domain.repository.NoteRepository
import zouitel.neighborhood.domain.repository.ProviderRepo
import zouitel.neighborhood.domain.repository.RootRepository
import zouitel.neighborhood.domain.repository.SettingsRepository
import zouitel.neighborhood.domain.repository.TagRepository
import zouitel.neighborhood.domain.usecase.AssistantUseCase
import zouitel.neighborhood.domain.usecase.ConversationUseCase
import zouitel.neighborhood.domain.usecase.DataUseCase
import zouitel.neighborhood.domain.usecase.LinkUseCase
import zouitel.neighborhood.domain.usecase.ModelUseCase
import zouitel.neighborhood.domain.usecase.NoteAndTagUseCase
import zouitel.neighborhood.domain.usecase.NoteUseCase
import zouitel.neighborhood.domain.usecase.ProviderUseCase
import zouitel.neighborhood.domain.usecase.RootUseCase
import zouitel.neighborhood.domain.usecase.SettingsUseCase
import zouitel.neighborhood.domain.usecase.TagUseCase
import zouitel.neighborhood.repository.repositoryImpl.AssistantRepositoryImpl
import zouitel.neighborhood.repository.repositoryImpl.ConversationRepositoryImpl
import zouitel.neighborhood.repository.repositoryImpl.ModelRepositoryImpl
import zouitel.neighborhood.repository.repositoryImpl.ProviderRepositoryImpl
import zouitel.neighborhood.repository.mapper.AssistantMapper
import zouitel.neighborhood.repository.mapper.ConversationMapper
import zouitel.neighborhood.repository.mapper.DataMapper
import zouitel.neighborhood.repository.mapper.LinkMapper
import zouitel.neighborhood.repository.mapper.ModelMapper
import zouitel.neighborhood.repository.mapper.NoteAndTagMapper
import zouitel.neighborhood.repository.mapper.NoteMapper
import zouitel.neighborhood.repository.mapper.ProviderMapper
import zouitel.neighborhood.repository.mapper.RootMapper
import zouitel.neighborhood.repository.mapper.SettingsMapper
import zouitel.neighborhood.repository.mapper.TagMapper
import zouitel.neighborhood.repository.repositoryImpl.DataRepositoryImpl
import zouitel.neighborhood.repository.repositoryImpl.LinkRepositoryImpl
import zouitel.neighborhood.repository.repositoryImpl.NoteAndTagRepositoryImpl
import zouitel.neighborhood.repository.repositoryImpl.NoteRepositoryImpl
import zouitel.neighborhood.repository.repositoryImpl.RootRepositoryImpl
import zouitel.neighborhood.repository.repositoryImpl.SettingsRepositoryImpl
import zouitel.neighborhood.repository.repositoryImpl.TagRepositoryImpl
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.lazyModule

val localRepoLazyModule = lazyModule {
    //Mappers.
    factoryOf(::DataMapper)
    factoryOf(::LinkMapper)
    factoryOf(::TagMapper)
    factoryOf(::NoteAndTagMapper)
    factoryOf(::RootMapper)
    factoryOf(::SettingsMapper)
    singleOf(::AssistantMapper)
    singleOf(::ConversationMapper)
    singleOf(::ModelMapper)
    singleOf(::ProviderMapper)
    factory {
        NoteMapper(get(), get())
    }

    //Repositories.
    singleOf(::DataRepositoryImpl) bind DataRepository::class
    singleOf(::LinkRepositoryImpl) bind LinkRepository::class
    singleOf(::NoteAndTagRepositoryImpl) bind NoteAndTagRepository::class
    singleOf(::NoteRepositoryImpl) bind NoteRepository::class
    singleOf(::RootRepositoryImpl) bind RootRepository::class
    singleOf(::TagRepositoryImpl) bind TagRepository::class
    singleOf(::SettingsRepositoryImpl) bind SettingsRepository::class
    singleOf(::AssistantRepositoryImpl) bind AssistantRepo::class
    singleOf(::ConversationRepositoryImpl) bind ConversationRepo::class
    singleOf(::ModelRepositoryImpl) bind ModelRepo::class
    singleOf(::ProviderRepositoryImpl) bind ProviderRepo::class

    //UseCases.
    factoryOf(DataUseCase::Insert)
    factoryOf(DataUseCase::InsertAll)
    factoryOf(DataUseCase::CombineUser)
    factoryOf(DataUseCase::Archive)
    factoryOf(DataUseCase::Rollback)
    factoryOf(DataUseCase::Delete)
    factoryOf(DataUseCase::Erase)
    factoryOf(DataUseCase::SweepDrafts)

    factoryOf(LinkUseCase::ObserveAll)
    factoryOf(LinkUseCase::GetAll)
    factoryOf(LinkUseCase::ObserveByUid)
    factoryOf(LinkUseCase::Insert)
    factoryOf(LinkUseCase::InsertAll)
    factoryOf(LinkUseCase::Delete)

    factoryOf(NoteAndTagUseCase::ObserveAll)
    factoryOf(NoteAndTagUseCase::GetAll)
    factoryOf(NoteAndTagUseCase::Insert)
    factoryOf(NoteAndTagUseCase::InsertAll)
    factoryOf(NoteAndTagUseCase::Delete)
    factoryOf(NoteAndTagUseCase::DeleteDrafts)

    factoryOf(NoteUseCase::ObserveAll)
    factoryOf(NoteUseCase::GetAll)
    factoryOf(NoteUseCase::ObserveByDefault)
    factoryOf(NoteUseCase::ObserveByName)
    factoryOf(NoteUseCase::ObserveByOldest)
    factoryOf(NoteUseCase::ObserveByNewest)
    factoryOf(NoteUseCase::ObserveArchives)
    factoryOf(NoteUseCase::ObserveByPriority)
    factoryOf(NoteUseCase::Cleanup)

    factoryOf(TagUseCase::ObserveAll)
    factoryOf(TagUseCase::GetAll)
    factoryOf(TagUseCase::Insert)
    factoryOf(TagUseCase::InsertAll)
    factoryOf(TagUseCase::Delete)

    factoryOf(RootUseCase::RootUseCase)

    factoryOf(SettingsUseCase::ObserveSettings)
    factoryOf(SettingsUseCase::SaveSettings)

    factoryOf(AssistantUseCase::Observe)
    factoryOf(AssistantUseCase::Upsert)

    factoryOf(ConversationUseCase::ObserveAll)
    factoryOf(ConversationUseCase::ObserveById)
    factoryOf(ConversationUseCase::Upsert)
    factoryOf(ConversationUseCase::Delete)
    factoryOf(ConversationUseCase::Cleanup)

    factoryOf(ModelUseCase::ObserveAll)
    factoryOf(ModelUseCase::ObserveById)
    factoryOf(ModelUseCase::ObserveByProviderId)
    factoryOf(ModelUseCase::Upsert)
    factoryOf(ModelUseCase::Delete)

    factoryOf(ProviderUseCase::ObserveAll)
    factoryOf(ProviderUseCase::ObserveById)
    factoryOf(ProviderUseCase::Upsert)
}