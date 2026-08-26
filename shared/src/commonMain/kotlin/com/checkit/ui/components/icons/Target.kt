package com.checkit.ui.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val AppIcons.Target: ImageVector
  get() {
    if (_target != null) {
      return _target!!
    }
    _target =
      ImageVector.Builder(
          name = "target",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero,
          ) {
            moveTo(8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.79f)
            reflectiveQuadToRelative(3.17f, 2.14f)
            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
            quadTo(22f, 9.92f, 22f, 12f)
            reflectiveQuadToRelative(-0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            close()
            moveToRelative(9.58f, -3.54f)
            quadTo(20f, 15.35f, 20f, 12f)
            reflectiveQuadTo(17.68f, 6.32f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadTo(6.33f, 6.32f)
            reflectiveQuadTo(4f, 12f)
            reflectiveQuadToRelative(2.33f, 5.68f)
            reflectiveQuadTo(12f, 20f)
            reflectiveQuadToRelative(5.68f, -2.32f)
            close()
            moveTo(7.75f, 16.25f)
            quadTo(6f, 14.5f, 6f, 12f)
            reflectiveQuadTo(7.75f, 7.75f)
            reflectiveQuadTo(12f, 6f)
            reflectiveQuadToRelative(4.25f, 1.75f)
            reflectiveQuadTo(18f, 12f)
            reflectiveQuadToRelative(-1.75f, 4.25f)
            reflectiveQuadTo(12f, 18f)
            reflectiveQuadTo(7.75f, 16.25f)
            close()
            moveToRelative(7.08f, -1.43f)
            quadTo(16f, 13.65f, 16f, 12f)
            reflectiveQuadTo(14.83f, 9.17f)
            reflectiveQuadTo(12f, 8f)
            reflectiveQuadTo(9.18f, 9.17f)
            reflectiveQuadTo(8f, 12f)
            reflectiveQuadToRelative(1.18f, 2.82f)
            reflectiveQuadTo(12f, 16f)
            reflectiveQuadToRelative(2.83f, -1.18f)
            close()
            moveTo(10.59f, 13.41f)
            quadTo(10f, 12.83f, 10f, 12f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(12f, 10f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 11.18f, 14f, 12f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(12f, 14f)
            reflectiveQuadTo(10.59f, 13.41f)
            close()
          }
        }
        .build()
    return _target!!
  }

private var _target: ImageVector? = null
