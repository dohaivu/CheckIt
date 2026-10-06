package com.checkit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.FocusPeriod
import com.checkit.domain.MetricItem
import com.checkit.domain.MetricUnit
import com.checkit.domain.NoteItem
import com.checkit.domain.Period
import com.checkit.domain.PeriodGoal
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.ui.tasks.TaskWorkspaceView
import com.checkit.ui.tasks.views.currentTimeMinutes
import com.checkit.ui.theme.AppIconColorDefaults
import com.checkit.ui.theme.AppIconColorDefaults.FallbackColor
import com.checkit.ui.theme.toColor
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toLocalIsoWeekDate
import kotlin.time.Instant

fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    this.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick,
        onDoubleClick = onDoubleClick
    )
}

internal fun TaskWorkspaceView.icon(): ImageVector = when (this) {
    TaskWorkspaceView.List -> Icons.AutoMirrored.Filled.ViewList
    TaskWorkspaceView.Agenda -> Icons.Default.ViewAgenda
    TaskWorkspaceView.Timeline -> Icons.Default.Schedule
    TaskWorkspaceView.Habits -> Icons.Default.Repeat
}

@Composable
internal fun NoteIcon(status: TaskStatus) {
    Icon(
        imageVector = if (status == TaskStatus.Completed) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.Notes,
        contentDescription = null,
        tint = if (status == TaskStatus.Completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
internal fun TaskIcon(completed: Boolean, color: Color) {
    Icon(
        imageVector = if (completed) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
        contentDescription = null,
        tint = if (completed) MaterialTheme.colorScheme.primary else color,
        modifier = Modifier.size(20.dp)
    )
}


@Composable
internal fun HabitIcon(completed: Boolean, color: Color) {
    if (completed) {
        BadgedActionIcon(baseIcon = Icons.Rounded.Repeat, isDone = true)
    } else {
        Icon(
            imageVector = Icons.Rounded.Repeat,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
internal fun DailyPlanIcon(source: DailyPlanItemSource, isDone: Boolean, isHabit: Boolean) {
    when (source) {
        DailyPlanItemSource.MyDayNote -> {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.EventNote,
                contentDescription = null,
                tint = FallbackColor,
                modifier = Modifier.size(20.dp)
            )
        }
        DailyPlanItemSource.MyDayReminder -> {
            BadgedActionIcon(baseIcon = Icons.Default.Schedule, isDone = isDone)
        }
        else -> {
            if (isHabit) {
                BadgedActionIcon(baseIcon = Icons.Default.Repeat, isDone = isDone)
            } else {
                Icon(
                    imageVector = if (isDone) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (isDone) MaterialTheme.colorScheme.primary else FallbackColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun BadgedActionIcon(
    baseIcon: ImageVector,
    isDone: Boolean,
    modifier: Modifier = Modifier,
    baseIconSize: Dp = 20.dp,
    badgeSize: Dp = 10.dp,
    baseIconTint: Color = FallbackColor,
    doneColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier.size(baseIconSize),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = baseIcon,
            contentDescription = null,
            tint = if (isDone) doneColor else baseIconTint,
            modifier = Modifier.size(baseIconSize)
        )

        Box(
            modifier = Modifier
                .size(badgeSize)
                .align(Alignment.BottomEnd)
                .offset(x = 1.dp, y = 0.dp)
                .clip(CircleShape)
                .background(if (isDone) doneColor else baseIconTint)
            ,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (isDone) "Completed" else "Not Completed",
                tint = Color.White,
                modifier = Modifier.size(badgeSize * 0.7f)
            )
        }
    }
}

internal fun LocalDate.compact(): String {
    val today = today()
    val monthDay = "${shortMonthName()} $day"
    return when (this) {
        today -> "Today"
        today.plus(1, DateTimeUnit.DAY) -> "Tomorrow"
        today.plus(-1, DateTimeUnit.DAY) -> "Yesterday"
        today.plus(2, DateTimeUnit.DAY),
        today.plus(3, DateTimeUnit.DAY),
        today.plus(4, DateTimeUnit.DAY),
        today.plus(5, DateTimeUnit.DAY),
        today.plus(6, DateTimeUnit.DAY) -> dayOfWeek.shortName()
        else -> {
            if (year == today.year) monthDay else "$monthDay, $year"
        }
    }
}

fun TaskPriority.priorityColor(): Color = when (this) {
    TaskPriority.High -> Color(0xFFDC2626)
    TaskPriority.Medium -> Color(0xFFCA8A04)
    TaskPriority.Low -> Color(0xFF2563EB)
    TaskPriority.None -> Color(0xFF64748B)
}

fun NoteItem?.cardColor(): Color {
    return this?.tags?.firstOrNull()?.color?.toColor() ?: FallbackColor
}

fun TaskItem?.cardColor(): Color {
    return this?.tags?.firstOrNull()?.color?.toColor() ?: FallbackColor
}

fun DailyPlanItem.cardColor(): Color {
    return this.tags.firstOrNull()?.color?.toColor() ?: AppIconColorDefaults.DailyPlanCardColor
}

fun TaskItem.timeRangeLabel(): String {
    val start = startTimeMinutes?.toClockLabel() ?: "Any time"
    val end = endTimeMinutes?.toClockLabel()
    return if (end == null) start else "$start - $end"
}

fun TaskItem.isOverdue(): Boolean {
    return doDate.isOverdue(today(), endTimeMinutes ?: startTimeMinutes, status == TaskStatus.Completed)
}
fun NoteItem.isOverdue(): Boolean {
    return date.isOverdue(today(), null,status == TaskStatus.Completed)
}
fun DailyPlanItem.isOverdue(date: LocalDate): Boolean {
    return date.isOverdue(today(), endTimeMinutes ?: startTimeMinutes, status == DailyPlanItemStatus.Done)
}
fun LocalDate?.isOverdue(today: LocalDate, deadline: Int?, isCompleted: Boolean): Boolean =
    when {
        isCompleted || this == null -> false
        this < today -> true
        this == today -> deadline != null && currentTimeMinutes() > deadline
        else -> false
    }

fun Int.toDurationLabel(compact: Boolean = false): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h${if(compact) "" else " "}${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

fun Int.toClockLabel(): String {
    val hour = this / 60
    val minute = this % 60
    val suffix = if (hour >= 12) "PM" else "AM"
    val displayHour = when (val normalized = hour % 12) {
        0 -> 12
        else -> normalized
    }
    return "$displayHour:${minute.toString().padStart(2, '0')} $suffix"
}

/** Epoch millis rendered as a local-time clock label ("2:32 PM"). */
fun Long.toClockLabel(): String {
    val local = Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return (local.hour * 60 + local.minute).toClockLabel()
}

/** Epoch millis rendered as a local date-time label ("Oct 6, 2026, 2:32 PM"). */
fun Long.toDateTimeLabel(): String {
    val local = Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.date.shortMonthName()} ${local.date.day}, ${local.date.year}, ${toClockLabel()}"
}

enum class TimelineItemType { Task, Note, DailyPlan, Journal }

data class TimelineItem(
    val id: String,
    val type: TimelineItemType,
    val date: LocalDate? = null,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val sortOrder: Int = 0,
    val isResizable: Boolean = false,
    val tag: Any? = null
)

fun Modifier.dashedBorder(
    width: Dp = 2.dp,
    color: Color = Color.Black,
    dashLength: Dp = 10.dp,
    gapLength: Dp = 10.dp,
    cornerRadius: Dp = 0.dp
): Modifier = this.drawBehind {
    // Convert Dp dimensions to target device pixel size
    val strokeWidthPx = width.toPx()
    val dashLengthPx = dashLength.toPx()
    val gapLengthPx = gapLength.toPx()
    val cornerRadiusPx = cornerRadius.toPx()

    val stroke = Stroke(
        width = strokeWidthPx,
        pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(dashLengthPx, gapLengthPx),
            phase = 0f
        )
    )

    // Adjust boundaries to ensure the line width is fully inside the component
    val halfWidth = strokeWidthPx / 2
    val sizeWithStroke = this.size.copy(
        width = this.size.width - strokeWidthPx,
        height = this.size.height - strokeWidthPx
    )

    // Draw the actual border shape
    drawRoundRect(
        color = color,
        topLeft = Offset(halfWidth, halfWidth),
        size = sizeWithStroke,
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
        style = stroke
    )
}

fun MetricUnit.displayName(customUnit: String? = null): String = when (this) {
    MetricUnit.None -> "None"
    MetricUnit.Custom -> customUnit?.takeIf { it.isNotBlank() } ?: "Custom"
    MetricUnit.Percentage -> "%"
    MetricUnit.Points -> "points"
    MetricUnit.Items -> "items"
    MetricUnit.Hours -> "hours"
    MetricUnit.Days -> "days"
    MetricUnit.VND -> "vnđ"
    MetricUnit.Lan -> "lần"
    MetricUnit.Km -> "km"
    MetricUnit.Rating -> "rating"
    MetricUnit.Countdown -> "Countdown"
    MetricUnit.DueDate -> "Due date"
}

fun MetricItem.displayUnit(): String? = when (unit) {
    MetricUnit.None -> null
    MetricUnit.Custom -> customUnit?.takeIf { it.isNotBlank() }
    MetricUnit.Percentage -> "%"
    MetricUnit.Points -> "points"
    MetricUnit.Items -> "items"
    MetricUnit.Hours -> "hours"
    MetricUnit.Days -> "days"
    MetricUnit.Rating -> "rating"
    MetricUnit.VND -> "đ"
    MetricUnit.Lan -> "lần"
    MetricUnit.Km -> "km"
    MetricUnit.Countdown -> null
    MetricUnit.DueDate -> null
}

fun MetricItem.isDateBased(): Boolean =
    unit == MetricUnit.Countdown || unit == MetricUnit.DueDate

fun MetricItem.dueLocalDate(): LocalDate? =
    dueDateEpochDays?.let { runCatching { LocalDate.fromEpochDays(it) }.getOrNull() }

/** Remaining days for Countdown: due - today. Null when not a Countdown or no due date. */
fun MetricItem.daysRemaining(today: LocalDate = today()): Int? {
    if (unit != MetricUnit.Countdown) return null
    val due = dueDateEpochDays ?: return null
    return (due - today.toEpochDays()).toInt()
}

fun MetricItem.countdownLabel(today: LocalDate = today()): String {
    val remaining = daysRemaining(today) ?: return "No date"
    return when {
        remaining > 1 -> "$remaining days left"
        remaining == 1 -> "1 day left"
        remaining == 0 -> "Due today"
        remaining == -1 -> "Overdue by 1 day"
        else -> "Overdue by ${-remaining} days"
    }
}

fun MetricItem.dueDateLabel(): String {
    val date = dueLocalDate() ?: return "No date"
    val base = date.compact()
    val time = dueTimeMinutes?.takeIf { unit == MetricUnit.DueDate }?.toClockLabel()
    return if (time != null) "$base, $time" else base
}

/** Date-based display text: Countdown shows remaining days, DueDate shows the date. */
fun MetricItem.dateBasedDisplay(today: LocalDate = today()): String = when (unit) {
    MetricUnit.Countdown -> {
        val label = countdownLabel(today)
        val due = dueLocalDate()?.let { " · Due ${it.compact()}" }.orEmpty()
        "$label$due"
    }
    MetricUnit.DueDate -> dueDateLabel()
    else -> value
}

/** True when a date-based metric is past due (time-aware for DueDate) and not completed. */
fun MetricItem.isMetricOverdue(today: LocalDate = today()): Boolean {
    if (!isDateBased() || isCompleted) return false
    val due = dueLocalDate() ?: return false
    val deadline = if (unit == MetricUnit.DueDate) dueTimeMinutes else null
    return due.isOverdue(today, deadline, false)
}

/**
 * Save-time validity: date-based units carry no [MetricItem.value], so they
 * are kept when a due date is set; other units need a non-blank value.
 * This drops empty draft rows (Add without filling) without losing dates.
 */
fun MetricItem.isValidForSave(): Boolean =
    if (isDateBased()) dueDateEpochDays != null else value.isNotBlank()

/**
 * Zero-arg variants for Swift callers (Kotlin default arguments are not
 * visible to Swift, and building a kotlinx LocalDate there is awkward).
 */
fun MetricItem.dateBasedDisplayToday(): String = dateBasedDisplay()
fun MetricItem.countdownLabelToday(): String = countdownLabel()
fun MetricItem.isMetricOverdueToday(): Boolean = isMetricOverdue()


fun MetricItem.toAnnotatedString(valueColor: Color): androidx.compose.ui.text.AnnotatedString =
    buildAnnotatedString {
        if (name.isNotBlank()) {
            append(name)
            append(" ")
        }
        if (isDateBased()) {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
            ) {
                append(dateBasedDisplay())
            }
        } else {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
            ) {
                append(value)
            }
            if (!targetValue.isNullOrBlank()) {
                append("/")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(targetValue)
                }
            }
            val unit = displayUnit()
            if (unit != null) {
                append(" ")
                append(unit)
            }
        }
    }

fun MetricItem.toPlainString(): String {
    return buildString {
        if (name.isNotBlank()) {
            append(name)
            append(" ")
        }
        if (isDateBased()) {
            append(dateBasedDisplay())
        } else {
            append(value)

            if (!targetValue.isNullOrBlank()) {
                append("/${targetValue}")
            }
            val unit = displayUnit()
            if (unit != null) {
                append(" ")
                append(unit)
            }
        }
    }
}

/**
 * Returns the value/target ratio in 0..1, or null when either side is not a
 * valid number or the target is not positive. Date-based units never report progress.
 */
fun MetricItem.progressRatio(): Float? {
    if (isDateBased()) return null
    val value = parseMetricDouble(value) ?: return null
    val target = targetValue?.let(::parseMetricDouble) ?: return null
    if (target <= 0) return null
    return (value / target).toFloat().coerceIn(0f, 1f)
}

/** Units whose value is a number (free-text units like None/Custom excluded). */
fun MetricUnit.isNumeric(): Boolean = when (this) {
    MetricUnit.Percentage,
    MetricUnit.Points,
    MetricUnit.Items,
    MetricUnit.Hours,
    MetricUnit.Days,
    MetricUnit.Rating,
    MetricUnit.VND,
    MetricUnit.Lan,
    MetricUnit.Km -> true
    MetricUnit.None,
    MetricUnit.Countdown,
    MetricUnit.DueDate,
    MetricUnit.Custom -> false
}

/** Countable whole-number units that suit a stepper. */
fun MetricUnit.isIntegerUnit(): Boolean = when (this) {
    MetricUnit.Points,
    MetricUnit.Items,
    MetricUnit.Lan -> true
    else -> false
}

/** Lenient number parse: trims, accepts comma decimals. Null when not a number. */
fun parseMetricDouble(raw: String): Double? =
    raw.trim().replace(',', '.').toDoubleOrNull()

/**
 * Input-time sanitizer per unit. Integers keep digits only; decimals keep
 * digits plus one dot and a leading minus; other units pass through untouched.
 * Never clamps (so typing "100" isn't fought) — clamp on commit/display.
 */
fun sanitizeMetricInput(unit: MetricUnit, raw: String): String {
    if (!unit.isNumeric()) return raw
    val normalized = raw.replace(',', '.')
    if (unit.isIntegerUnit()) return normalized.filter { it.isDigit() }
    val builder = StringBuilder()
    var dotSeen = false
    normalized.forEach { c ->
        when {
            c.isDigit() -> builder.append(c)
            c == '.' && !dotSeen -> {
                dotSeen = true
                builder.append(c)
            }
            c == '-' && builder.isEmpty() -> builder.append(c)
        }
    }
    return builder.toString()
}

/** Compact display for slider-derived values: "45", "45.5" — never "45.0". */
fun formatMetricDouble(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return ""
    val rounded = kotlin.math.round(value * 10) / 10.0
    return if (rounded == kotlin.math.floor(rounded)) rounded.toLong().toString()
    else rounded.toString().trimEnd('0').trimEnd('.')
}

@Composable
fun Period.color() = when (this) {
    Period.Day -> MaterialTheme.colorScheme.secondary
    Period.Week -> MaterialTheme.colorScheme.tertiary
    Period.Month -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun Period.gradient(): Brush {
    val color = color()
    val surfaceBase = MaterialTheme.colorScheme.surfaceContainerLow
    val periodContainer = when (this) {
        Period.Day -> MaterialTheme.colorScheme.secondaryContainer
        Period.Week -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    return remember(color, periodContainer, surfaceBase) {
        Brush.linearGradient(
            colors = listOf(
                periodContainer.copy(alpha = 0.58f),
                color.copy(alpha = 0.14f),
                surfaceBase
            ),
            start = Offset.Zero,
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }
}

@Composable
fun Period.periodDetail(date: LocalDate): String = when (this) {
    Period.Day -> "${date.dayOfWeek.localizedShortName().uppercase()} · ${date.month.localizedShortName().uppercase()} ${date.day}"
    Period.Week -> "W${date.toLocalIsoWeekDate().isoWeekNumber}"
    Period.Month -> date.month.localizedName().uppercase()
    Period.Quarter -> {
        val quarter = ((date.month.number - 1) / 3) + 1
        "Q$quarter"
    }
    Period.Year -> "${date.year}"
}
@Composable
fun PeriodGoal.periodDetail(): String = this.period.periodDetail(this.startDate)

@Composable
fun FocusPeriod.periodDetail(): String = this.period.periodDetail(this.anchorDate)