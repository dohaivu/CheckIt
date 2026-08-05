package com.checkit.widget

import androidx.glance.action.ActionParameters

internal const val ExtraDailyPlanItemId: String = "com.checkit.extra.DAILY_PLAN_ITEM_ID"
internal const val ExtraTaskId: String = "com.checkit.extra.TASK_ID"
internal const val ExtraNoteId: String = "com.checkit.extra.NOTE_ID"
internal const val ExtraOpenMyDaySuggestions: String = "com.checkit.extra.OPEN_MY_DAY_SUGGESTIONS"
internal const val ExtraOpenDayReview: String = "com.checkit.extra.OPEN_DAY_REVIEW"
internal const val ExtraOpenPlanAssist: String = "com.checkit.extra.OPEN_PLAN_ASSIST"
internal const val ExtraOpenCheckIn: String = "com.checkit.extra.OPEN_CHECK_IN"
internal const val ExtraOpenQuickSprint: String = "com.checkit.extra.OPEN_QUICK_SPRINT"
internal const val ExtraOpenNewJournalEntry: String = "com.checkit.extra.OPEN_NEW_JOURNAL_ENTRY"
internal const val ExtraStartSprintForItemId: String = "com.checkit.extra.START_SPRINT_FOR_ITEM_ID"

internal val DailyPlanItemIdParameterKey = ActionParameters.Key<Long>(ExtraDailyPlanItemId)
internal val TaskIdParameterKey = ActionParameters.Key<Long>(ExtraTaskId)
internal val NoteIdParameterKey = ActionParameters.Key<Long>(ExtraNoteId)
internal val OpenMyDaySuggestionsParameterKey = ActionParameters.Key<Boolean>(ExtraOpenMyDaySuggestions)
internal val OpenDayReviewParameterKey = ActionParameters.Key<Boolean>(ExtraOpenDayReview)
internal val OpenPlanAssistParameterKey = ActionParameters.Key<Boolean>(ExtraOpenPlanAssist)
internal val OpenCheckInParameterKey = ActionParameters.Key<Boolean>(ExtraOpenCheckIn)
internal val OpenNewJournalEntryParameterKey = ActionParameters.Key<Boolean>(ExtraOpenNewJournalEntry)
internal val OpenQuickSprintParameterKey = ActionParameters.Key<Boolean>(ExtraOpenQuickSprint)
