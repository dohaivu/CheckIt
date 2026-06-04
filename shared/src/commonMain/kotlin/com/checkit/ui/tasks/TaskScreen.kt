package com.checkit.ui.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.tab_tasks
import com.checkit.ui.TaskUiState
import com.checkit.ui.TaskWorkspaceView
import com.checkit.ui.components.TinyTopAppBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TaskScreen(
    state: TaskUiState,
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                TaskSidebar(
                    lists = state.board.lists,
                    filters = state.board.filters,
                    selectedListId = state.selectedListId,
                    selectedFilterId = state.selectedFilterId,
                    onListClick = { listId ->
                        viewModel.selectList(listId)
                        scope.launch { drawerState.close() }
                    },
                    onFilterClick = { filterId ->
                        viewModel.selectFilter(filterId)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                TaskActionFab(
                    onTaskClick = viewModel::openNewTask,
                    onNoteClick = viewModel::openNewNote
                )
            },
            topBar = {
                TinyTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open sidebar")
                        }
                    },
                    title = {
                        Text(
                            stringResource(Res.string.tab_tasks),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    actions = {
                        TaskWorkspaceView.entries.forEach { view ->
                            IconButton(onClick = { viewModel.selectView(view) }) {
                                Icon(
                                    imageVector = view.icon(),
                                    contentDescription = view.name,
                                    tint = if (state.selectedView == view) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (state.isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                TaskContent(
                    state = state,
                    onTaskClick = viewModel::openTask,
                    onNoteClick = viewModel::openNote,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
        }
    }

    state.editor?.let { editor ->
        TaskEditorSheet(
            editor = editor,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::saveEditor,
            onDelete = viewModel::deleteEditorItem,
            onTaskNameChange = viewModel::updateTaskName,
            onTaskDescriptionChange = viewModel::updateTaskDescription,
            onTaskDueDateChange = viewModel::updateTaskDueDate,
            onTaskStartTimeChange = viewModel::updateTaskStartTime,
            onTaskEndTimeChange = viewModel::updateTaskEndTime,
            onTaskRepeatChange = viewModel::updateTaskRepeat,
            onTaskPriorityChange = viewModel::updateTaskPriority,
            onNoteContentChange = viewModel::updateNoteContent
        )
    }
}
