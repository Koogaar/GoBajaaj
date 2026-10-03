package com.example.somalitraffic.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SomaliSkylineBottom(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x00F0F7FF),
                        Color(0x77E0F2FE),
                        Color(0xCCE0F2FE)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val skyBuildingColor = Color(0xFF93C5FD).copy(alpha = 0.35f)
            val frontBuildingColor = Color(0xFF60A5FA).copy(alpha = 0.45f)
            val flagBlue = Color(0xFF4189DD)
            val roadColor = Color(0xFF475569)
            val roadLineColor = Color(0xFFE2E8F0)

            // Background skyline buildings
            val bgPath = Path().apply {
                moveTo(0f, height)
                lineTo(0f, height * 0.7f)
                lineTo(width * 0.08f, height * 0.7f)
                lineTo(width * 0.08f, height * 0.55f)
                lineTo(width * 0.15f, height * 0.55f)
                lineTo(width * 0.15f, height * 0.75f)
                lineTo(width * 0.28f, height * 0.75f)
                lineTo(width * 0.28f, height * 0.48f)
                lineTo(width * 0.35f, height * 0.48f)
                lineTo(width * 0.35f, height * 0.7f)
                lineTo(width * 0.48f, height * 0.7f)
                lineTo(width * 0.48f, height * 0.35f)
                // Minaret Spire
                lineTo(width * 0.51f, height * 0.20f)
                lineTo(width * 0.54f, height * 0.35f)
                lineTo(width * 0.65f, height * 0.7f)
                lineTo(width * 0.72f, height * 0.52f)
                lineTo(width * 0.82f, height * 0.52f)
                lineTo(width * 0.82f, height * 0.68f)
                lineTo(width, height * 0.68f)
                lineTo(width, height)
                close()
            }
            drawPath(path = bgPath, color = skyBuildingColor)

            // Foreground skyline & mosque dome
            val fgPath = Path().apply {
                moveTo(0f, height)
                lineTo(0f, height * 0.78f)
                lineTo(width * 0.12f, height * 0.78f)
                lineTo(width * 0.12f, height * 0.62f)
                lineTo(width * 0.22f, height * 0.62f)
                lineTo(width * 0.22f, height * 0.80f)
                lineTo(width * 0.42f, height * 0.80f)
                // Dome
                cubicTo(
                    width * 0.42f, height * 0.58f,
                    width * 0.56f, height * 0.58f,
                    width * 0.56f, height * 0.80f
                )
                lineTo(width * 0.75f, height * 0.80f)
                lineTo(width * 0.75f, height * 0.64f)
                lineTo(width * 0.88f, height * 0.64f)
                lineTo(width * 0.88f, height * 0.78f)
                lineTo(width, height * 0.78f)
                lineTo(width, height)
                close()
            }
            drawPath(path = fgPath, color = frontBuildingColor)

            // Flag pole & Somali Flag on right building
            val poleX = width * 0.80f
            val poleTopY = height * 0.32f
            drawLine(
                color = Color(0xFF64748B),
                start = Offset(poleX, height * 0.64f),
                end = Offset(poleX, poleTopY),
                strokeWidth = 3f
            )
            val flagPath = Path().apply {
                moveTo(poleX, poleTopY)
                lineTo(poleX + 36f, poleTopY + 6f)
                lineTo(poleX + 36f, poleTopY + 26f)
                lineTo(poleX, poleTopY + 20f)
                close()
            }
            drawPath(path = flagPath, color = flagBlue)

            // Perspective Road stretching to bottom
            val roadPath = Path().apply {
                moveTo(width * 0.44f, height * 0.82f)
                lineTo(width * 0.56f, height * 0.82f)
                lineTo(width * 0.95f, height)
                lineTo(width * 0.05f, height)
                close()
            }
            drawPath(path = roadPath, color = roadColor)

            // Dashed center line for road
            val roadLinePath = Path().apply {
                moveTo(width * 0.50f, height * 0.83f)
                lineTo(width * 0.50f, height)
            }
            drawPath(
                path = roadLinePath,
                color = roadLineColor,
                style = Stroke(
                    width = 4f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
            )
        }
    }
}
