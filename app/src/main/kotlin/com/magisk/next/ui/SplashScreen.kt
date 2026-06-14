package com.magisk.next.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Брендовый сплеш NextMod IDE v2.
 * N рисует себя штрих за штрихом (геометрия 1:1 с иконкой приложения),
 * за буквой пульсирует свечение, надпись всплывает с мигающим
 * терминальным курсором после «IDE».
 *
 * Подключение в MainActivity — последним блоком внутри темы:
 *     var showSplash by remember { mutableStateOf(true) }
 *     if (showSplash) {
 *         BrandSplashScreen(onFinished = { showSplash = false })
 *     }
 */
@Composable
fun BrandSplashScreen(
    onFinished: () -> Unit,
    durationMillis: Long = 1400L
) {
    var dismissing by remember { mutableStateOf(false) }

    // [*] Прогресс отрисовки N: 0..1, три штриха разбирают диапазон между собой
    val drawProgress = remember { Animatable(0f) }
    // [*] Появление текста
    var textVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        drawProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
        )
        textVisible = true
        delay(durationMillis - 950 - 350)
        dismissing = true
        delay(350)
        onFinished()
    }

    // Уход: растворение + лёгкий наезд масштаба
    val screenAlpha by animateFloatAsState(
        targetValue = if (dismissing) 0f else 1f,
        animationSpec = tween(350), label = "screenAlpha"
    )
    val screenScale by animateFloatAsState(
        targetValue = if (dismissing) 1.06f else 1f,
        animationSpec = tween(350), label = "screenScale"
    )

    // Появление текста: fade + всплытие
    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(450), label = "textAlpha"
    )
    val textOffset by animateFloatAsState(
        targetValue = if (textVisible) 0f else 14f,
        animationSpec = tween(450, easing = FastOutSlowInEasing), label = "textOffset"
    )

    // Бесконечные эффекты: пульс свечения + мигание курсора
    val infinite = rememberInfiniteTransition(label = "splashFx")
    val glowPulse by infinite.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "glow"
    )
    val cursorAlpha by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                this.durationMillis = 4000
                1f at 0 using LinearEasing
                1f at 480
                0f at 500
                0f at 980
            }
        ),
        label = "cursor"
    )

    val brandStart = Color(0xFF4D9FFF)
    val brandEnd = Color(0xFF19E3D2)
    val strokeWhite = Color(0xFFE8EDF4)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .scale(screenScale)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF17222F), Color(0xFF0A0E14)),
                    radius = 1300f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // ── Логотип: N рисует себя ─────────────────────────────────────
            Canvas(modifier = Modifier.size(190.dp)) {
                val s = size.minDimension / 108f          // масштаб из координат иконки
                fun p(x: Float, y: Float) = Offset(x * s, y * s)
                val stroke = 9.5f * s
                val pr = drawProgress.value

                // Фазы штрихов: левая нога 0–0.35, диагональ 0.25–0.7, правая 0.6–1.0
                val leg1 = ((pr) / 0.35f).coerceIn(0f, 1f)
                val diag = ((pr - 0.25f) / 0.45f).coerceIn(0f, 1f)
                val leg2 = ((pr - 0.60f) / 0.40f).coerceIn(0f, 1f)

                // Пульсирующее свечение за буквой
                if (pr > 0.2f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                brandStart.copy(alpha = 0.16f * glowPulse * pr),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension * 0.52f,
                        center = p(54f, 54f)
                    )
                }

                // Левая нога: снизу вверх (73 → 35)
                if (leg1 > 0f) drawLine(
                    color = strokeWhite,
                    start = p(41f, 73f),
                    end = p(41f, 73f - 38f * leg1),
                    strokeWidth = stroke, cap = StrokeCap.Round
                )
                // Диагональ: сверху вниз, градиент бренда
                if (diag > 0f) drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(brandStart, brandEnd),
                        start = p(41f, 35f), end = p(67f, 73f)
                    ),
                    start = p(41f, 35f),
                    end = p(41f + 26f * diag, 35f + 38f * diag),
                    strokeWidth = stroke, cap = StrokeCap.Round
                )
                // Правая нога: снизу вверх (73 → 35)
                if (leg2 > 0f) drawLine(
                    color = strokeWhite,
                    start = p(67f, 73f),
                    end = p(67f, 73f - 38f * leg2),
                    strokeWidth = stroke, cap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Надпись с терминальным курсором ────────────────────────────
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = strokeWhite)) { append("NextMod ") }
                    withStyle(
                        SpanStyle(
                            brush = Brush.linearGradient(listOf(brandStart, brandEnd))
                        )
                    ) { append("IDE") }
                    withStyle(
                        SpanStyle(color = brandEnd.copy(alpha = cursorAlpha))
                    ) { append("_") }
                },
                style = TextStyle(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                modifier = Modifier
                    .alpha(textAlpha)
                    .offset(y = textOffset.dp)
            )
        }
    }
}
