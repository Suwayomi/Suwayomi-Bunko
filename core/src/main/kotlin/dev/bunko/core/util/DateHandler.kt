package dev.bunko.core.util

import androidx.compose.ui.text.intl.Locale
import dev.bunko.core.lang.toPlatform
import me.tatarka.inject.annotations.Inject
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class DateHandler @Inject constructor() {
    val formatOptions by lazy {
        listOf(
            "",
            "MM/dd/yy",
            "dd/MM/yy",
            "yyyy-MM-dd",
        )
    }

    fun getDateFormat(format: String): (Instant) -> String =
        when (format) {
            "" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                .withLocale(Locale.current.toPlatform())
                .withZone(ZoneId.systemDefault())

            else -> DateTimeFormatter.ofPattern(format)
                .withZone(ZoneId.systemDefault())
        }.let { formatter ->
            {
                formatter.format(it.toJavaInstant())
            }
        }

    val dateTimeFormat: (Instant) -> String by lazy {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.current.toPlatform())
            .withZone(ZoneId.systemDefault())
            .let { formatter ->
                {
                    formatter.format(it.toJavaInstant())
                }
            }
    }
}
