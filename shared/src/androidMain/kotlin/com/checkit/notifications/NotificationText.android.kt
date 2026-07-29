package com.checkit.notifications

import com.checkit.domain.ActionReminderQuotes

internal object NotificationText {
    fun withActionQuote(body: String): String =
        "$body\n\n${ActionReminderQuotes.random().line}"
}
