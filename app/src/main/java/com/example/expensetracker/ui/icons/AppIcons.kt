package com.example.expensetracker.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Власні іконки: `material-icons-extended` важить десятки МБ, а потрібна лише кругова діаграма.
 * Геометрія (viewport 24×24): сектор 270° + «висунута» чверть; заповнена й контурна версії.
 */
object AppIcons {
    private const val MAIN_SLICE = "M12,12 L12,3 A9,9 0 1 0 21,12 Z"
    private const val DETACHED_SLICE = "M13.5,10.5 L13.5,1.5 A9,9 0 0 1 22.5,10.5 Z"

    val PieChartFilled: ImageVector by lazy {
        ImageVector.Builder("PieChartFilled", 24.dp, 24.dp, 24f, 24f).apply {
            addPath(PathParser().parsePathString(MAIN_SLICE).toNodes(), fill = SolidColor(Color.Black))
            addPath(PathParser().parsePathString(DETACHED_SLICE).toNodes(), fill = SolidColor(Color.Black))
        }.build()
    }

    val PieChartOutlined: ImageVector by lazy {
        ImageVector.Builder("PieChartOutlined", 24.dp, 24.dp, 24f, 24f).apply {
            for (slice in listOf(MAIN_SLICE, DETACHED_SLICE)) {
                addPath(
                    PathParser().parsePathString(slice).toNodes(),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
    }
}
