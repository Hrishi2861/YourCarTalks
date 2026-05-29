package com.hrishi.yourcartalks.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.painterResource
import com.hrishi.yourcartalks.R

@Composable
fun CarSilhouetteIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Icon(
        painter = painterResource(R.drawable.ic_car),
        contentDescription = null,
        modifier = modifier,
        tint = color
    )
}

@Composable
fun KeyIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Icon(
        painter = painterResource(R.drawable.ic_contact),
        contentDescription = null,
        modifier = modifier,
        tint = color
    )
}

@Composable
fun PersonIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Icon(
        painter = painterResource(R.drawable.ic_person),
        contentDescription = null,
        modifier = modifier,
        tint = color
    )
}

@Composable
fun PencilIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF757575)
) {
    Icon(
        painter = painterResource(R.drawable.ic_edit),
        contentDescription = null,
        modifier = modifier,
        tint = color
    )
}

@Composable
fun DownloadIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeWidth: Float = 2f
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.15f), end = Offset(w * 0.5f, h * 0.7f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        val arrowPath = Path().apply {
            moveTo(w * 0.25f, h * 0.5f)
            lineTo(w * 0.5f, h * 0.8f)
            lineTo(w * 0.75f, h * 0.5f)
        }
        drawPath(arrowPath, color = color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color = color, start = Offset(w * 0.1f, h * 0.85f), end = Offset(w * 0.9f, h * 0.85f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun ChevronLeftIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeWidth: Float = 2f
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.65f, h * 0.25f)
            lineTo(w * 0.3f, h * 0.5f)
            lineTo(w * 0.65f, h * 0.75f)
        }
        drawPath(path, color = color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ChevronRightIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeWidth: Float = 2f
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.25f)
            lineTo(w * 0.7f, h * 0.5f)
            lineTo(w * 0.35f, h * 0.75f)
        }
        drawPath(path, color = color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SunIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeWidth: Float = 1.5f
) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.width * 0.15f
        drawCircle(color = color, radius = r, center = Offset(cx, cy), style = Stroke(strokeWidth))
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val sx = cx + (r + size.width * 0.05f) * Math.cos(angle).toFloat()
            val sy = cy + (r + size.width * 0.05f) * Math.sin(angle).toFloat()
            val ex = cx + (r + size.width * 0.14f) * Math.cos(angle).toFloat()
            val ey = cy + (r + size.width * 0.14f) * Math.sin(angle).toFloat()
            drawLine(color = color, start = Offset(sx, sy), end = Offset(ex, ey), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun MoonIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeWidth: Float = 1.5f
) {
    Canvas(modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val r = size.width * 0.25f
        drawCircle(color = color, radius = r, center = Offset(cx, cy), style = Stroke(strokeWidth))
        drawCircle(
            color = color,
            radius = r * 0.75f,
            center = Offset(cx + r * 0.35f, cy - r * 0.15f),
            style = Stroke(strokeWidth * 2.5f)
        )
    }
}

@Composable
fun SystemIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeWidth: Float = 1.5f
) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.width * 0.25f
        drawCircle(color = color, radius = r, center = Offset(cx, cy), style = Stroke(strokeWidth))
        val clipPath = Path().apply {
            moveTo(cx - r, cy - r)
            lineTo(cx + r, cy - r)
            lineTo(cx + r, cy + r)
            close()
        }
        withTransform({
            clipPath(clipPath)
        }) {
            drawCircle(color = color, radius = r * 0.5f, center = Offset(cx + r * 0.15f, cy - r * 0.15f))
        }
    }
}

@Composable
fun ShieldIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4CAF50),
    strokeWidth: Float = 1.5f
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            lineTo(w * 0.9f, h * 0.2f)
            lineTo(w * 0.85f, h * 0.65f)
            cubicTo(w * 0.75f, h * 0.85f, w * 0.6f, h * 0.95f, w * 0.5f, h * 0.95f)
            cubicTo(w * 0.4f, h * 0.95f, w * 0.25f, h * 0.85f, w * 0.15f, h * 0.65f)
            lineTo(w * 0.1f, h * 0.2f)
            close()
        }
        drawPath(path, color = color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val checkPath = Path().apply {
            moveTo(w * 0.35f, h * 0.5f)
            lineTo(w * 0.45f, h * 0.65f)
            lineTo(w * 0.65f, h * 0.35f)
        }
        drawPath(checkPath, color = color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun RadioIndicator(
    modifier: Modifier = Modifier,
    selected: Boolean,
    selectedColor: Color = Color(0xFFE8A020),
    unselectedColor: Color = Color(0xFF757575),
    strokeWidth: Float = 1.5f
) {
    Canvas(modifier) {
        val r = size.width / 2f * 0.8f
        if (selected) {
            drawCircle(color = selectedColor, radius = r, style = Stroke(strokeWidth))
            drawCircle(color = Color.White, radius = r * 0.35f)
        } else {
            drawCircle(color = unselectedColor, radius = r, style = Stroke(strokeWidth))
        }
    }
}

@Composable
fun ActiveDot(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4CAF50)
) {
    Canvas(modifier) {
        val r = size.width / 2f * 0.8f
        drawCircle(color = color, radius = r)
    }
}

@Composable
fun PageDotIndicator(
    active: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFE8A020),
    inactiveColor: Color = Color(0xFF2C2C2C)
) {
    Canvas(modifier) {
        val r = size.width / 2f
        drawCircle(color = if (active) activeColor else inactiveColor, radius = r)
    }
}
