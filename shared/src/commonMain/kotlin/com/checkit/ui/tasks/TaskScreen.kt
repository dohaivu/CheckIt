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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.tasks.list.ListEditorSheet
import com.checkit.ui.tasks.list.ListViewModel
import com.checkit.ui.tasks.views.ViewOptionsMenu
import com.checkit.ui.theme.materialIcon
import com.checkit.ui.theme.toColor
import kotlinx.coroutines.launch

@Composable
internal fun TaskScreen(
    state: TaskUiState,
    viewModel: TaskViewModel,
    listViewModel: ListViewModel,
    onOpenTags: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState by listViewModel.uiState.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                TaskSidebar(
                    lists = state.board.lists,
                    isBoardSelected = state.selectedListId == null,
                    selectedListId = state.selectedListId,
                    isTagsSelected = false,
                    onBoardClick = {
                        viewModel.selectBoard()
                        scope.launch { drawerState.close() }
                    },
                    onListClick = { listId ->
                        viewModel.selectList(listId)
                        scope.launch { drawerState.close() }
                    },
                    onTagsClick = {
                        scope.launch { drawerState.close() }
                        onOpenTags()
                    },
                    onAddListClick = { listViewModel.openNewList() },
                    onEditListClick = { list -> listViewModel.openEditList(list) }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                TaskActionFab(
                    onTaskClick = { viewModel.openNewTask() },
                    onHabitClick = viewModel::openNewHabit,
                    onNoteClick = viewModel::openNewNote,
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
                        val titleIcon: ImageVector?
                        val titleColor: Color
                        val titleText: String
                        when {
                            state.selectedList != null -> {
                                titleIcon = materialIcon(state.selectedList.icon)
                                titleColor = state.selectedList.color.toColor()
                                titleText = state.selectedList.title
                            }
                            state.selectedTag != null -> {
                                titleIcon = null
                                titleColor = state.selectedTag.color.toColor()
                                titleText = state.selectedTag.name
                            }
                            else -> {
                                titleIcon = materialIcon("AllInclusive")
                                titleColor = MaterialTheme.colorScheme.primary
                                titleText = "All tasks"
                            }
                        }
                        TaskTitleContent(
                            title = titleText,
                            icon = titleIcon,
                            color = titleColor
                        )
                    },
                    actions = {
                        ViewOptionsMenu(
                            showCompleted = state.showCompleted,
                            onShowCompletedChange = viewModel::setShowCompleted,
                            searchText = state.searchText,
                            onSearchTextChange = viewModel::updateSearchText,
                            filters = state.board.filters,
                            selectedFilterId = state.selectedFilterId,
                            selectFilter = viewModel::selectFilter,
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
            val contentModifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())
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
                    modifier = contentModifier
                )
            }
        }
    }

    listState.editor?.let { listEditor ->
        ListEditorSheet(
            editor = listEditor,
            onDismiss = listViewModel::dismissEditor,
            onSave = { listViewModel.saveEditor(onSaved = viewModel::selectList) },
            onDelete = { listViewModel.deleteEditorList() },
            onTitleChange = listViewModel::updateTitle,
            onColorChange = listViewModel::updateColor,
            onIconChange = listViewModel::updateIcon
        )
    }
}
