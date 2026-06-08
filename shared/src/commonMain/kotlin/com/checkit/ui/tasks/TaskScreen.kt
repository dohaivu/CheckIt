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
import com.checkit.ui.TaskUiState
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.components.ViewOptionsMenu
import kotlinx.coroutines.launch

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
                    tags = state.board.tags,
                    selectedListId = state.selectedListId,
                    selectedFilterId = state.selectedFilterId,
                    selectedTagId = state.selectedTagId,
                    onListClick = { listId ->
                        viewModel.selectList(listId)
                        scope.launch { drawerState.close() }
                    },
                    onFilterClick = { filterId ->
                        viewModel.selectFilter(filterId)
                        scope.launch { drawerState.close() }
                    },
                    onTagClick = { tagId ->
                        viewModel.selectTag(tagId)
                        scope.launch { drawerState.close() }
                    },
                    onAddListClick = { viewModel.openNewList() },
                    onEditListClick = { list -> viewModel.openEditList(list) },
                    onAddTagClick = { viewModel.openNewTag() },
                    onEditTagClick = { tag -> viewModel.openEditTag(tag) }
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
                            state.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    actions = {
                        ViewOptionsMenu(
                            showCompleted = state.showCompleted,
                            onShowCompletedChange = viewModel::setShowCompleted,
                            availableViews = state.availableViews,
                            selectedView = state.selectedView,
                            selectView = viewModel::selectView,
                            sortOption = state.sortOption,
                            selectSortOption = viewModel::selectSortOption
                        )
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
                    onListDisplayTypeChange = viewModel::selectListDisplayType,
                    onTimelineCreateTask = viewModel::openNewTaskAt,
                    onTimelineTaskTimeChange = viewModel::updateTaskTime,
                    onTimelineNoteTimeChange = viewModel::updateNoteTime,
                    modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())
                )
            }
        }
    }

    state.listEditor?.let { listEditor ->
        TaskListEditorSheet(
            editor = listEditor,
            onDismiss = viewModel::dismissListEditor,
            onSave = viewModel::saveListEditor,
            onNameChange = viewModel::updateListEditorName,
            onColorChange = viewModel::updateListEditorColor,
            onIconChange = viewModel::updateListEditorIcon
        )
    }

    state.tagEditor?.let { tagEditor ->
        TaskTagEditorSheet(
            editor = tagEditor,
            onDismiss = viewModel::dismissTagEditor,
            onSave = viewModel::saveTagEditor,
            onNameChange = viewModel::updateTagEditorName,
            onColorChange = viewModel::updateTagEditorColor
        )
    }
}
