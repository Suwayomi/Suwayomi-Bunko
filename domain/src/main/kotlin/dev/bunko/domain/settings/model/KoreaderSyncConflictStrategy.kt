package dev.bunko.domain.settings.model

enum class KoreaderSyncConflictStrategy {
    PROMPT,
    KEEP_LOCAL,
    KEEP_REMOTE,
    DISABLED,
    UNKNOWN__,
}
