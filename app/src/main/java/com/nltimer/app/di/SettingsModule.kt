package com.nltimer.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.nltimer.core.data.SettingsPrefs
import com.nltimer.core.data.SettingsPrefsImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okio.Path.Companion.toPath

/**
 * Hilt 设置依赖注入模块
 * 提供 DataStore 实例和 SettingsPrefs 接口绑定，注册在 SingletonComponent 中
 */
@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    /**
     * 提供基于文件的 Preferences DataStore 实例
     * 存储路径为应用文件目录下的 settings.preferences_pb
     */
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath {
        context.filesDir.resolve("settings.preferences_pb").absolutePath.toPath()
    }

    /**
     * 提供 SettingsPrefs 接口实现
     * 实际返回 SettingsPrefsImpl 实例，封装 DataStore 读写操作
     */
    @Provides
    @Singleton
    fun provideSettingsPrefs(
        dataStore: DataStore<Preferences>
    ): SettingsPrefs = SettingsPrefsImpl(dataStore)
}
