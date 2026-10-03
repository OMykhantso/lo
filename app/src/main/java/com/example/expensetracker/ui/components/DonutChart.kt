package com.example.expensetracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme

/** Сегмент діаграми: [value] — вага (будь-яка невід’ємна величина), [label] — для опису доступності. */
data class ChartSlice(val value: Float, val color: Color, val label: String)

/**
 * Кільцева діаграма (Lab 4, AI-завдання): малюється на `Canvas` дугами, при появі та зміні даних
 * плавно «розгортається» від 0 до повного кола. Усередині кільця — довільний [centerContent].
 *
 * @param gapDegrees просвіт між сегментами (не застосовується, якщо сегмент один)
 */
@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 28.dp,
    gapDegrees: Float = 2f,
    centerContent: @Composable () -> Unit = {},
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 800, easing = FastOutSlowInEasing))
    }

    val positive = slices.filter { it.value > 0f }
    val total = positive.sumOf { it.value.toDouble() }.toFloat()
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val description = chartDescription(positive, total)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
            val arcSize = Size(size.width, size.height)
            drawArc(
                color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset.Zero, size = arcSize, style = stroke,
            )
            if (total > 0f) {
                val gap = if (positive.size > 1) gapDegrees else 0f
                var start = -90f
                for (slice in positive) {
                    val full = slice.value / total * 360f
                    val sweep = (full - gap).coerceAtLeast(MIN_SWEEP) * progress.value
                    drawArc(
                        color = slice.color, startAngle = start + gap / 2f, sweepAngle = sweep, useCenter = false,
                        topLeft = Offset.Zero, size = arcSize, style = stroke,
                    )
                    start += full
                }
            }
        }
        centerContent()
    }
}

private const val MIN_SWEEP = 0.5f

internal fun chartDescription(slices: List<ChartSlice>, total: Float): String =
    if (slices.isEmpty() || total <= 0f) {
        "Діаграма витрат: даних немає"
    } else {
        slices.joinToString(prefix = "Діаграма витрат: ", separator = ", ") {
            "${it.label} ${Math.round(it.value / total * 100)}%"
        }
    }

@Preview(showBackground = true)
@Composable
private fun DonutChartPreview() {
    ExpenseTrackerTheme {
        DonutChart(
            slices = listOf(
                ChartSlice(50f, Color(0xFFC24E00), "Їжа"),
                ChartSlice(30f, Color(0xFF1B6EC2), "Транспорт"),
                ChartSlice(20f, Color(0xFF7A4FC9), "Житло"),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
