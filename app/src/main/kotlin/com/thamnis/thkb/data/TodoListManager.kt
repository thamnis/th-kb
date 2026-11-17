package com.thamnis.thkb.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TodoTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

class TodoListManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _tasks = MutableStateFlow(loadTasks())
    val tasks: StateFlow<List<TodoTask>> = _tasks.asStateFlow()

    fun addTask(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val current = _tasks.value.toMutableList()
        current.add(0, TodoTask(title = trimmed))
        saveTasks(current)
    }

    fun toggleTask(id: String) {
        val updated = _tasks.value.map { task ->
            if (task.id == id) task.copy(isCompleted = !task.isCompleted) else task
        }.sortedWith(compareBy({ it.isCompleted }, { -it.createdAt }))
        saveTasks(updated)
    }

    fun deleteTask(id: String) {
        val updated = _tasks.value.filterNot { it.id == id }
        saveTasks(updated)
    }

    fun clearCompleted() {
        val updated = _tasks.value.filterNot { it.isCompleted }
        saveTasks(updated)
    }

    fun restoreTasks(tasks: List<TodoTask>) {
        if (tasks.isEmpty()) return
        val current = _tasks.value.toMutableList()
        tasks.forEach { restored ->
            val idx = current.indexOfFirst { it.id == restored.id || it.title == restored.title }
            if (idx >= 0) {
                current[idx] = restored
            } else {
                current.add(restored)
            }
        }
        saveTasks(current.sortedWith(compareBy({ it.isCompleted }, { -it.createdAt })))
    }

    private fun saveTasks(list: List<TodoTask>) {
        _tasks.value = list
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("isCompleted", item.isCompleted)
                    put("createdAt", item.createdAt)
                },
            )
        }
        prefs.edit().putString(KEY_TODOS, arr.toString()).apply()
    }

    private fun loadTasks(): List<TodoTask> {
        val raw = prefs.getString(KEY_TODOS, null) ?: return defaultInitialTasks()
        val list = mutableListOf<TodoTask>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", UUID.randomUUID().toString())
                val title = obj.optString("title", "").trim()
                if (title.isNotEmpty()) {
                    list.add(
                        TodoTask(
                            id = id,
                            title = title,
                            isCompleted = obj.optBoolean("isCompleted", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        ),
                    )
                }
            }
        } catch (_: Throwable) {
            return defaultInitialTasks()
        }
        return if (list.isEmpty()) defaultInitialTasks() else list
    }

    private fun defaultInitialTasks(): List<TodoTask> = listOf(
        TodoTask(title = "Welcome to your keyboard Todo list!", isCompleted = false),
        TodoTask(title = "Tap task text to paste into any app", isCompleted = false),
        TodoTask(title = "Smooth animations on check and delete", isCompleted = true),
    )

    companion object {
        private const val PREFS_NAME = "th_kb_todo_prefs"
        private const val KEY_TODOS = "todo_items"
    }
}
