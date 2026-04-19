package zouitel.neighborhood.database.di

import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.lazyModule
import zouitel.neighborhood.database.database.Database
import zouitel.neighborhood.database.database.TagsCallback
import zouitel.neighborhood.database.datasourceImpl.AssistantDatasourceImpl
import zouitel.neighborhood.database.datasourceImpl.ConversationDatasourceImpl
import zouitel.neighborhood.database.datasourceImpl.DataDataSourceImpl
import zouitel.neighborhood.database.datasourceImpl.LinkDataSourceImpl
import zouitel.neighborhood.database.datasourceImpl.ModelDatasourceImpl
import zouitel.neighborhood.database.datasourceImpl.NoteAndTagDataSourceImpl
import zouitel.neighborhood.database.datasourceImpl.NoteDataSourceImpl
import zouitel.neighborhood.database.datasourceImpl.ProviderDatasourceImpl
import zouitel.neighborhood.database.datasourceImpl.TagDataSourceImpl
import zouitel.neighborhood.database.mapper.AssistantMapper
import zouitel.neighborhood.database.mapper.ConversationMapper
import zouitel.neighborhood.database.mapper.DataMapper
import zouitel.neighborhood.database.mapper.LinkMapper
import zouitel.neighborhood.database.mapper.ModelMapper
import zouitel.neighborhood.database.mapper.NoteAndTagMapper
import zouitel.neighborhood.database.mapper.NoteMapper
import zouitel.neighborhood.database.mapper.ProviderMapper
import zouitel.neighborhood.database.mapper.TagMapper
import zouitel.neighborhood.database.utils.Constants
import zouitel.neighborhood.repository.datasource.AssistantDatasource
import zouitel.neighborhood.repository.datasource.ConversationDatasource
import zouitel.neighborhood.repository.datasource.DataDataSource
import zouitel.neighborhood.repository.datasource.LinkDataSource
import zouitel.neighborhood.repository.datasource.ModelDatasource
import zouitel.neighborhood.repository.datasource.NoteAndTagDataSource
import zouitel.neighborhood.repository.datasource.NoteDataSource
import zouitel.neighborhood.repository.datasource.ProviderDatasource
import zouitel.neighborhood.repository.datasource.TagDataSource
import zouitel.neighborhood.security.encrition.FileEncryption

val databaseLazyModule = lazyModule {
    //DataSource.
    singleOf(::DataDataSourceImpl) bind DataDataSource::class
    singleOf(::NoteDataSourceImpl) bind NoteDataSource::class
    singleOf(::LinkDataSourceImpl) bind LinkDataSource::class
    singleOf(::TagDataSourceImpl) bind TagDataSource::class
    singleOf(::NoteAndTagDataSourceImpl) bind NoteAndTagDataSource::class
    singleOf(::AssistantDatasourceImpl) bind AssistantDatasource::class
    singleOf(::ConversationDatasourceImpl) bind ConversationDatasource::class
    singleOf(::ModelDatasourceImpl) bind ModelDatasource::class
    singleOf(::ProviderDatasourceImpl) bind ProviderDatasource::class

    //Dao's.
    single { get<Database>().getNoteDao() }
    single { get<Database>().getLabelDao() }
    single { get<Database>().getNoteAndLabelDao() }
    single { get<Database>().getEntityDao() }
    single { get<Database>().getLinkDao() }
    single { get<Database>().assistantDao() }
    single { get<Database>().conversationDao() }
    single { get<Database>().modelDao() }
    single { get<Database>().providerDao() }

    //Mappers
    factoryOf(::DataMapper)
    factoryOf(::LinkMapper)
    factoryOf(::TagMapper)
    factoryOf(::NoteAndTagMapper)
    factoryOf(::AssistantMapper)
    factoryOf(::ConversationMapper)
    factoryOf(::ModelMapper)
    factoryOf(::ProviderMapper)
    factory {
        NoteMapper(get(), get())
    }

    //Database.
    single {
        lateinit var db: Database
        db = Room.databaseBuilder(
            androidContext(),
            Database::class.java,
            Constants.DATABASE
        )
            .openHelperFactory(SupportOpenHelperFactory(FileEncryption(androidContext()).getCrypticPass()))
            .fallbackToDestructiveMigration(false)
            .fallbackToDestructiveMigrationOnDowngrade(false)
            .addCallback(TagsCallback { db })
            .build()
        db
    }
}