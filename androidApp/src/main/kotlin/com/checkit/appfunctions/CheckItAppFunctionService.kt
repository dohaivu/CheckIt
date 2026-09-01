package com.checkit.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.checkit.data.JournalEntryWriteInput
import com.checkit.data.TaskWriteInput
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskType
import com.checkit.domain.usecase.AddJournalEntryUseCase
import com.checkit.domain.usecase.AddTaskUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.ObserveWorkingTasksUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Entry point for CheckIt AppFunctions.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "CheckItAppFunctionService",
    appFunctionXmlFileName = "checkit_app_functions"
)
abstract class BaseCheckItAppFunctionService : AppFunctionService(), KoinComponent {

    private val addTask: AddTaskUseCase by inject()
    private val completeTask: CompleteTaskUseCase by inject()
    private val observeWorkingTasks: ObserveWorkingTasksUseCase by inject()
    private val addJournalEntry: AddJournalEntryUseCase by inject()

    /**
     * Creates a new task in CheckIt.
     *
     * @param title The title of the task to be created.
     * @return A success message confirming the task creation.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createTask(
        title: String
    ): String = withContext(Dispatchers.IO) {
        addTask(
            TaskWriteInput(
                name = title.trim(),
                description = "",
                subtasks = emptyList(),
                status = TaskStatus.Open,
                priority = TaskPriority.None,
                type = TaskType.Task,
                doDate = null,
                startTimeMinutes = null,
                endTimeMinutes = null,
                repeatRRule = null,
                reminders = emptyList(),
                tagIds = emptyList()
            )
        )
        "Task '$title' created successfully in CheckIt."
    }

    /**
     * Marks an existing task as completed in CheckIt.
     *
     * @param taskId The unique identifier of the task to complete.
     * @return A success message confirming the task completion.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun markTaskComplete(
        taskId: Long
    ): String = withContext(Dispatchers.IO) {
        completeTask(taskId)
        "Task with ID $taskId marked as complete."
    }

    /**
     * Retrieves a brief list of tasks scheduled for today.
     *
     * @return A list of [TaskBrief] objects for today.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getTasksForToday(): List<TaskBrief> = withContext(Dispatchers.IO) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        observeWorkingTasks(today).first().map { task ->
            TaskBrief(
                title = task.name,
                isCompleted = task.status == TaskStatus.Completed,
                priority = task.priority.name
            )
        }
    }

    /**
     * Adds a new journal entry to CheckIt for today.
     *
     * @param content The text content of the journal entry.
     * @return A success message.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun addJournalNote(
        content: String
    ): String = withContext(Dispatchers.IO) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        addJournalEntry(
            JournalEntryWriteInput(
                date = today,
                label = null,
                content = content,
                moods = emptyList(),
                tagIds = emptyList(),
                attachments = emptyList()
            )
        ).getOrThrow()
        "Journal entry added to CheckIt for $today."
    }
}

/**
 * A brief summary of a task.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class TaskBrief(
    /** The title of the task */
    val title: String,
    /** Whether the task is completed */
    val isCompleted: Boolean,
    /** The priority of the task */
    val priority: String
)
