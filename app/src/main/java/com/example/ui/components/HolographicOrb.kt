package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.agent.AgentStatus
import com.example.ui.theme.HologramAmber
import com.example.ui.theme.HologramCrimson
import com.example.ui.theme.HologramCyan
import com.example.ui.theme.HologramEmerald
import com.example.ui.theme.HologramViolet
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var radius: Float,
    var speed: Float,
    var alpha: Float,
    var angle: Float
)

@Composable
fun HolographicOrb(
    status: AgentStatus,
    audioAmplitude: Float,
    baseColor: Color = HologramCyan,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_anim")

    // Continuous rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Reverse fast rotation for neural mesh
    val fastCounterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status == AgentStatus.THINKING) 2500 else 6000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    // Breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Holographic state-based accent color
    val stateColor = when (status) {
        AgentStatus.IDLE -> baseColor
        AgentStatus.LISTENING -> baseColor
        AgentStatus.THINKING -> HologramViolet
        AgentStatus.SPEAKING -> baseColor
        AgentStatus.CONFIRMING -> HologramAmber
        AgentStatus.EXECUTING -> HologramEmerald
        AgentStatus.ERROR -> HologramCrimson
    }

    // Seeded ambient particles
    val particles = remember {
        List(24) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 2.5f + 1f,
                speed = Random.nextFloat() * 0.02f + 0.005f,
                alpha = Random.nextFloat() * 0.6f + 0.2f,
                angle = Random.nextFloat() * 360f
            )
        }
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.65f

            // Dynamic amplitude expansion
            val dynamicScale = if (status == AgentStatus.LISTENING || status == AgentStatus.SPEAKING) {
                breathingPulse + (audioAmplitude * 0.45f)
            } else if (status == AgentStatus.THINKING) {
                breathingPulse * 1.05f
            } else {
                breathingPulse
            }

            val currentRadius = baseRadius * dynamicScale

            // 1. Outer Glow Aura (Luminous for dark glassmorphic canvas)
            val glowBrush = Brush.radialGradient(
                colors = listOf(
                    stateColor.copy(alpha = 0.35f),
                    stateColor.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = center,
                radius = currentRadius * 1.6f
            )
            drawCircle(brush = glowBrush, radius = currentRadius * 1.6f, center = center)

            // Frosted Glass Core Bubble (Refraction & Specular Highlights)
            val glassSphereBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    stateColor.copy(alpha = 0.18f),
                    Color(0x05FFFFFF),
                    Color.Transparent
                ),
                center = Offset(center.x - currentRadius * 0.32f, center.y - currentRadius * 0.32f),
                radius = currentRadius * 1.15f
            )
            drawCircle(brush = glassSphereBrush, radius = currentRadius * 0.98f, center = center)

            // Fine Glass Rim Outline
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        stateColor.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.12f)
                    ),
                    start = Offset(center.x - currentRadius, center.y - currentRadius),
                    end = Offset(center.x + currentRadius, center.y + currentRadius)
                ),
                radius = currentRadius * 0.98f,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // 2. Ambient Floating Geometric Particles
            for (p in particles) {
                val pDist = baseRadius * (0.8f + p.x * 0.8f)
                val pRad = Math.toRadians((p.angle + rotation * p.speed * 50f).toDouble())
                val px = center.x + (cos(pRad) * pDist).toFloat()
                val py = center.y + (sin(pRad) * pDist).toFloat()
                drawCircle(
                    color = stateColor.copy(alpha = p.alpha * 0.7f),
                    radius = p.radius,
                    center = Offset(px, py)
                )
            }

            // 3. Audio-Reactive Waveform Ring (when listening or speaking)
            if (status == AgentStatus.LISTENING || status == AgentStatus.SPEAKING) {
                val waveCount = 32
                for (i in 0 until waveCount) {
                    val angleDeg = (i * (360f / waveCount)) + rotation
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val waveAmp = if (i % 2 == 0) audioAmplitude * 28f else audioAmplitude * 12f
                    val r1 = currentRadius + 6f
                    val r2 = currentRadius + 14f + waveAmp

                    val startX = center.x + (cos(angleRad) * r1).toFloat()
                    val startY = center.y + (sin(angleRad) * r1).toFloat()
                    val endX = center.x + (cos(angleRad) * r2).toFloat()
                    val endY = center.y + (sin(angleRad) * r2).toFloat()

                    drawLine(
                        color = stateColor.copy(alpha = 0.85f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.5f
                    )
                }
            }

            // 4. Outer Segmented Orbital Ring
            rotate(rotation, pivot = center) {
                drawCircle(
                    color = stateColor.copy(alpha = 0.45f),
                    radius = currentRadius * 1.18f,
                    center = center,
                    style = Stroke(
                        width = 1.8f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 14f), 0f)
                    )
                )
            }

            // 5. Secondary Orbital Ring (opposite rotation)
            rotate(fastCounterRotation, pivot = center) {
                drawCircle(
                    color = stateColor.copy(alpha = 0.75f),
                    radius = currentRadius * 0.95f,
                    center = center,
                    style = Stroke(
                        width = 2.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(35f, 18f, 12f, 18f), 0f)
                    )
                )

                // 4 Orbital Satellite Nodes (High-contrast double ring)
                for (j in 0 until 4) {
                    val nodeAngle = Math.toRadians((j * 90.0))
                    val nx = center.x + (cos(nodeAngle) * (currentRadius * 0.95f)).toFloat()
                    val ny = center.y + (sin(nodeAngle) * (currentRadius * 0.95f)).toFloat()
                    drawCircle(
                        color = stateColor,
                        radius = 4f,
                        center = Offset(nx, ny)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2f,
                        center = Offset(nx, ny)
                    )
                }
            }

            // 6. Neural Mesh / Inner Core Polygon
            rotate(rotation * 1.5f, pivot = center) {
                val nodeCount = 6
                val polyPoints = mutableListOf<Offset>()
                for (k in 0 until nodeCount) {
                    val a = Math.toRadians((k * (360.0 / nodeCount)))
                    val pr = currentRadius * 0.52f
                    val px = center.x + (cos(a) * pr).toFloat()
                    val py = center.y + (sin(a) * pr).toFloat()
                    polyPoints.add(Offset(px, py))
                }

                // Connect mesh nodes
                for (p1 in polyPoints) {
                    for (p2 in polyPoints) {
                        drawLine(
                            color = stateColor.copy(alpha = 0.35f),
                            start = p1,
                            end = p2,
                            strokeWidth = 1.2f
                        )
                    }
                }
            }

            // 7. Central Intelligent Nucleus
            val nucleusBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    stateColor.copy(alpha = 0.85f),
                    stateColor.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = center,
                radius = currentRadius * 0.45f
            )
            drawCircle(brush = nucleusBrush, radius = currentRadius * 0.45f, center = center)

            // 8. Core White Hotspot
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = currentRadius * 0.12f,
                center = center
            )
        }
    }
}
