package br.com.anotacoes.di

import android.content.Context
import androidx.room.Room
import br.com.anotacoes.data.alarm.AlarmIntentBuilder
import br.com.anotacoes.data.db.TaskDatabase.Companion.MIGRATION_1_2
import br.com.anotacoes.data.db.TaskDatabase.Companion.MIGRATION_2_3
import br.com.anotacoes.data.db.TaskDatabase.Companion.MIGRATION_3_4
import br.com.anotacoes.data.db.TaskDatabase.Companion.MIGRATION_4_5
import br.com.anotacoes.data.alarm.TaskAlarmManagerImpl
import br.com.anotacoes.data.db.TaskDatabase
import br.com.anotacoes.data.db.TaskDao
import br.com.anotacoes.data.db.ReminderDao
import br.com.anotacoes.data.db.converter.TypeConverters
import br.com.anotacoes.data.notification.TaskNotificationManagerImpl
import br.com.anotacoes.data.repository.TaskRepositoryImpl
import br.com.anotacoes.data.repository.ReminderRepositoryImpl
import br.com.anotacoes.data.icon.AppIconRepositoryImpl
import br.com.anotacoes.domain.port.TaskAlarmPort
import br.com.anotacoes.domain.port.TaskNotificationPort
import br.com.anotacoes.domain.repository.AppIconRepository
import br.com.anotacoes.domain.repository.ReminderRepository
import br.com.anotacoes.domain.repository.TaskRepository
import br.com.anotacoes.domain.usecase.AlarmScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTypeConverters(): TypeConverters = TypeConverters()

    @Provides
    @Singleton
    fun provideTaskDatabase(@ApplicationContext context: Context): TaskDatabase {
        return Room.databaseBuilder(
            context,
            TaskDatabase::class.java,
            "task_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: TaskDatabase): TaskDao = database.taskDao()

    @Provides
    @Singleton
    fun provideReminderDao(database: TaskDatabase): ReminderDao = database.reminderDao()

    @Provides
    @Singleton
    fun provideAlarmScheduler(): AlarmScheduler = AlarmScheduler()

    @Provides
    @Singleton
    fun provideAlarmIntentBuilder(): AlarmIntentBuilder = AlarmIntentBuilder()

    @Provides
    @Singleton
    fun provideTaskAlarmPort(
        @ApplicationContext context: Context,
        intentBuilder: AlarmIntentBuilder
    ): TaskAlarmPort {
        return TaskAlarmManagerImpl(context, intentBuilder)
    }

    @Provides
    @Singleton
    fun provideTaskNotificationPort(
        @ApplicationContext context: Context
    ): TaskNotificationPort {
        return TaskNotificationManagerImpl(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        converters: TypeConverters
    ): TaskRepository {
        return TaskRepositoryImpl(taskDao, converters)
    }

    @Provides
    @Singleton
    fun provideReminderRepository(
        dao: ReminderDao
    ): ReminderRepository {
        return ReminderRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): br.com.anotacoes.domain.repository.SettingsRepository {
        return br.com.anotacoes.data.settings.SettingsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideAppIconRepository(
        @ApplicationContext context: Context
    ): AppIconRepository {
        return AppIconRepositoryImpl(context)
    }
}
