package com.fiveis.xend.ui.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom icon for the template action (stylized "T").
 */
val TemplateTIcon: ImageVector = ImageVector.Builder(
    name = "TemplateTIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = SolidColor(Color.White),
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(4f, 5f)
        lineTo(20f, 5f)
        lineTo(20f, 8.5f)
        lineTo(13.5f, 8.5f)
        lineTo(13.5f, 19f)
        lineTo(10.5f, 19f)
        lineTo(10.5f, 8.5f)
        lineTo(4f, 8.5f)
        close()
    }
}.build()
