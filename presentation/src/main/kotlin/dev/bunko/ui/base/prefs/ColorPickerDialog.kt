package dev.bunko.ui.base.prefs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ExperimentalGraphicsApi
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.bunko.i18n.MR
import dev.bunko.ui.base.dialog.AppDialog
import dev.bunko.ui.base.dialog.DialogButton
import dev.bunko.ui.base.dialog.DialogContentPadding
import dev.bunko.ui.base.dialog.DialogState
import dev.bunko.ui.util.compose.toHexString
import dev.bunko.ui.util.compose.toLong
import dev.bunko.uicore.components.keyboardHandler
import dev.bunko.uicore.resources.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.round

@Composable
fun ColorPickerDialog(
    state: DialogState,
    title: String,
    onCloseRequest: () -> Unit = {},
    onSelected: (Color) -> Unit,
    initialColor: Color = Color.Unspecified,
) {
    if (!state.isShowing) return
    var currentColor by remember(initialColor) { mutableStateOf(initialColor) }
    var showPresets by remember { mutableStateOf(true) }

    AppDialog(
        state = state,
        title = title,
        confirm = DialogButton(stringResource(MR.strings.action_select)) { onSelected(currentColor) },
        dismissText = stringResource(MR.strings.action_cancel),
        onDismiss = onCloseRequest,
        neutral = DialogButton(
            stringResource(if (showPresets) MR.strings.color_custom else MR.strings.color_presets),
        ) { showPresets = !showPresets },
    ) {
        Box(DialogContentPadding) {
            if (showPresets) {
                ColorPresets(initialColor = currentColor, onColorChanged = { currentColor = it })
            } else {
                ColorPalette(initialColor = currentColor, onColorChanged = { currentColor = it })
            }
        }
    }
}

@Composable
private fun ColorPresets(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
) {
    val presets = remember {
        if (initialColor.isSpecified) {
            (listOf(initialColor) + presetColors).distinct().toImmutableList()
        } else {
            presetColors
        }
    }

    var selectedColor by remember { mutableStateOf(initialColor.takeOrElse { presets.first() }) }
    var selectedShade by remember { mutableStateOf<Color?>(null) }

    val shades = remember(selectedColor) { getColorShades(selectedColor) }

    Column {
        SwatchRow(presets, selected = selectedColor.takeIf { selectedShade == null }) {
            selectedShade = null
            selectedColor = it
            onColorChanged(it)
        }
        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        SwatchRow(shades, selected = selectedShade) {
            selectedShade = it
            onColorChanged(it)
        }
    }
}

@Composable
private fun SwatchRow(
    colors: ImmutableList<Color>,
    selected: Color?,
    onClick: (Color) -> Unit,
) {
    FlowRow(
        maxItemsInEachRow = SWATCHES_PER_ROW,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        colors.forEach { color -> Swatch(color, isSelected = color == selected) { onClick(color) } }
        repeat((SWATCHES_PER_ROW - colors.size % SWATCHES_PER_ROW) % SWATCHES_PER_ROW) { Spacer(Modifier.size(SWATCH_SIZE)) }
    }
}

@Composable
private fun Swatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val ring = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(SWATCH_SIZE)
            .border(if (isSelected) 2.dp else 1.dp, ring, CircleShape)
            .padding(if (isSelected) 4.dp else 0.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                tint = if (color.luminance() > 0.5) Color.Black else Color.White,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private const val SWATCHES_PER_ROW = 5
private val SWATCH_SIZE = 44.dp

private fun getColorShades(color: Color): ImmutableList<Color> {
    val f = color.toLong()
    return listOf(
        shadeColor(f, 0.9),
        shadeColor(f, 0.7),
        shadeColor(f, 0.5),
        shadeColor(f, 0.333),
        shadeColor(f, 0.166),
        shadeColor(f, -0.125),
        shadeColor(f, -0.25),
        shadeColor(f, -0.375),
        shadeColor(f, -0.5),
        shadeColor(f, -0.675),
        shadeColor(f, -0.7),
        shadeColor(f, -0.775),
    ).toImmutableList()
}

private fun shadeColor(
    f: Long,
    percent: Double,
): Color {
    val t = if (percent < 0) 0.0 else 255.0
    val p = if (percent < 0) percent * -1 else percent
    val r = f shr 16
    val g = f shr 8 and 0x00FF
    val b = f and 0x0000FF

    val red = (round((t - r) * p) + r).toInt()
    val green = (round((t - g) * p) + g).toInt()
    val blue = (round((t - b) * p) + b).toInt()
    return Color(red = red, green = green, blue = blue, alpha = 255)
}

@Composable
fun ColorPalette(
    initialColor: Color = Color.White,
    onColorChanged: (Color) -> Unit = {},
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    var textFieldHex by remember { mutableStateOf(initialColor.toHexString()) }

    var hue by remember { mutableStateOf(initialColor.toHsv()[0]) }
    var hueCursor by remember { mutableStateOf(0f) }

    var matrixSize by remember { mutableStateOf(IntSize(0, 0)) }
    var matrixCursor by remember { mutableStateOf(Offset(0f, 0f)) }

    val saturationGradient = remember(hue, matrixSize) {
        Brush.linearGradient(
            colors = listOf(Color.White, hueToColor(hue)),
            start = Offset(0f, 0f),
            end = Offset(matrixSize.width.toFloat(), 0f),
        )
    }
    val valueGradient = remember(matrixSize) {
        Brush.linearGradient(
            colors = listOf(Color.White, Color.Black),
            start = Offset(0f, 0f),
            end = Offset(0f, matrixSize.height.toFloat()),
        )
    }

    val cursorColor = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outlineVariant
    val cursorStroke = Stroke(4f)

    fun setSelectedColor(
        color: Color,
        invalidate: Boolean = false,
    ) {
        selectedColor = color
        textFieldHex = color.toHexString()
        if (invalidate) {
            val hsv = color.toHsv()
            hue = hsv[0]
            matrixCursor = satValToCoordinates(hsv[1], hsv[2], matrixSize)
            hueCursor = hueToCoordinate(hsv[0], matrixSize)
        }
        onColorChanged(color)
    }

    Column {
        Row(Modifier.height(IntrinsicSize.Max)) {
            Box(
                Modifier
                    .aspectRatio(1f)
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .onSizeChanged {
                        matrixSize = it
                        val hsv = selectedColor.toHsv()
                        matrixCursor = satValToCoordinates(hsv[1], hsv[2], it)
                        hueCursor = hueToCoordinate(hue, it)
                    }
                    .drawWithContent {
                        drawRect(brush = valueGradient)
                        drawRect(brush = saturationGradient, blendMode = BlendMode.Multiply)
                        drawCircle(
                            Color.Black,
                            radius = 8f,
                            center = matrixCursor,
                            style = cursorStroke,
                        )
                        drawCircle(
                            Color.LightGray,
                            radius = 12f,
                            center = matrixCursor,
                            style = cursorStroke,
                        )
                    }
                    .pointerInput(Unit) {
                        detectMove { offset ->
                            val safeOffset = offset.copy(
                                x = offset.x.coerceIn(0f, matrixSize.width.toFloat()),
                                y = offset.y.coerceIn(0f, matrixSize.height.toFloat()),
                            )
                            matrixCursor = safeOffset
                            val newColor = matrixCoordinatesToColor(hue, safeOffset, matrixSize)
                            setSelectedColor(newColor)
                        }
                    },
            )
            Box(
                Modifier
                    .fillMaxHeight()
                    .requiredWidth(40.dp)
                    .padding(start = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .drawWithCache {
                        var h = 360f
                        val colors = MutableList(size.height.toInt()) {
                            hueToColor(h).also {
                                h -= 360f / size.height
                            }
                        }
                        val cursorSize = Size(size.width, 10f)
                        val cursorTopLeft = Offset(0f, hueCursor - (cursorSize.height / 2))
                        onDrawBehind {
                            colors.fastForEachIndexed { i, color ->
                                val pos = i.toFloat()
                                drawLine(color, Offset(0f, pos), Offset(size.width, pos))
                            }
                            drawRect(
                                cursorColor,
                                topLeft = cursorTopLeft,
                                size = cursorSize,
                                style = cursorStroke,
                            )
                        }
                    }
                    .pointerInput(Unit) {
                        detectMove { offset ->
                            val safeY = offset.y.coerceIn(0f, matrixSize.height.toFloat())
                            hueCursor = safeY
                            hue = hueCoordinatesToHue(safeY, matrixSize)
                            val newColor = matrixCoordinatesToColor(hue, matrixCursor, matrixSize)
                            setSelectedColor(newColor)
                        }
                    },
            )
        }
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(selectedColor).border(1.dp, outline, CircleShape),
            )
            OutlinedTextField(
                value = textFieldHex,
                onValueChange = {
                    val newColor = hexStringToColor(it)
                    if (newColor != null) {
                        setSelectedColor(newColor, invalidate = true)
                    } else {
                        textFieldHex = it
                    }
                },
                singleLine = true,
                modifier = Modifier.padding(start = 16.dp).weight(1f).keyboardHandler(singleLine = true) {
                    it.clearFocus()
                },
            )
        }
    }
}

private suspend fun PointerInputScope.detectMove(onMove: (Offset) -> Unit) {
    awaitEachGesture {
        var change = awaitFirstDown()
        while (change.pressed) {
            onMove(change.position)
            change = awaitPointerEvent().changes.first()
        }
    }
}


private fun matrixCoordinatesToColor(
    hue: Float,
    position: Offset,
    size: IntSize,
): Color {
    val saturation = 1f / size.width * position.x
    val value = 1f - (1f / size.height * position.y)
    return hsvToColor(hue, saturation, value)
}

private fun hueCoordinatesToHue(
    y: Float,
    size: IntSize,
): Float {
    val hue = 360f - y * 360f / size.height
    return hsvToColor(hue, 1f, 1f).toHsv()[0]
}

private fun satValToCoordinates(
    saturation: Float,
    value: Float,
    size: IntSize,
): Offset = Offset(saturation * size.width, ((1f - value) * size.height))

private fun hueToCoordinate(
    hue: Float,
    size: IntSize,
): Float = size.height - (hue * size.height / 360f)


@OptIn(ExperimentalGraphicsApi::class)
fun hsvToColor(
    hue: Float,
    saturation: Float,
    value: Float,
): Color = Color.hsv(hue, saturation.coerceIn(0F, 1F), value.coerceIn(0F, 1F))

private fun hueToColor(hue: Float): Color = hsvToColor(hue, 1f, 1f)

private val presetColors = listOf(
    Color(0xFFF44336),
    Color(0xFFE91E63),
    Color(0xFFFF2C93),
    Color(0xFF9C27B0),
    Color(0xFF673AB7),
    Color(0xFF3F51B5),
    Color(0xFF2196F3),
    Color(0xFF03A9F4),
    Color(0xFF00BCD4),
    Color(0xFF009688),
    Color(0xFF4CAF50),
    Color(0xFF8BC34A),
    Color(0xFFCDDC39),
    Color(0xFFFFEB3B),
    Color(0xFFFFC107),
    Color(0xFFFF9800),
    Color(0xFF795548),
    Color(0xFF607D8B),
    Color(0xFF9E9E9E),
).toImmutableList()

internal fun Color.toHsv(): FloatArray {
    fun Float.toIntColor() = (this * 256).toInt()
    val result = floatArrayOf(0f, 0f, 0f)
    android.graphics.Color.RGBToHSV(red.toIntColor(), green.toIntColor(), blue.toIntColor(), result)
    return result
}

internal fun hexStringToColor(hex: String): Color? =
    if (hex.isEmpty()) {
        null
    } else {
        try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: IllegalArgumentException) {
            null
        }
    }
