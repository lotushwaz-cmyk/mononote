package com.mononote.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** 시안과 같은 얇은 선 아이콘 (24x24 기준 SVG 경로) */
object MonoIcons {
    private fun stroke(name: String, vararg paths: String, width: Float = 1.5f): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach {
            b.addPath(
                pathData = addPathNodes(it),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val Calendar by lazy {
        stroke(
            "calendar",
            "M6 5h12a3 3 0 0 1 3 3v10a3 3 0 0 1 -3 3H6a3 3 0 0 1 -3 -3V8a3 3 0 0 1 3 -3z",
            "M3 10h18", "M8 3v4", "M16 3v4",
        )
    }
    val Diary by lazy { stroke("diary", "M5 4h11a3 3 0 0 1 3 3v13H8a3 3 0 0 1 -3 -3z", "M9 9h6", "M9 13h4") }
    val Archive by lazy {
        stroke(
            "archive",
            "M4.5 4h15a1.5 1.5 0 0 1 1.5 1.5v2a1.5 1.5 0 0 1 -1.5 1.5h-15a1.5 1.5 0 0 1 -1.5 -1.5v-2a1.5 1.5 0 0 1 1.5 -1.5z",
            "M5 9v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1 -1V9", "M10 13h4",
        )
    }
    val Habit by lazy {
        stroke("habit", "M21 12a9 9 0 1 1 -18 0a9 9 0 1 1 18 0z", "M8 12.5l3 3 5 -6")
    }
    val Money by lazy {
        stroke(
            "money",
            "M5.5 6h13a2.5 2.5 0 0 1 2.5 2.5v8a2.5 2.5 0 0 1 -2.5 2.5h-13a2.5 2.5 0 0 1 -2.5 -2.5v-8a2.5 2.5 0 0 1 2.5 -2.5z",
            "M3 10h18", "M16 14.5h2",
        )
    }
    val Check by lazy { stroke("check", "M5 12.5l4.5 4.5L19 7.5", width = 2.2f) }
    val Plus by lazy { stroke("plus", "M12 5v14", "M5 12h14", width = 1.6f) }
    val Back by lazy { stroke("back", "M15 6l-6 6 6 6") }
    val Forward by lazy { stroke("forward", "M9 6l6 6 -6 6") }
    val Search by lazy { stroke("search", "M18 11a7 7 0 1 1 -14 0a7 7 0 1 1 14 0z", "M20 20l-3.5 -3.5") }
    val Settings by lazy {
        stroke(
            "settings",
            "M4 7h9", "M17 7h3", "M4 17h3", "M11 17h9",
            "M17 7a2 2 0 1 1 -4 0a2 2 0 1 1 4 0z",
            "M11 17a2 2 0 1 1 -4 0a2 2 0 1 1 4 0z",
        )
    }
    val Image by lazy {
        stroke(
            "image",
            "M6 4h12a2 2 0 0 1 2 2v12a2 2 0 0 1 -2 2H6a2 2 0 0 1 -2 -2V6a2 2 0 0 1 2 -2z",
            "M4 16l5 -5 4 4 2 -2 5 5",
        )
    }
    val Star by lazy {
        ImageVector.Builder("star", 24.dp, 24.dp, 24f, 24f).addPath(
            pathData = addPathNodes("M12 3l2.7 5.6 6.1 0.9 -4.4 4.3 1 6.1L12 17l-5.4 2.9 1 -6.1L3.2 9.5l6.1 -0.9z"),
            fill = SolidColor(Color.Black),
        ).build()
    }
}
