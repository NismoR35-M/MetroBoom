package com.metroN.boomingC.music

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.metroN.boomingC.music.metadata.AudioInfo
import com.metroN.boomingC.music.system.Indexer

@Module
@InstallIn(SingletonComponent::class)
class MusicModule {
    @Singleton @Provides fun musicRepository() = MusicRepository.new()
    @Singleton @Provides fun indexer() = Indexer.new()
    @Provides fun settings(@ApplicationContext context: Context) = MusicSettings.from(context)
    @Provides
    fun audioInfoProvider(@ApplicationContext context: Context) = AudioInfo.Provider.from(context)
}
