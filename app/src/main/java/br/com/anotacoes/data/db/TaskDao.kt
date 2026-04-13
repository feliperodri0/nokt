package br.com.anotacoes.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("SELECT * FROM tasks ORDER BY task_date ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE task_date = :dateMillis")
    fun getTasksForDate(dateMillis: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks WHERE
            (recurrence_type = 'SINGLE' AND task_date = :dateMillis)
            OR recurrence_type = 'DAILY'
            OR recurrence_type = 'CUSTOM_WEEKLY'
            OR recurrence_type = 'MONTHLY'
            OR recurrence_type = 'YEARLY'
        ORDER BY
            CASE WHEN status = 'ACTIVE' THEN 0 ELSE 1 END,
            is_all_day DESC,
            start_hour ASC,
            start_minute ASC
    """)
    fun getTasksForDateWithRecurrence(dateMillis: Long): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET status = :status WHERE id = :taskId")
    suspend fun updateStatus(taskId: String, status: String)

    @Query("SELECT * FROM tasks WHERE task_date BETWEEN :startMillis AND :endMillis ORDER BY task_date ASC")
    fun getTasksForDateRange(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE task_date = :dateMillis AND is_all_day = 0 AND ((start_hour < :hour AND end_hour > :hour) OR (start_hour = :hour AND start_minute <= :minute AND end_hour > :hour) OR (start_hour = :hour AND end_hour = :hour AND end_minute > :minute))")
    fun getActiveTasksForDate(dateMillis: Long, hour: Int, minute: Int): Flow<List<TaskEntity>>

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}
