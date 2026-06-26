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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.checkit.ui.TaskUiState
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.okr.GoalEditorSheet
import com.checkit.ui.okr.GoalViewModel
import com.checkit.ui.okr.ObjectiveEditorSheet
import com.checkit.ui.okr.ObjectiveScreen
import com.checkit.ui.okr.ObjectiveViewModel
import com.checkit.ui.tasks.list.ListEditorSheet
import com.checkit.ui.tasks.list.ListViewModel
import com.checkit.ui.tasks.tag.TagEditorSheet
import com.checkit.ui.tasks.tag.TagViewModel
import com.checkit.ui.tasks.views.ViewOptionsMenu
import kotlinx.coroutines.launch

@Composable
internal fun TaskScreen(
    state: TaskUiState,
    viewModel: TaskViewModel,
    goalViewModel: GoalViewModel,
    objectiveViewModel: ObjectiveViewModel,
    listViewModel: ListViewModel,
    tagViewModel: TagViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val goalState by goalViewModel.uiState.collectAsState()
    val listState by listViewModel.uiState.collectAsState()
    val tagState by tagViewModel.uiState.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                TaskSidebar(
                    goals = state.board.goals,
                    lists = state.board.objectives.filter { it.goalId == null },
                    tags = state.board.tags,
                    isBoardSelected = state.selectedGoalId == null &&
                        state.selectedListId == null &&
                        state.selectedFilterId == null &&
                        state.selectedTagId == null,
                    selectedListId = state.selectedListId,
                    selectedGoalId = state.selectedGoalId,
                    selectedTagId = state.selectedTagId,
                    onBoardClick = {
                        viewModel.selectBoard()
                        scope.launch { drawerState.close() }
                    },
                    onListClick = { listId ->
                        viewModel.selectList(listId)
                        scope.launch { drawerState.close() }
                    },
                    onTagClick = { tagId ->
                        viewModel.selectTag(tagId)
                        scope.launch { drawerState.close() }
                    },
                    onGoalClick = { goalId ->
                        viewModel.selectGoal(goalId)
                        scope.launch { drawerState.close() }
                    },
                    onAddGoalClick = { goalViewModel.openNewGoal() },
                    onEditGoalClick = { goal -> goalViewModel.openEditGoal(goal) },
                    onAddListClick = { listViewModel.openNewList() },
                    onEditListClick = { list -> listViewModel.openEditList(list) },
                    onAddTagClick = { tagViewModel.openNewTag() },
                    onEditTagClick = { tag -> tagViewModel.openEditTag(tag) }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                if (state.selectedGoalId != null) {
                    TaskActionFab(
                        onObjectiveClick = state.selectedGoalId?.let { goalId ->
                            { listViewModel.openNewObjective(goalId) }
                        }
                    )
                } else {
                    TaskActionFab(
                        onTaskClick = { viewModel.openNewTask() },
                        onNoteClick = viewModel::openNewNote,
                    )
                }
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
            } else if (state.selectedGoal != null) {
                ObjectiveScreen(
                    goal = state.selectedGoal,
                    board = state.board,
                    viewModel = objectiveViewModel,
                    onTaskClick = viewModel::openTask,
                    onAddTask = viewModel::openNewTaskOnKeyResult,
                    onEditObjective = listViewModel::openEditList,
                    modifier = contentModifier
                )
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

    goalState.editor?.let { goalEditor ->
        GoalEditorSheet(
            editor = goalEditor,
            onDismiss = goalViewModel::dismissEditor,
            onSave = { goalViewModel.saveEditor() },
            onDelete = { goalViewModel.deleteEditorGoal() },
            onTitleChange = goalViewModel::updateTitle,
            onColorChange = goalViewModel::updateColor,
            onIconChange = goalViewModel::updateIcon
        )
    }

    listState.editor?.let { listEditor ->
        if (listEditor.goalId != null) {
            ObjectiveEditorSheet(
                editor = listEditor,
                onDismiss = listViewModel::dismissEditor,
                onSave = { listViewModel.saveEditor(onSaved = viewModel::selectList) },
                onDelete = { listViewModel.deleteEditorList() },
                onTitleChange = listViewModel::updateName,
                onDateRangeChange = listViewModel::updateDateRange,
                onColorChange = listViewModel::updateColor,
                onIconChange = listViewModel::updateIcon
            )
        } else {
            ListEditorSheet(
                editor = listEditor,
                onDismiss = listViewModel::dismissEditor,
                onSave = { listViewModel.saveEditor(onSaved = viewModel::selectList) },
                onDelete = { listViewModel.deleteEditorList() },
                onNameChange = listViewModel::updateName,
                onColorChange = listViewModel::updateColor,
                onIconChange = listViewModel::updateIcon
            )
        }
    }

    tagState.editor?.let { tagEditor ->
        TagEditorSheet(
            editor = tagEditor,
            onDismiss = tagViewModel::dismissEditor,
            onSave = { tagViewModel.saveEditor(onSaved = viewModel::selectTag) },
            onDelete = { tagViewModel.deleteEditorTag() },
            onNameChange = tagViewModel::updateName,
            onColorChange = tagViewModel::updateColor
        )
    }
}
