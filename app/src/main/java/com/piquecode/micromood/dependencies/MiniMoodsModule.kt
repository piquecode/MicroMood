package com.piquecode.micromood.dependencies

import android.content.Context
import com.piquecode.micromood.data.MoodDao
import com.piquecode.micromood.data.MoodDatabase
import com.piquecode.micromood.data.MoodRepository
import com.piquecode.micromood.data.MoodRepositoryImpl
import com.piquecode.micromood.data.PreferenceDao
import com.piquecode.micromood.data.PreferenceDaoImpl
import com.piquecode.micromood.handlers.error.ErrorHandler
import com.piquecode.micromood.handlers.error.SentryErrorHandler
import com.piquecode.micromood.moods.MoodSelectionUseCase
import com.piquecode.micromood.moods.MoodSelectionUseCaseImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class MiniMoodsModule {
    @Provides
    fun providesErrorHandler(
        prefs: PreferenceDao,
        dispatchers: CoroutineDispatchers
    ): ErrorHandler = SentryErrorHandler(prefs, dispatchers)

    @Provides
    fun providesDispatchers(): CoroutineDispatchers = CoroutineDispatchers()

    @Provides
    @Singleton
    fun providesMoodDao(database: MoodDatabase): MoodDao = database.getMoodDao()

    @Provides
    fun providesMoodDatabase(@ApplicationContext context: Context): MoodDatabase =
        MoodDatabase.getInstance(context)

    @Provides
    fun provideMoodRepositoryImpl(impl: MoodRepositoryImpl): MoodRepository = impl

    @Provides
    fun providePreferencesDao(impl: PreferenceDaoImpl): PreferenceDao = impl

    @Provides
    fun providesMoodSelectionUseCase(impl: MoodSelectionUseCaseImpl): MoodSelectionUseCase = impl
}