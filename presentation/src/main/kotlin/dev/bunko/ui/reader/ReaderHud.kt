/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bunko.domain.manga.model.MangaMeta
import dev.bunko.domain.reader.model.Direction
import dev.bunko.domain.reader.model.ImageScale
import dev.bunko.i18n.MR
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.ui.reader.model.ReaderChapter
import dev.bunko.ui.reader.model.ReaderItem
import dev.bunko.ui.reader.model.ReaderPage
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

private val BarSlide = tween<IntOffset>(200)
private val BarFade = tween<Float>(150)

private val QuickScales = listOf(ImageScale.FitScreen, ImageScale.FitWidth, ImageScale.FitHeight, ImageScale.OriginalSize)

internal const val MAX_DIMMER = 0.8F

internal val AUTO_SCROLL_SPEEDS = 20..300

@Composable
private fun hudColor() = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9F)

@Composable
fun ReaderTopBar(
    visible: Boolean,
    mangaTitle: String,
    chapterName: String,
    onBack: () -> Unit,
    onOpenInBrowser: (() -> Unit)?,
    incognito: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible,
        enter = slideInVertically(BarSlide) { -it } + fadeIn(BarFade),
        exit = slideOutVertically(BarSlide) { -it } + fadeOut(BarFade),
        modifier = modifier,
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Row(
                Modifier.fillMaxWidth().background(hudColor()).statusBarsPadding().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(MR.strings.action_back)) }
                Column(Modifier.weight(1F).padding(horizontal = 4.dp)) {
                    Text(mangaTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    Text(
                        chapterName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (incognito) {
                    Icon(
                        Icons.Outlined.VisibilityOff,
                        stringResource(MR.strings.incognito_mode),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
                if (onOpenInBrowser != null) {
                    IconButton(onOpenInBrowser) { Icon(Icons.Outlined.Public, stringResource(MR.strings.action_open_in_browser)) }
                }
            }
        }
    }
}

@Composable
fun ReaderBottomBar(
    visible: Boolean,
    previousChapter: ReaderChapter?,
    chapter: ReaderChapter,
    nextChapter: ReaderChapter?,
    direction: Direction,
    continuous: Boolean,
    pages: ImmutableList<ReaderItem>,
    currentPage: ReaderItem?,
    readerModes: ImmutableList<String>,
    seriesReaderMode: String,
    imageScale: ImageScale,
    navigate: (Int) -> Unit,
    movePrevChapter: () -> Unit,
    moveNextChapter: () -> Unit,
    setReaderMode: (String) -> Unit,
    setOrientation: (OrientationLock) -> Unit,
    setImageScale: (ImageScale) -> Unit,
    autoScroll: Boolean,
    setAutoScroll: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible,
        enter = slideInVertically(BarSlide) { it } + fadeIn(BarFade),
        exit = slideOutVertically(BarSlide) { it } + fadeOut(BarFade),
        modifier = modifier,
    ) {
        val chapterPages = remember(pages, chapter) {
            pages.filter { it is ReaderPage && it.chapter.chapter.id == chapter.chapter.id }
        }
        val currentIndex = (currentPage as? ReaderPage)
            ?.takeIf { it.chapter.chapter.id == chapter.chapter.id }
            ?.index2
            ?: 0
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChapterNavigator(
                    isRtl = direction == Direction.Left,
                    hasPrevious = previousChapter != null,
                    hasNext = nextChapter != null,
                    onPrevious = movePrevChapter,
                    onNext = moveNextChapter,
                    count = chapterPages.size,
                    currentIndex = currentIndex,
                    onPageSelected = { navigate(pages.indexOf(chapterPages.getOrNull(it) ?: return@ChapterNavigator)) },
                )
                QuickActions(
                    continuous = continuous,
                    vertical = direction.isVertical,
                    readerModes = readerModes,
                    seriesReaderMode = seriesReaderMode,
                    setReaderMode = setReaderMode,
                    setOrientation = setOrientation,
                    imageScale = imageScale,
                    setImageScale = setImageScale,
                    autoScroll = autoScroll,
                    setAutoScroll = setAutoScroll,
                    onOpenSettings = onOpenSettings,
                )
            }
        }
    }
}

@Composable
private fun ChapterNavigator(
    isRtl: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    count: Int,
    currentIndex: Int,
    onPageSelected: (Int) -> Unit,
) {
    val buttonColors = IconButtonDefaults.filledIconButtonColors(
        containerColor = hudColor(),
        contentColor = MaterialTheme.colorScheme.primary,
        disabledContainerColor = hudColor(),
    )
    // the buttons follow the reading direction, never the system locale
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                if (isRtl) onNext else onPrevious,
                enabled = if (isRtl) hasNext else hasPrevious,
                colors = buttonColors,
            ) {
                Icon(Icons.Outlined.SkipPrevious, stringResource(if (isRtl) MR.strings.nav_next_chapter else MR.strings.nav_prev_chapter))
            }
            if (count > 1) {
                CompositionLocalProvider(LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    PageSlider(Modifier.weight(1F).padding(horizontal = 4.dp), count, currentIndex, onPageSelected)
                }
            } else {
                Spacer(Modifier.weight(1F))
            }
            FilledIconButton(
                if (isRtl) onPrevious else onNext,
                enabled = if (isRtl) hasPrevious else hasNext,
                colors = buttonColors,
            ) {
                Icon(Icons.Outlined.SkipNext, stringResource(if (isRtl) MR.strings.nav_prev_chapter else MR.strings.nav_next_chapter))
            }
        }
    }
}

@Composable
private fun PageSlider(
    modifier: Modifier,
    count: Int,
    currentIndex: Int,
    onPageSelected: (Int) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0F) }
    var lastSent by remember { mutableStateOf(-1) }
    val haptic = LocalHapticFeedback.current
    val last = (count - 1).toFloat()
    val value = (if (dragging) dragValue else currentIndex.toFloat()).coerceIn(0F, last)
    Row(
        modifier.clip(RoundedCornerShape(24.dp)).background(hudColor()).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.CenterEnd) {
            Text((value.roundToInt() + 1).toString(), style = MaterialTheme.typography.bodyMedium)
            Text(count.toString(), style = MaterialTheme.typography.bodyMedium, color = Color.Transparent)
        }
        Slider(
            value = value,
            onValueChange = {
                dragging = true
                dragValue = it
                val index = it.roundToInt()
                if (index != lastSent) {
                    lastSent = index
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPageSelected(index)
                }
            },
            onValueChangeFinished = {
                dragging = false
                lastSent = -1
            },
            valueRange = 0F..last,
            steps = (count - 2).coerceAtLeast(0),
            modifier = Modifier.weight(1F).padding(horizontal = 8.dp),
        )
        Text(count.toString(), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun QuickActions(
    continuous: Boolean,
    vertical: Boolean,
    readerModes: ImmutableList<String>,
    seriesReaderMode: String,
    setReaderMode: (String) -> Unit,
    setOrientation: (OrientationLock) -> Unit,
    imageScale: ImageScale,
    setImageScale: (ImageScale) -> Unit,
    autoScroll: Boolean,
    setAutoScroll: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val modeDialog = remember { DialogState() }
    val orientationDialog = remember { DialogState() }
    val orientation = LocalOrientationController.current
    Row(
        Modifier.fillMaxWidth().background(hudColor()).navigationBarsPadding().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(modeDialog::show) {
            Icon(
                if (continuous) Icons.Outlined.ViewDay else Icons.AutoMirrored.Outlined.MenuBook,
                stringResource(MR.strings.reader_mode),
                tint = activeTint(seriesReaderMode != MangaMeta.DEFAULT_READER_MODE),
            )
        }
        if (orientation != null) {
            IconButton(orientationDialog::show) {
                Icon(
                    orientation.current.icon(),
                    stringResource(MR.strings.reader_orientation),
                    tint = activeTint(orientation.current != OrientationLock.Unlocked),
                )
            }
        }
        if (continuous) {
            if (vertical) {
                IconButton({ setAutoScroll(!autoScroll) }) {
                    Icon(
                        if (autoScroll) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        stringResource(MR.strings.reader_auto_scroll),
                        tint = activeTint(autoScroll),
                    )
                }
            }
        } else {
            ScalePicker(imageScale, setImageScale)
        }
        IconButton(onOpenSettings) { Icon(Icons.Outlined.Settings, stringResource(MR.strings.reader_settings)) }
    }
    ReaderModePickerDialog(modeDialog, readerModes, seriesReaderMode, setReaderMode)
    if (orientation != null) {
        OrientationPickerDialog(orientationDialog, orientation.current, setOrientation)
    }
}

@Composable
private fun activeTint(active: Boolean) = if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current

@Composable
private fun ScalePicker(
    selected: ImageScale,
    onSelect: (ImageScale) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }) { Icon(Icons.Outlined.FitScreen, stringResource(MR.strings.image_scale)) }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            QuickScales.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.res)) },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                    leadingIcon = { RadioButton(selected = option == selected, onClick = null) },
                )
            }
        }
    }
}
