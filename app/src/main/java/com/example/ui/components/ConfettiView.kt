package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import kotlin.random.Random

private data class ConfettiParticle(
    var x: Float,
    var y: Float,
    val color: Color,
    val size: Float,
    var speedX: Float,
    var speedY: Float,
    var rotation: Float,
    val rotationSpeed: Float,
    val isCircle: Boolean
)

@Composable
fun ConfettiView(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val colors = listOf(
        Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFFBBF24),
        Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFF8B5CF6),
        Color(0xFFEC4899), Color(0xFF06B6D4), Color(0xFF10B981)
    )

    var particles by remember { mutableStateOf(emptyList<ConfettiParticle>()) }

    // Initialize particles when activated
    LaunchedEffect(isActive) {
        if (isActive) {
            particles = List(120) {
                ConfettiParticle(
                    x = Random.nextFloat() * 1000f, // updated dynamically in drawing
                    y = -Random.nextFloat() * 400f, // start above screen
                    color = colors.random(),
                    size = Random.nextFloat() * 12f + 8f,
                    speedX = Random.nextFloat() * 4f - 2f,
                    speedY = Random.nextFloat() * 6f + 4f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 5f - 2.5f,
                    isCircle = Random.nextBoolean()
                )
            }
        } else {
            particles = emptyList()
        }
    }

    // High performance gameloop animation
    LaunchedEffect(isActive) {
        if (isActive) {
            while (true) {
                withFrameMillis { _ ->
                    particles = particles.map { p ->
                        // Add organic wind drift / sinewave sway
                        val currentSway = kotlin.math.sin(p.y * 0.02f) * 0.5f
                        p.copy(
                            y = p.y + p.speedY,
                            x = p.x + p.speedX + currentSway,
                            rotation = (p.rotation + p.rotationSpeed) % 360f
                        )
                    }
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("confetti_canvas")
    ) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            // Wrap coordinate space of particles to matches layout size
            if (p.x < -50f) p.x = width + 50f
            if (p.x > width + 50f) p.x = -50f
            
            // Loop back to the top when reaching bottom
            if (p.y > height + 20) {
                p.y = -20f
                p.x = Random.nextFloat() * width
            }

            withTransform({
                translate(p.x, p.y)
                rotate(p.rotation, pivot = Offset(p.size / 2f, p.size / 2f))
            }) {
                if (p.isCircle) {
                    drawCircle(
                        color = p.color,
                        radius = p.size / 2f,
                        center = Offset(p.size / 2f, p.size / 2f)
                    )
                } else {
                    drawRect(
                        color = p.color,
                        size = Size(p.size, p.size * 0.6f)
                    )
                }
            }
        }
    }
}
