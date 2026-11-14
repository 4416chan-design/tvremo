package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.rememberHapticHelper
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.CharcoalWell
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenContainer
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Reference-matched Full Bullnose dial slider row:
 * - Left: standalone circular icon button (Mute toggle or Brightness 30%/60% toggle + long-press picture settings)
 * - Right: Full Bullnose capsule with horizontal progress fill, `-` button, centered value readout, `+` button,
 *   and smooth relative horizontal drag (+1/-1 steps, never jumping on tap).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullBullnoseDialSlider(
    icon: ImageVector,
    iconContentDescription: String,
    iconActive: Boolean,
    value: Int,
    minValue: Int,
    maxValue: Int,
    valueText: String,
    hapticEnabled: Boolean,
    height: Dp = 52.dp,
    accentColor: Color = MintGreen,
    dimmed: Boolean = false,
    iconTestTag: String,
    sliderTestTag: String,
    valueTestTag: String,
    onIconClick: () -> Unit,
    onIconLongClick: (() -> Unit)? = null,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticHelper = rememberHapticHelper()
    val currentOnStep by rememberUpdatedState(onStep)
    val currentHapticEnabled by rememberUpdatedState(hapticEnabled)
    val density = LocalDensity.current

    val range = (maxValue - minValue).coerceAtLeast(1)
    val targetFraction = ((value - minValue).toFloat() / range.toFloat()).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(stiffness = 700f, dampingRatio = 0.85f),
        label = "dial_slider_fraction"
    )

    val iconBg by animateColorAsState(
        targetValue = if (iconActive) MintGreenContainer else CharcoalSurface,
        label = "dial_icon_bg"
    )
    val iconBorderColor by animateColorAsState(
        targetValue = if (iconActive) accentColor.copy(alpha = 0.55f) else CharcoalBorder,
        label = "dial_icon_border"
    )
    val iconTint by animateColorAsState(
        targetValue = if (iconActive) accentColor else TextPrimary,
        label = "dial_icon_tint"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        // Standalone left circular icon button
        val iconInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(height)
                .clip(CircleShape)
                .background(iconBg)
                .border(1.dp, iconBorderColor, CircleShape)
                .combinedClickable(
                    interactionSource = iconInteraction,
                    indication = ripple(bounded = true, color = accentColor),
                    onClickLabel = iconContentDescription,
                    onClick = {
                        if (currentHapticEnabled) {
                            hapticHelper.vibrateClick()
                        }
                        onIconClick()
                    },
                    onLongClick = onIconLongClick?.let { longAction ->
                        {
                            if (currentHapticEnabled) {
                                hapticHelper.vibrateClick(durationMs = 35L, amplitude = 220)
                            }
                            longAction()
                        }
                    }
                )
                .testTag(iconTestTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Full Bullnose capsule track with - / centered value / + and smooth drag
        val stepThresholdPx = with(density) { 5.5.dp.toPx() }
        Surface(
            color = CharcoalWell,
            shape = CircleShape,
            modifier = Modifier
                .weight(1f)
                .height(height)
                .clip(CircleShape)
                .border(1.dp, CharcoalBorder, CircleShape)
                .pointerInput(stepThresholdPx) {
                    var dragAccumulator = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onDragEnd = { dragAccumulator = 0f },
                        onDragCancel = { dragAccumulator = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulator += dragAmount
                            while (dragAccumulator >= stepThresholdPx) {
                                dragAccumulator -= stepThresholdPx
                                if (currentHapticEnabled) {
                                    hapticHelper.vibrateClick(durationMs = 14L, amplitude = 150)
                                }
                                currentOnStep(1)
                            }
                            while (dragAccumulator <= -stepThresholdPx) {
                                dragAccumulator += stepThresholdPx
                                if (currentHapticEnabled) {
                                    hapticHelper.vibrateClick(durationMs = 14L, amplitude = 150)
                                }
                                currentOnStep(-1)
                            }
                        }
                    )
                }
                .testTag(sliderTestTag)
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val trackWidth = maxWidth
                val fillWidth = trackWidth * animatedFraction

                if (fillWidth > 0.dp) {
                    val fillBrush = if (dimmed) {
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF282A2E), Color(0xFF383B41))
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                CharcoalSurface,
                                Color(0xFF565A62)
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(fillWidth)
                            .fillMaxHeight()
                            .background(fillBrush)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(1.5.dp)
                                .fillMaxHeight()
                                .background(
                                    if (dimmed) TextSecondary.copy(alpha = 0.25f)
                                    else accentColor.copy(alpha = 0.55f)
                                )
                        )
                    }
                }

                // Minus button on left (hold-to-repeat)
                val minusInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(height)
                        .clip(CircleShape)
                        .holdToRepeatClickable(
                            interactionSource = minusInteraction,
                            indication = ripple(bounded = true, color = accentColor),
                            contentDescription = "감소",
                            onStep = {
                                if (currentHapticEnabled) {
                                    hapticHelper.vibrateClick(durationMs = 16L, amplitude = 160)
                                }
                                currentOnStep(-1)
                            },
                            onRepeatStep = {
                                if (currentHapticEnabled) {
                                    hapticHelper.vibrateClick(durationMs = 12L, amplitude = 140)
                                }
                                currentOnStep(-1)
                            }
                        )
                        .testTag("${sliderTestTag}_minus"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "1 감소",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Centered numeric readout
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = MonoFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (dimmed) TextSecondary else TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag(valueTestTag)
                )

                // Plus button on right (hold-to-repeat)
                val plusInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(height)
                        .clip(CircleShape)
                        .holdToRepeatClickable(
                            interactionSource = plusInteraction,
                            indication = ripple(bounded = true, color = accentColor),
                            contentDescription = "증가",
                            onStep = {
                                if (currentHapticEnabled) {
                                    hapticHelper.vibrateClick(durationMs = 16L, amplitude = 160)
                                }
                                currentOnStep(1)
                            },
                            onRepeatStep = {
                                if (currentHapticEnabled) {
                                    hapticHelper.vibrateClick(durationMs = 12L, amplitude = 140)
                                }
                                currentOnStep(1)
                            }
                        )
                        .testTag("${sliderTestTag}_plus"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "1 증가",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
