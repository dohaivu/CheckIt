package com.checkit.widget

import androidx.glance.action.ActionParameters

internal const val ExtraDailyPlanItemId: String = "com.checkit.extra.DAILY_PLAN_ITEM_ID"
internal const val ExtraTaskId: String = "com.checkit.extra.TASK_ID"
internal const val ExtraNoteId: String = "com.checkit.extra.NOTE_ID"

internal val DailyPlanItemIdParameterKey = ActionParameters.Key<Long>(ExtraDailyPlanItemId)
internal val TaskIdParameterKey = ActionParameters.Key<Long>(ExtraTaskId)
internal val NoteIdParameterKey = ActionParameters.Key<Long>(ExtraNoteId)
