/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.uicore.theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import dev.bunko.i18n.MR
import dev.icerock.moko.resources.StringResource

data class Theme(
    val id: Int,
    val titleRes: StringResource,
    val colors: Colors,
    val extraColors: ExtraColors,
)

fun bunkoLightColors(
    primary: Color = Color(0xFF0057CE),
    primaryVariant: Color = Color(0xFF001947),
    secondary: Color = Color(0xFF0057CE),
    secondaryVariant: Color = Color(0xFF018786),
    background: Color = Color(0xFFFDFBFF),
    surface: Color = Color(0xFFFDFBFF),
    error: Color = Color(0xFFB00020),
    onPrimary: Color = Color.White,
    onSecondary: Color = Color.White,
    onBackground: Color = Color(0xFF1B1B1E),
    onSurface: Color = Color(0xFF1B1B1E),
    onError: Color = Color.White,
) = lightColors(
    primary = primary,
    primaryVariant = primaryVariant,
    secondary = secondary,
    secondaryVariant = secondaryVariant,
    background = background,
    surface = surface,
    error = error,
    onPrimary = onPrimary,
    onSecondary = onSecondary,
    onBackground = onBackground,
    onSurface = onSurface,
    onError = onError,
)

fun bunkoDarkColors(
    primary: Color = Color(0xFFAEC6FF),
    primaryVariant: Color = Color(0xFF00419E),
    secondary: Color = Color(0xFFAEC6FF),
    secondaryVariant: Color = Color(0xFF00419E),
    background: Color = Color(0xFF1B1B1E),
    surface: Color = Color(0xFF1B1B1E),
    error: Color = Color(0xFFCF6679),
    onPrimary: Color = Color(0xFF002C71),
    onSecondary: Color = Color(0xFF002C71),
    onBackground: Color = Color(0xFFE4E2E6),
    onSurface: Color = Color(0xFFE4E2E6),
    onError: Color = Color.White,
) = darkColors(
    primary = primary,
    primaryVariant = primaryVariant,
    secondary = secondary,
    secondaryVariant = secondaryVariant,
    background = background,
    surface = surface,
    error = error,
    onPrimary = onPrimary,
    onSecondary = onSecondary,
    onBackground = onBackground,
    onSurface = onSurface,
    onError = onError,
)

fun extraColors(
    tertiary: Color = Color(0xFF006E17),
    onTertiary: Color = Color.White,
) = ExtraColors(
    tertiary = tertiary,
    onTertiary = onTertiary,
)


const val DYNAMIC_THEME_ID = 100

val themes = listOf(
    Theme(
        1,
        MR.strings.theme_default,
        bunkoLightColors(),
        extraColors(),
    ),
    Theme(
        3,
        MR.strings.theme_default,
        bunkoDarkColors(),
        extraColors(
            tertiary = Color(0xFF7ADC77),
            onTertiary = Color(0xFF003907),
        ),
    ),
    Theme(
        31,
        MR.strings.theme_washi,
        bunkoLightColors(
            primary = Color(0xFF6B4E3D),
            primaryVariant = Color(0xFF6B4E3D),
            secondary = Color(0xFF8A6A4F),
            secondaryVariant = Color(0xFF8A6A4F),
            background = Color(0xFFFAF5EA),
            surface = Color(0xFFFAF5EA),
            onBackground = Color(0xFF2B2520),
            onSurface = Color(0xFF2B2520),
        ),
        extraColors(
            tertiary = Color(0xFFC0392B),
            onTertiary = Color(0xFFFFFFFF),
        ),
    ),
    Theme(
        32,
        MR.strings.theme_washi,
        bunkoDarkColors(
            primary = Color(0xFFE3C3A8),
            primaryVariant = Color(0xFFE3C3A8),
            secondary = Color(0xFFD9BFA3),
            secondaryVariant = Color(0xFFD9BFA3),
            background = Color(0xFF1E1A16),
            surface = Color(0xFF1E1A16),
            onPrimary = Color(0xFF3D2A1E),
            onSecondary = Color(0xFF3D2A1E),
            onBackground = Color(0xFFEDE3D7),
            onSurface = Color(0xFFEDE3D7),
        ),
        extraColors(
            tertiary = Color(0xFFFF8A73),
            onTertiary = Color(0xFF4A0E05),
        ),
    ),
    Theme(
        33,
        MR.strings.theme_matcha,
        bunkoLightColors(
            primary = Color(0xFF4A6B2A),
            primaryVariant = Color(0xFF4A6B2A),
            secondary = Color(0xFF5D7A45),
            secondaryVariant = Color(0xFF5D7A45),
            background = Color(0xFFF6F8EE),
            surface = Color(0xFFF6F8EE),
            onBackground = Color(0xFF1D2118),
            onSurface = Color(0xFF1D2118),
        ),
        extraColors(
            tertiary = Color(0xFF8E4A5C),
            onTertiary = Color(0xFFFFFFFF),
        ),
    ),
    Theme(
        34,
        MR.strings.theme_matcha,
        bunkoDarkColors(
            primary = Color(0xFFB4D68C),
            primaryVariant = Color(0xFFB4D68C),
            secondary = Color(0xFFA9C48F),
            secondaryVariant = Color(0xFFA9C48F),
            background = Color(0xFF161A12),
            surface = Color(0xFF161A12),
            onPrimary = Color(0xFF1F3508),
            onSecondary = Color(0xFF1F3508),
            onBackground = Color(0xFFE2E6D8),
            onSurface = Color(0xFFE2E6D8),
        ),
        extraColors(
            tertiary = Color(0xFFF0B3C2),
            onTertiary = Color(0xFF4D1A28),
        ),
    ),
    Theme(
        35,
        MR.strings.theme_yozakura,
        bunkoLightColors(
            primary = Color(0xFFA23B6E),
            primaryVariant = Color(0xFFA23B6E),
            secondary = Color(0xFF8C5A74),
            secondaryVariant = Color(0xFF8C5A74),
            background = Color(0xFFFFF7FA),
            surface = Color(0xFFFFF7FA),
            onBackground = Color(0xFF2A1C23),
            onSurface = Color(0xFF2A1C23),
        ),
        extraColors(
            tertiary = Color(0xFF9A5B00),
            onTertiary = Color(0xFFFFFFFF),
        ),
    ),
    Theme(
        36,
        MR.strings.theme_yozakura,
        bunkoDarkColors(
            primary = Color(0xFFFFB1D3),
            primaryVariant = Color(0xFFFFB1D3),
            secondary = Color(0xFFE6BFD2),
            secondaryVariant = Color(0xFFE6BFD2),
            background = Color(0xFF1C1420),
            surface = Color(0xFF1C1420),
            onPrimary = Color(0xFF5C0F38),
            onSecondary = Color(0xFF5C0F38),
            onBackground = Color(0xFFF0E1EA),
            onSurface = Color(0xFFF0E1EA),
        ),
        extraColors(
            tertiary = Color(0xFFFFC86B),
            onTertiary = Color(0xFF3F2A00),
        ),
    ),
    Theme(
        37,
        MR.strings.theme_aizome,
        bunkoLightColors(
            primary = Color(0xFF2F428F),
            primaryVariant = Color(0xFF2F428F),
            secondary = Color(0xFF4A5A99),
            secondaryVariant = Color(0xFF4A5A99),
            background = Color(0xFFF6F7FC),
            surface = Color(0xFFF6F7FC),
            onBackground = Color(0xFF191B24),
            onSurface = Color(0xFF191B24),
        ),
        extraColors(
            tertiary = Color(0xFF8A5F0A),
            onTertiary = Color(0xFFFFFFFF),
        ),
    ),
    Theme(
        38,
        MR.strings.theme_aizome,
        bunkoDarkColors(
            primary = Color(0xFFB3C3FF),
            primaryVariant = Color(0xFFB3C3FF),
            secondary = Color(0xFFBEC6EA),
            secondaryVariant = Color(0xFFBEC6EA),
            background = Color(0xFF0F1322),
            surface = Color(0xFF0F1322),
            onPrimary = Color(0xFF102169),
            onSecondary = Color(0xFF102169),
            onBackground = Color(0xFFE1E3F2),
            onSurface = Color(0xFFE1E3F2),
        ),
        extraColors(
            tertiary = Color(0xFFF2C14E),
            onTertiary = Color(0xFF3D2E00),
        ),
    ),
    Theme(
        39,
        MR.strings.theme_kintsugi,
        bunkoLightColors(
            primary = Color(0xFF7A5A0E),
            primaryVariant = Color(0xFF7A5A0E),
            secondary = Color(0xFF6E6455),
            secondaryVariant = Color(0xFF6E6455),
            background = Color(0xFFFAF8F3),
            surface = Color(0xFFFAF8F3),
            onBackground = Color(0xFF1F1C17),
            onSurface = Color(0xFF1F1C17),
        ),
        extraColors(
            tertiary = Color(0xFF2E7D6B),
            onTertiary = Color(0xFFFFFFFF),
        ),
    ),
    Theme(
        40,
        MR.strings.theme_kintsugi,
        bunkoDarkColors(
            primary = Color(0xFFE9C46A),
            primaryVariant = Color(0xFFE9C46A),
            secondary = Color(0xFFCFC3A8),
            secondaryVariant = Color(0xFFCFC3A8),
            background = Color(0xFF151413),
            surface = Color(0xFF151413),
            onPrimary = Color(0xFF3B2C00),
            onSecondary = Color(0xFF3B2C00),
            onBackground = Color(0xFFECE6DA),
            onSurface = Color(0xFFECE6DA),
        ),
        extraColors(
            tertiary = Color(0xFF7FD1BA),
            onTertiary = Color(0xFF00382D),
        ),
    ),
    Theme(
        41,
        MR.strings.theme_arcade,
        bunkoLightColors(
            primary = Color(0xFFA01386),
            primaryVariant = Color(0xFFA01386),
            secondary = Color(0xFF6C3BAA),
            secondaryVariant = Color(0xFF6C3BAA),
            background = Color(0xFFFCF7FF),
            surface = Color(0xFFFCF7FF),
            onBackground = Color(0xFF20152B),
            onSurface = Color(0xFF20152B),
        ),
        extraColors(
            tertiary = Color(0xFF00788F),
            onTertiary = Color(0xFFFFFFFF),
        ),
    ),
    Theme(
        42,
        MR.strings.theme_arcade,
        bunkoDarkColors(
            primary = Color(0xFFFF6FD8),
            primaryVariant = Color(0xFFFF6FD8),
            secondary = Color(0xFFB79CFF),
            secondaryVariant = Color(0xFFB79CFF),
            background = Color(0xFF120E1F),
            surface = Color(0xFF120E1F),
            onPrimary = Color(0xFF4A0040),
            onSecondary = Color(0xFF4A0040),
            onBackground = Color(0xFFEDE4FF),
            onSurface = Color(0xFFEDE4FF),
        ),
        extraColors(
            tertiary = Color(0xFF4DE3FF),
            onTertiary = Color(0xFF00363F),
        ),
    ),
    Theme(
        43,
        MR.strings.theme_newsprint,
        bunkoLightColors(
            primary = Color(0xFF1C1C1C),
            primaryVariant = Color(0xFF1C1C1C),
            secondary = Color(0xFF4A4A4A),
            secondaryVariant = Color(0xFF4A4A4A),
            background = Color(0xFFF7F6F2),
            surface = Color(0xFFF7F6F2),
            onBackground = Color(0xFF1C1C1C),
            onSurface = Color(0xFF1C1C1C),
        ),
        extraColors(
            tertiary = Color(0xFFC8102E),
            onTertiary = Color(0xFFFFFFFF),
        ),
    ),
    Theme(
        44,
        MR.strings.theme_newsprint,
        bunkoDarkColors(
            primary = Color(0xFFEDEDED),
            primaryVariant = Color(0xFFEDEDED),
            secondary = Color(0xFFBDBDBD),
            secondaryVariant = Color(0xFFBDBDBD),
            background = Color(0xFF161616),
            surface = Color(0xFF161616),
            onPrimary = Color(0xFF1C1C1C),
            onSecondary = Color(0xFF1C1C1C),
            onBackground = Color(0xFFE8E8E8),
            onSurface = Color(0xFFE8E8E8),
        ),
        extraColors(
            tertiary = Color(0xFFFF6B6B),
            onTertiary = Color(0xFF3B0008),
        ),
    ),
    Theme(
        45,
        MR.strings.theme_coral_reef,
        bunkoLightColors(
            primary = Color(0xFFB8432F),
            primaryVariant = Color(0xFFB8432F),
            secondary = Color(0xFFA35A4A),
            secondaryVariant = Color(0xFFA35A4A),
            background = Color(0xFFFFF8F6),
            surface = Color(0xFFFFF8F6),
            onBackground = Color(0xFF251A18),
            onSurface = Color(0xFF251A18),
        ),
        extraColors(
            tertiary = Color(0xFF006A71),
            onTertiary = Color(0xFFFFFFFF),
        ),
    ),
    Theme(
        46,
        MR.strings.theme_coral_reef,
        bunkoDarkColors(
            primary = Color(0xFFFF9E87),
            primaryVariant = Color(0xFFFF9E87),
            secondary = Color(0xFFF0B9AC),
            secondaryVariant = Color(0xFFF0B9AC),
            background = Color(0xFF0D1B24),
            surface = Color(0xFF0D1B24),
            onPrimary = Color(0xFF5C1405),
            onSecondary = Color(0xFF5C1405),
            onBackground = Color(0xFFE3EDF2),
            onSurface = Color(0xFFE3EDF2),
        ),
        extraColors(
            tertiary = Color(0xFF6ADBE3),
            onTertiary = Color(0xFF003539),
        ),
    ),
)
