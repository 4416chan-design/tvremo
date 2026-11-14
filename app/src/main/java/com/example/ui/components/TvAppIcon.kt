package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Input
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TvAppBadgeIcon(
    iconKey: String,
    accentHex: Long,
    appName: String,
    size: Dp = 24.dp
) {
    val bgColor = Color(accentHex).copy(alpha = 0.20f)
    val fgColor = Color(accentHex)

    val monogram = when (iconKey) {
        "netflix" -> "N"
        "tving" -> "T"
        "wavve" -> "W"
        "coupang" -> "C"
        "watcha" -> "W"
        "disney" -> "D+"
        "prime" -> "P"
        "appletv" -> "tv"
        "kodi" -> "K"
        "plex" -> ">"
        else -> null
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (monogram != null) {
            Text(
                text = monogram,
                color = fgColor,
                fontSize = (size.value * 0.44f).sp,
                fontWeight = FontWeight.ExtraBold
            )
        } else {
            val vec = when (iconKey) {
                "youtube" -> Icons.Rounded.PlayArrow
                "spotify" -> Icons.Rounded.MusicNote
                "twitch" -> Icons.Rounded.VideoLibrary
                "googletv" -> Icons.Rounded.Tv
                else -> customActionIcon(iconKey)
            }
            Icon(
                imageVector = vec,
                contentDescription = appName,
                tint = fgColor,
                modifier = Modifier.size(size * 0.62f)
            )
        }
    }
}

fun customActionIcon(iconKey: String): ImageVector = when (iconKey) {
    "input" -> Icons.AutoMirrored.Rounded.Input
    "captions" -> Icons.Rounded.ClosedCaption
    "channel_up" -> Icons.Rounded.LiveTv
    "channel_down" -> Icons.Rounded.LiveTv
    "info" -> Icons.Rounded.Info
    "menu" -> Icons.Rounded.Menu
    "guide" -> Icons.Rounded.Movie
    "tv_settings" -> Icons.Rounded.Settings
    "next" -> Icons.Rounded.SkipNext
    "prev" -> Icons.Rounded.SkipPrevious
    "stop" -> Icons.Rounded.Stop
    "sleep" -> Icons.Rounded.NightsStay
    "youtube", "netflix", "disney", "tving", "wavve", "coupang", "watcha", "spotify", "prime", "appletv", "twitch", "plex", "kodi", "googletv" -> Icons.Rounded.LiveTv
    else -> Icons.Rounded.Apps
}
