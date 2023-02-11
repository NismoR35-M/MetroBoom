package com.metroN.boomingC.search

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class SearchModule {
    @Provides fun engine(@ApplicationContext context: Context) = SearchEngine.from(context)
    @Provides fun settings(@ApplicationContext context: Context) = SearchSettings.from(context)
}
