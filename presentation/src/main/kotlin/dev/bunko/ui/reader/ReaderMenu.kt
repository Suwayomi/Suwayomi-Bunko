/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.bunko.ui.reader

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import android.content.Context
import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.LocalContentColor as M2ContentColor
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.bunko.domain.reader.model.ImageSaveTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import dev.bunko.core.lang.withIOContext
import dev.bunko.domain.reader.model.Direction
import dev.bunko.domain.reader.model.ImageScale
import dev.bunko.domain.reader.model.NavigationMode
import dev.bunko.domain.reader.model.ReaderBackground
import dev.bunko.i18n.MR
import dev.bunko.ui.base.LocalViewModels
import dev.bunko.ui.base.model.StableHolder
import dev.bunko.ui.reader.model.Navigation
import dev.bunko.ui.reader.model.PageMove
import dev.bunko.ui.reader.model.ReaderChapter
import dev.bunko.ui.reader.model.ReaderItem
import dev.bunko.ui.reader.model.ReaderPage
import dev.bunko.ui.reader.navigation.EdgeNavigation
import dev.bunko.ui.reader.navigation.KindlishNavigation
import dev.bunko.ui.reader.navigation.LNavigation
import dev.bunko.ui.reader.navigation.RightAndLeftNavigation
import dev.bunko.ui.reader.navigation.ViewerNavigation
import dev.bunko.ui.reader.viewer.ContinuousReader
import dev.bunko.ui.reader.viewer.PagerReader
import dev.bunko.ui.reader.viewer.readerZoom
import dev.bunko.uicore.components.ErrorScreen
import dev.bunko.uicore.components.LoadingScreen
import dev.bunko.uicore.components.mangaAspectRatio
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun ReaderMenu(
    chapterId: Long,
    mangaId: Long,
    onCloseRequest: () -> Unit,
) {
    val viewModels = LocalViewModels.current
    val vm = remember { viewModels.readerViewModel(ReaderMenuViewModel.Params(chapterId, mangaId)) }
    DisposableEffect(vm) {
        onDispose(vm::onDispose)
    }
    DisposableEffect(Unit) {
        onDispose(vm::sendProgress)
    }

    val state by vm.state.collectAsState()
    val previousChapter by vm.previousChapter.collectAsState()
    val chapter by vm.chapter.collectAsState()
    val nextChapter by vm.nextChapter.collectAsState()
    val pages by vm.pages.collectAsState()
    val readerModes by vm.readerModes.collectAsState()
    val readerMode by vm.readerMode.collectAsState()
    val continuous by vm.readerModeSettings.continuous.collectAsState()
    val direction by vm.readerModeSettings.direction.collectAsState()
    val padding by vm.readerModeSettings.padding.collectAsState()
    val imageScale by vm.readerModeSettings.imageScale.collectAsState()
    val fitSize by vm.readerModeSettings.fitSize.collectAsState()
    val maxSize by vm.readerModeSettings.maxSize.collectAsState()
    val navigationMode by vm.readerModeSettings.navigationMode.collectAsState()
    val currentPage by vm.currentPage.collectAsState()
    val currentPageOffset by vm.currentPageOffset.collectAsState()
    val readerSettingsMenuOpen by vm.readerSettingsMenuOpen.collectAsState()
    val volumeKeys by vm.volumeKeys.collectAsState()
    val volumeKeysInverted by vm.volumeKeysInverted.collectAsState()
    val tappingInvertMode by vm.tappingInvertMode.collectAsState()
    val mangaTitle by vm.mangaTitle.collectAsState()
    val dimmer by vm.dimmer.collectAsState()
    val autoScroll by vm.autoScroll.collectAsState()
    val autoScrollSpeed by vm.autoScrollSpeed.collectAsState()
    val incognito by vm.incognito.collectAsState()
    val background by vm.background.collectAsState()

    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    var volumeKeyHeld by remember { mutableStateOf(false) }
    var menuPage by remember { mutableStateOf<ReaderPage?>(null) }
    val imageSaveTarget by vm.imageSaveTarget.collectAsState()
    Surface(
        Modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                hasFocus = it.hasFocus
            }
            .focusable()
            .onKeyEvent {
                if (it.key == Key.VolumeUp || it.key == Key.VolumeDown) {
                    if (!volumeKeys) return@onKeyEvent false
                    return@onKeyEvent when (it.type) {
                        KeyEventType.KeyDown -> {
                            if (!volumeKeyHeld) {
                                volumeKeyHeld = true
                                val back = (it.key == Key.VolumeUp) != volumeKeysInverted
                                vm.navigate(if (back) Navigation.PREV else Navigation.NEXT)
                            }
                            true
                        }

                        KeyEventType.KeyUp -> {
                            volumeKeyHeld = false
                            true
                        }

                        else -> false
                    }
                }
                if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (it.key) {
                    Key.W, Key.DirectionUp -> vm.navigate(Navigation.UP)
                    Key.S, Key.DirectionDown -> vm.navigate(Navigation.DOWN)
                    Key.A, Key.DirectionLeft -> vm.navigate(Navigation.LEFT)
                    Key.D, Key.DirectionRight -> vm.navigate(Navigation.RIGHT)
                    Key.Spacebar -> vm.navigate(Navigation.NEXT)
                    else -> false
                }
            },
    ) {
        Crossfade(state to chapter) { (state, chapter) ->
            if (state is ReaderChapter.State.Loaded && chapter != null) {
                if (pages.any { it is ReaderPage }) {
                    ReaderHudMenu(
                        mangaTitle = mangaTitle,
                        previousChapter = previousChapter,
                        chapter = chapter,
                        nextChapter = nextChapter,
                        pages = pages,
                        readerModes = readerModes,
                        seriesReaderMode = vm.seriesReaderMode.collectAsState().value,
                        seriesOrientation = vm.seriesOrientation.collectAsState().value,
                        setSeriesOrientation = vm::setSeriesOrientation,
                        continuous = continuous,
                        direction = direction,
                        padding = padding,
                        imageScale = imageScale,
                        fitSize = fitSize,
                        maxSize = maxSize,
                        navigationViewer = remember(navigationMode, tappingInvertMode) {
                            navigationMode.toNavigation()?.apply { invertMode = tappingInvertMode }
                        },
                        currentPage = currentPage,
                        currentPageOffset = currentPageOffset,
                        navigate = vm::navigate,
                        navigateTap = vm::navigate,
                        pageEmitterHolder = vm.pageEmitter,
                        retry = vm::retry,
                        progress = vm::progress,
                        updateLastPageReadOffset = vm::updateLastPageReadOffset,
                        requestPreloadChapter = vm::requestPreloadChapter,
                        readerMenuOpen = readerSettingsMenuOpen,
                        setMangaReaderMode = vm::setMangaReaderMode,
                        setImageScale = vm::setImageScale,
                        movePrevChapter = vm::prevChapter,
                        moveNextChapter = vm::nextChapter,
                        volumeKeys = volumeKeys,
                        setVolumeKeys = vm::setVolumeKeys,
                        dimmer = dimmer,
                        setDimmer = vm::setDimmer,
                        autoScroll = autoScroll,
                        setAutoScroll = vm::setAutoScroll,
                        autoScrollSpeed = autoScrollSpeed,
                        setAutoScrollSpeed = vm::setAutoScrollSpeed,
                        background = background,
                        setBackground = vm::setBackground,
                        incognito = incognito,
                        onLongPress = { menuPage = currentPage as? ReaderPage },
                        onCloseRequest = onCloseRequest,
                    )
                } else {
                    ErrorScreen(stringResource(MR.strings.no_pages_found))
                }
            } else {
                LoadingScreen(
                    state is ReaderChapter.State.Wait || state is ReaderChapter.State.Loading,
                    errorMessage = (state as? ReaderChapter.State.Error)?.error?.let { stringResource(it.toReaderFailure().message) },
                    retry = vm::init,
                )
            }
        }
    }

    LaunchedEffect(hasFocus) {
        if (!hasFocus) {
            focusRequester.requestFocus()
        }
    }

    menuPage?.let { page ->
        var dontAsk by remember(page) { mutableStateOf(false) }
        fun save(target: ImageSaveTarget) {
            menuPage = null
            vm.savePage(page, target, dontAsk)
        }
        Dialog(onDismissRequest = { menuPage = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.width(IntrinsicSize.Max), RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.widthIn(min = 180.dp).padding(vertical = 6.dp)) {
                    if (imageSaveTarget == ImageSaveTarget.Ask) {
                        SaveOption(stringResource(MR.strings.reader_save_to_phone)) { save(ImageSaveTarget.Phone) }
                        SaveOption(stringResource(MR.strings.reader_save_to_server)) { save(ImageSaveTarget.Server) }
                    } else {
                        SaveOption(stringResource(MR.strings.reader_save_image)) { save(imageSaveTarget) }
                    }
                    if (imageSaveTarget == ImageSaveTarget.Ask) Row(
                        Modifier.fillMaxWidth().clickable { dontAsk = !dontAsk }.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = dontAsk, onCheckedChange = { dontAsk = it })
                        Text(stringResource(MR.strings.reader_dont_ask_again), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderHudMenu(
    mangaTitle: String,
    previousChapter: ReaderChapter?,
    chapter: ReaderChapter,
    nextChapter: ReaderChapter?,
    pages: ImmutableList<ReaderItem>,
    readerModes: ImmutableList<String>,
    seriesReaderMode: String,
    seriesOrientation: OrientationLock?,
    setSeriesOrientation: (OrientationLock) -> Unit,
    continuous: Boolean,
    direction: Direction,
    padding: Int,
    imageScale: ImageScale,
    fitSize: Boolean,
    maxSize: Int,
    navigationViewer: ViewerNavigation?,
    currentPage: ReaderItem?,
    currentPageOffset: Int,
    navigate: (Int) -> Unit,
    navigateTap: (Navigation) -> Unit,
    pageEmitterHolder: StableHolder<SharedFlow<PageMove>>,
    retry: (ReaderPage) -> Unit,
    progress: (ReaderItem) -> Unit,
    updateLastPageReadOffset: (Int) -> Unit,
    requestPreloadChapter: (ReaderChapter) -> Unit,
    readerMenuOpen: Boolean,
    setMangaReaderMode: (String) -> Unit,
    setImageScale: (ImageScale) -> Unit,
    movePrevChapter: () -> Unit,
    moveNextChapter: () -> Unit,
    volumeKeys: Boolean,
    setVolumeKeys: (Boolean) -> Unit,
    dimmer: Float,
    setDimmer: (Float) -> Unit,
    autoScroll: Boolean,
    setAutoScroll: (Boolean) -> Unit,
    autoScrollSpeed: Int,
    setAutoScrollSpeed: (Int) -> Unit,
    background: ReaderBackground,
    setBackground: (ReaderBackground) -> Unit,
    incognito: Boolean,
    onLongPress: () -> Unit,
    onCloseRequest: () -> Unit,
) {
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val orientation = LocalOrientationController.current
    val latestOrientation by rememberUpdatedState(orientation)
    LaunchedEffect(seriesOrientation) {
        seriesOrientation?.let { latestOrientation?.onChange(it) }
    }
    val setOrientation: (OrientationLock) -> Unit = { lock ->
        orientation?.onChange(lock)
        setSeriesOrientation(lock)
    }
    if (settingsOpen) {
        ReaderSettingsSheet(
            state = ReaderSettingsState(
                readerModes = readerModes,
                seriesReaderMode = seriesReaderMode,
                orientation = orientation?.current,
                background = background,
                volumeKeys = volumeKeys,
                dimmer = dimmer,
                autoScrollSpeed = autoScrollSpeed,
            ),
            actions = ReaderSettingsActions(
                setReaderMode = setMangaReaderMode,
                setOrientation = setOrientation,
                setBackground = setBackground,
                setVolumeKeys = setVolumeKeys,
                setDimmer = setDimmer,
                setAutoScrollSpeed = setAutoScrollSpeed,
            ),
            onDismiss = { settingsOpen = false },
        )
    }
    val canvas = background.toColor()
    Box(Modifier.fillMaxSize().background(canvas)) {
        val scrim = Color.Black.copy(alpha = dimmer)
        val onCanvas = if (canvas.luminance() > 0.5F) Color.Black else Color.White
        CompositionLocalProvider(
            LocalContentColor provides onCanvas,
            M2ContentColor provides onCanvas,
        ) {
            ReaderLayout(
                modifier = Modifier.drawWithContent {
                    drawContent()
                    if (dimmer > 0f) drawRect(scrim)
                },
                pages = pages,
                continuous = continuous,
                direction = direction,
                padding = padding,
                imageScale = imageScale,
                fitSize = fitSize,
                maxSize = maxSize,
                navigationViewer = navigationViewer,
                currentPage = currentPage,
                currentPageOffset = currentPageOffset,
                navigateTap = { navigation ->
                    if (autoScroll && !readerMenuOpen) setAutoScroll(false)
                    navigateTap(navigation)
                },
                onLongPress = onLongPress,
                pageEmitterHolder = pageEmitterHolder,
                retry = retry,
                progress = progress,
                updateLastPageReadOffset = updateLastPageReadOffset,
                requestPreloadChapter = requestPreloadChapter,
                autoScroll = autoScroll && !readerMenuOpen,
                autoScrollSpeed = autoScrollSpeed,
            )
        }
        val uriHandler = LocalUriHandler.current
        ReaderTopBar(
            visible = readerMenuOpen,
            mangaTitle = mangaTitle,
            chapterName = chapter.chapter.name,
            onBack = onCloseRequest,
            onOpenInBrowser = chapter.chapter.realUrl?.let { url -> { uriHandler.openUri(url) } },
            incognito = incognito,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        ReaderBottomBar(
            visible = readerMenuOpen,
            previousChapter = previousChapter,
            chapter = chapter,
            nextChapter = nextChapter,
            direction = direction,
            continuous = continuous,
            pages = pages,
            currentPage = currentPage,
            readerModes = readerModes,
            seriesReaderMode = seriesReaderMode,
            imageScale = imageScale,
            navigate = navigate,
            movePrevChapter = movePrevChapter,
            moveNextChapter = moveNextChapter,
            setReaderMode = setMangaReaderMode,
            setOrientation = setOrientation,
            setImageScale = setImageScale,
            autoScroll = autoScroll,
            setAutoScroll = setAutoScroll,
            onOpenSettings = { settingsOpen = true },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun ReaderLayout(
    modifier: Modifier,
    pages: ImmutableList<ReaderItem>,
    continuous: Boolean,
    direction: Direction,
    padding: Int,
    imageScale: ImageScale,
    fitSize: Boolean,
    maxSize: Int,
    navigationViewer: ViewerNavigation?,
    currentPage: ReaderItem?,
    currentPageOffset: Int,
    navigateTap: (Navigation) -> Unit,
    onLongPress: () -> Unit,
    pageEmitterHolder: StableHolder<SharedFlow<PageMove>>,
    retry: (ReaderPage) -> Unit,
    progress: (ReaderItem) -> Unit,
    updateLastPageReadOffset: (Int) -> Unit,
    requestPreloadChapter: (ReaderChapter) -> Unit,
    autoScroll: Boolean,
    autoScrollSpeed: Int,
) {
    val loadingModifier = Modifier.widthIn(max = 700.dp)
        .fillMaxWidth()
        .aspectRatio(mangaAspectRatio)
    val onTap = { position: Offset, size: IntSize ->
        navigateTap(navigationViewer?.getAction(position, size.toSize()) ?: Navigation.MENU)
    }
    if (continuous) {
        ContinuousReader(
            modifier = modifier.fillMaxSize().readerZoom(pagerAxis = null, onLongPress = onLongPress, onTap = onTap),
            pages = pages,
            direction = direction,
            maxSize = maxSize,
            padding = padding,
            currentPage = currentPage,
            currentPageOffset = currentPageOffset,
            loadingModifier = loadingModifier,
            pageContentScale = if (fitSize) {
                if (direction.isVertical) {
                    ContentScale.FillWidth
                } else {
                    ContentScale.FillHeight
                }
            } else {
                ContentScale.Fit
            },
            pageEmitterHolder = pageEmitterHolder,
            retry = retry,
            progress = progress,
            updateLastPageReadOffset = updateLastPageReadOffset,
            requestPreloadChapter = requestPreloadChapter,
            autoScroll = autoScroll,
            autoScrollSpeed = autoScrollSpeed,
        )
    } else {
        PagerReader(
            modifier = modifier,
            direction = direction,
            currentPage = currentPage,
            pages = pages,
            loadingModifier = loadingModifier,
            pageContentScale = imageScale.toContentScale(),
            pageEmitterHolder = pageEmitterHolder,
            onTap = onTap,
            onLongPress = onLongPress,
            retry = retry,
            progress = progress,
            requestPreloadChapter = requestPreloadChapter,
        )
    }
}

@Composable
fun ReaderImage(
    imageIndex: Int,
    drawableHolder: StableHolder<(suspend () -> ReaderPage.ImageDecodeState)?>,
    bitmapInfo: ReaderPage.BitmapInfo?,
    progress: Float,
    status: ReaderPage.Status,
    error: ReaderFailure?,
    imageModifier: Modifier = Modifier.fillMaxSize(),
    loadingModifier: Modifier = imageModifier,
    contentScale: ContentScale = ContentScale.Fit,
    retry: (Int) -> Unit,
) {
    Crossfade(drawableHolder to status) { (drawableHolder, status) ->
        val decodeState = produceState<ReaderPage.ImageDecodeState?>(null, drawableHolder) {
            val callback = drawableHolder.item
            if (callback != null) {
                withIOContext {
                    value = callback()
                }
            }
        }
        val decode = decodeState.value
        if (decode != null && decode is ReaderPage.ImageDecodeState.Success) {
            Image(
                bitmap = decode.bitmap,
                modifier = imageModifier,
                contentDescription = null,
                contentScale = contentScale,
                filterQuality = FilterQuality.High,
            )
        } else {
            LoadingScreen(
                status == ReaderPage.Status.QUEUE || status == ReaderPage.Status.WORKING,
                loadingModifier.let { modifier ->
                    if (bitmapInfo != null) {
                        modifier.heightIn(min = with(LocalDensity.current) { bitmapInfo.size.height.toDp() })
                    } else {
                        modifier
                    }
                },
                progress,
                (error ?: ReaderFailure.Decode.takeIf { decode != null })?.let { stringResource(it.message) },
            ) { retry(imageIndex) }
        }
    }
}

@Composable
fun ChapterSeparator(
    previousChapter: ReaderChapter?,
    nextChapter: ReaderChapter?,
    requestPreloadChapter: (ReaderChapter) -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(350.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                previousChapter == null && nextChapter != null -> {
                    Text(stringResource(MR.strings.no_previous_chapter))
                }

                previousChapter != null && nextChapter != null -> {
                    val prevChapter by previousChapter.stateObserver.collectAsState()
                    when (prevChapter) {
                        ReaderChapter.State.Loading, ReaderChapter.State.Wait -> {
                            LaunchedEffect(Unit) {
                                requestPreloadChapter(previousChapter)
                            }
                            CircularProgressIndicator()
                        }

                        else -> Unit
                    }
                    Text(stringResource(MR.strings.previous_chapter, previousChapter.chapter.name))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(MR.strings.next_chapter, nextChapter.chapter.name))
                    val nexChapter by nextChapter.stateObserver.collectAsState()
                    when (nexChapter) {
                        ReaderChapter.State.Loading, ReaderChapter.State.Wait -> {
                            LaunchedEffect(Unit) {
                                requestPreloadChapter(nextChapter)
                            }
                            CircularProgressIndicator()
                        }

                        else -> Unit
                    }
                }

                previousChapter != null && nextChapter == null -> {
                    Text(stringResource(MR.strings.no_next_chapter))
                }
            }
        }
    }
}

fun NavigationMode.toNavigation() =
    when (this) {
        NavigationMode.Disabled -> null
        NavigationMode.RightAndLeftNavigation -> RightAndLeftNavigation()
        NavigationMode.KindlishNavigation -> KindlishNavigation()
        NavigationMode.LNavigation -> LNavigation()
        NavigationMode.EdgeNavigation -> EdgeNavigation()
    }

@Composable
fun ReaderBackground.toColor(): Color =
    when (this) {
        ReaderBackground.Black -> Color.Black
        ReaderBackground.Gray -> ReaderGray
        ReaderBackground.White -> Color.White
        ReaderBackground.System -> MaterialTheme.colorScheme.background
    }

private val ReaderGray = Color(0xFF202125)

fun ImageScale.toContentScale() =
    when (this) {
        ImageScale.FitScreen -> ContentScale.Fit
        ImageScale.FitHeight -> ContentScale.FillHeight
        ImageScale.FitWidth -> ContentScale.FillWidth
        ImageScale.OriginalSize -> ContentScale.None
        ImageScale.SmartFit -> ContentScale.Fit
        ImageScale.Stretch -> ContentScale.FillBounds
    }

class ReaderLauncher(
    private val context: Context,
) {
    fun launch(
        chapterId: Long,
        mangaId: Long,
    ) {
        Intent(context, Class.forName("dev.bunko.android.ReaderActivity")).apply {
            putExtra("manga", mangaId)
            putExtra("chapter", chapterId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }.let(context::startActivity)
    }
}

@Composable
fun rememberReaderLauncher(): ReaderLauncher {
    val context = LocalContext.current
    return remember(context) { ReaderLauncher(context) }
}

@Composable
private fun SaveOption(text: String, onClick: () -> Unit) {
    Text(text, Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyLarge)
}
