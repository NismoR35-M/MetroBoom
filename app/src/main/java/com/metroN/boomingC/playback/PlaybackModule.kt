package com.metroN.boomingC.playback

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.metroN.boomingC.playback.persist.PersistenceRepository
import com.metroN.boomingC.playback.state.PlaybackStateManager

@Module
@InstallIn(SingletonComponent::class)
class PlaybackModule {
    @Provides fun playbackStateManager() = PlaybackStateManager.get()

    @Provides fun settings(@ApplicationContext context: Context) = PlaybackSettings.from(context)

    @Provides
    fun persistenceRepository(@ApplicationContext context: Context) =
        PersistenceRepository.from(context)
}
