package com.planora.app.core.data.repository

import com.planora.app.core.data.database.dao.TaskDao
import com.planora.app.core.data.database.entities.Task
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(private val dao: TaskDao) {
    fun getAllTasks(): Flow<List<Task>> = dao.getAllTasks()

    suspend fun getTaskById(id: Long): Task?                  = dao.getTaskById(id)
    suspend fun insertTask(task: Task): Long                   = dao.insertTask(task)
    suspend fun updateTask(task: Task)                         = dao.updateTask(task)
    suspend fun deleteTask(task: Task)                         = dao.deleteTask(task)
    suspend fun updateTaskCompletion(id: Long, done: Boolean)  = dao.updateTaskCompletion(id, done)
}
