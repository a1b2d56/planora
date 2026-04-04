package com.devil.taskzio.data.repository

import com.devil.taskzio.data.database.dao.TaskDao
import com.devil.taskzio.data.database.entities.Task
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
