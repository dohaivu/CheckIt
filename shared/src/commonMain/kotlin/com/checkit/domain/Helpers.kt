package com.checkit.domain

import doist.x.normalize.Form
import doist.x.normalize.normalize

fun String.removeAccents(): String {
    // 1. Decompose characters (e.g., "á" becomes "a" + combining acute accent)
    val normalized = this.normalize(Form.NFD)

    // 2. Filter out the combining diacritical marks block (\u0300–\u036F)
    return normalized.filter { char ->
        char.code !in 0x0300..0x036F
    }.replace('đ', 'd')
        .replace('Đ', 'D')
}
