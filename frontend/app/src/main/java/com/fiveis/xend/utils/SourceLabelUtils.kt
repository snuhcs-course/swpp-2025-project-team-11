package com.fiveis.xend.utils

import java.util.Locale

private const val SOURCE_LABEL_DELIMITER = ","

/**
 * Merge existing source labels with new mailbox labels (e.g., INBOX, SENT).
 */
fun mergeSourceLabels(existing: String?, vararg labels: String): String {
    val normalized = existing
        ?.split(SOURCE_LABEL_DELIMITER)
        ?.map { it.trim().uppercase(Locale.ROOT) }
        ?.filter { it.isNotBlank() }
        ?.toMutableSet() ?: mutableSetOf()

    labels.map { it.trim().uppercase(Locale.ROOT) }
        .filter { it.isNotBlank() }
        .forEach { normalized += it }

    if (normalized.isEmpty()) {
        return ""
    }

    return normalized.sorted().joinToString(SOURCE_LABEL_DELIMITER)
}

/**
 * Extract mailbox labels (INBOX/SENT) from raw Gmail label ids.
 */
fun mailboxLabelsFromIds(labelIds: List<String>): List<String> {
    val normalized = labelIds.map { it.uppercase(Locale.ROOT) }
    val result = mutableListOf<String>()
    if (normalized.any { it.contains("INBOX") }) {
        result += "INBOX"
    }
    if (normalized.any { it.contains("SENT") }) {
        result += "SENT"
    }
    return result
}
