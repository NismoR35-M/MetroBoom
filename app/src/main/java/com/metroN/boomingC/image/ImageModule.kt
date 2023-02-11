package com.metroN.boomingC.image

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class ImageModule {
    @Provides fun settings(@ApplicationContext context: Context) = ImageSettings.from(context)
}
