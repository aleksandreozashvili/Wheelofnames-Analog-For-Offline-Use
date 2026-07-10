package com.example.ui.components

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.sound.SoundSynthesizer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WheelView(
    names: List<String>,
    rotationAngle: Float,
    theme: String,
    centerEmoji: String,
    modifier: Modifier = Modifier,
    soundSynthesizer: SoundSynthesizer,
    onTick: () -> Unit = {}
) {
    val sweepAngle = if (names.isNotEmpty()) 360f / names.size else 360f
    val palette = remember(theme) { getPalette(theme) }
    val textColor = remember(theme) { getThemeTextColor(theme) }

    // Wobble animation for the physical pointer
    val pointerRotation = remember { Animatable(0f) }
    val lastTickIndex = remember { mutableStateOf(-1) }

    // Track segment boundary crossing in real-time
    LaunchedEffect(rotationAngle) {
        if (names.isNotEmpty()) {
            // Pointer is at -90 degrees (top of the wheel)
            val relativeAngle = (-90f - rotationAngle) % 360f
            val positiveAngle = (relativeAngle + 360f) % 360f
            val currentSegmentIndex = (positiveAngle / sweepAngle).toInt().coerceIn(0, names.size - 1)

            if (lastTickIndex.value != currentSegmentIndex) {
                lastTickIndex.value = currentSegmentIndex
                onTick()
                
                // Animate pointer flap / wobble
                pointerRotation.snapTo(12f)
                pointerRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier.testTag("wheel_container"),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow/shadow ring behind the wheel
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) / 2 * 0.95f

            // Outer dark ring to frame the wheel
            drawCircle(
                color = Color(0xFF1E293B),
                radius = radius + 6.dp.toPx(),
                center = center
            )
            // Silver bezel rim
            drawCircle(
                color = Color(0xFFE2E8F0),
                radius = radius + 2.dp.toPx(),
                center = center
            )
        }

        // The Spinning Wheel Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(rotationZ = rotationAngle)
                .testTag("spinning_wheel_canvas")
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) / 2 * 0.95f
            val sizeDim = radius * 2

            if (names.isEmpty()) {
                // Draw a beautiful empty wheel state
                drawCircle(
                    color = Color.LightGray,
                    radius = radius,
                    center = center
                )
                return@Canvas
            }

            // Draw individual colored segments
            for (i in names.indices) {
                val startAngle = i * sweepAngle
                val color = palette[i % palette.size]
                
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(sizeDim, sizeDim)
                )
            }

            // Draw names along radial lines
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas

                for (i in names.indices) {
                    val name = names[i]
                    val segmentCenterAngle = i * sweepAngle + (sweepAngle / 2f)
                    
                    // Native paint configuration for readable text
                    val paint = Paint().apply {
                        color = textColor.toArgb()
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        // Scale text size intelligently based on the number of segments to prevent overlapping
                        val baseSize = radius * 0.08f
                        val scaleFactor = when {
                            names.size <= 5 -> 1.0f
                            names.size <= 10 -> 0.85f
                            names.size <= 20 -> 0.65f
                            else -> (12f / names.size).coerceAtLeast(0.35f)
                        }
                        textSize = baseSize * scaleFactor
                        textAlign = Paint.Align.RIGHT
                    }

                    // Save canvas state
                    nativeCanvas.save()
                    
                    // Rotate and translate to the sector's center axis
                    nativeCanvas.translate(center.x, center.y)
                    nativeCanvas.rotate(segmentCenterAngle)

                    // Get bounds to adjust baseline centering
                    val bounds = Rect()
                    paint.getTextBounds(name, 0, name.length, bounds)
                    val textHeight = bounds.height()

                    // Truncate name if it's too long to fit in the sector
                    val availableWidth = radius * 0.75f
                    val measuredWidth = paint.measureText(name)
                    val displayName = if (measuredWidth > availableWidth) {
                        var tempName = name
                        while (tempName.isNotEmpty() && paint.measureText("$tempName...") > availableWidth) {
                            tempName = tempName.dropLast(1)
                        }
                        if (tempName.isNotEmpty()) "$tempName..." else "..."
                    } else {
                        name
                    }

                    // Draw text. Offset it slightly from the outer edge of the wheel
                    // We draw along positive X-axis because rotation rotates the positive X-axis
                    nativeCanvas.drawText(
                        displayName,
                        radius * 0.82f, // End of text is near the outer margin
                        textHeight / 2f, // Vertically center the text baseline
                        paint
                    )

                    // Restore canvas state
                    nativeCanvas.restore()
                }
            }

            // Draw dividing thin silver spokes to make the wheel segments pop
            for (i in names.indices) {
                val spokeAngleRad = (i * sweepAngle) * PI / 180f
                val endX = center.x + radius * cos(spokeAngleRad).toFloat()
                val endY = center.y + radius * sin(spokeAngleRad).toFloat()
                drawLine(
                    color = Color(0x33FFFFFF), // Transparent white overlay line
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Draw Center Hub (remains static and doesn't spin, displays the emoji)
        Canvas(modifier = Modifier.size(72.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val hubRadius = size.width / 2

            // Dark inner hub border
            drawCircle(
                color = Color(0xFF0F172A),
                radius = hubRadius,
                center = center
            )
            // Silver hub inner fill
            drawCircle(
                color = Color(0xFFF1F5F9),
                radius = hubRadius - 4.dp.toPx(),
                center = center
            )

            // We'll draw the emoji centered on the hub
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                val paint = Paint().apply {
                    textSize = hubRadius * 1.1f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val bounds = Rect()
                paint.getTextBounds(centerEmoji, 0, centerEmoji.length, bounds)
                nativeCanvas.drawText(
                    centerEmoji,
                    center.x,
                    center.y + (bounds.height() / 2f),
                    paint
                )
            }
        }

        // Draw physical pointer / flapper at the top pointing down
        Canvas(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(width = 36.dp, height = 44.dp)
                .graphicsLayer {
                    // Pivot at the top-center where the peg holds the flapper
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                    rotationZ = pointerRotation.value
                }
                .testTag("wheel_pointer")
        ) {
            val width = size.width
            val height = size.height

            // Pointer triangle shape pointing downwards
            val path = Path().apply {
                moveTo(width / 2f, height) // Tip pointing down
                lineTo(0f, 0f) // Top left corner
                lineTo(width, 0f) // Top right corner
                close()
            }

            // Golden body with high-contrast red accent
            drawPath(path = path, color = Color(0xFFEF4444)) // Vibrant red arrow
            
            // Draw small metallic peg at the top center
            drawCircle(
                color = Color(0xFFF1F5F9),
                radius = 5.dp.toPx(),
                center = Offset(width / 2f, 5.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF475569),
                radius = 3.dp.toPx(),
                center = Offset(width / 2f, 5.dp.toPx())
            )
        }
    }
}

// Color palettes for customization
fun getPalette(theme: String): List<Color> {
    return when (theme) {
        "Rainbow" -> listOf(
            Color(0xFFEF4444), // Crimson
            Color(0xFFF97316), // Orange
            Color(0xFFFBBF24), // Gold Yellow
            Color(0xFF22C55E), // Vibrant Green
            Color(0xFF3B82F6), // Sky Blue
            Color(0xFF8B5CF6), // Royal Violet
            Color(0xFFEC4899)  // Deep Pink
        )
        "Cosmic" -> listOf(
            Color(0xFF1E1B4B), // Indigo Depth
            Color(0xFF4C1D95), // Royal Amethyst
            Color(0xFF2563EB), // Cosmic Azure
            Color(0xFF0D9488), // Aurora Teal
            Color(0xFF701A75), // Magenta Nebula
            Color(0xFF0284C7)  // Deep Sky Blue
        )
        "Neon" -> listOf(
            Color(0xFFFF007F), // Electric Fuchsia
            Color(0xFF39FF14), // Cyber Lime
            Color(0xFF00F5FF), // Cyber Cyan
            Color(0xFFFFEA00), // Electric Banana
            Color(0xFFB000FF)  // Synthwave Violet
        )
        "Pastel" -> listOf(
            Color(0xFFFFC0CB), // Baby Pink
            Color(0xFFFFDAB9), // Soft Peach
            Color(0xFFE0F7FA), // Mint Dew
            Color(0xFFE8EAF6), // Soft Lavender
            Color(0xFFFFF9C4), // Soft Lemon
            Color(0xFFC8E6C9)  // Pale Emerald
        )
        else -> listOf(
            Color(0xFFEF4444),
            Color(0xFFF97316),
            Color(0xFFFBBF24),
            Color(0xFF22C55E),
            Color(0xFF3B82F6),
            Color(0xFF8B5CF6)
        )
    }
}

fun getThemeTextColor(theme: String): Color {
    return when (theme) {
        "Pastel" -> Color(0xFF1E293B) // Slate Dark for soft backgrounds
        else -> Color.White // High contrast white
    }
}
