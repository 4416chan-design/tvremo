package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AppPreferencesEntity
import com.example.data.local.FavoriteAppEntity
import com.example.data.local.PairedTvEntity
import com.example.data.local.RecentTvAppEntity
import com.example.data.model.TvAppCatalog
import com.example.data.model.TvAppCatalogItem
import com.example.ui.components.TvAppBadgeIcon
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalWell
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenContainer
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.OnMintGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    favoriteApps: List<FavoriteAppEntity>,
    recentApps: List<RecentTvAppEntity>,
    pairedTvs: List<PairedTvEntity>,
    preferences: AppPreferencesEntity,
    onNavigateBack: () -> Unit,
    onToggleCatalogFavorite: (TvAppCatalogItem, Boolean) -> Unit,
    onAddFavoriteFromRecent: (RecentTvAppEntity) -> Unit,
    onAddCustomFavorite: (name: String, uri: String, pkg: String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onMoveFavorite: (Int, Int) -> Unit,
    onConnectSavedTv: (PairedTvEntity) -> Unit,
    onForgetTv: (String) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleAutoKeyboard: (Boolean) -> Unit,
    onSetImeEnterDelayMs: (Long) -> Unit = {}
) {
    var showAddCustomAppDialog by remember { mutableStateOf(false) }

    val favoritePackages = remember(favoriteApps) { favoriteApps.map { it.packageName }.toSet() }
    val timeFormatter = remember { SimpleDateFormat("MM/dd HH:mm", Locale.KOREA) }

    Scaffold(
        containerColor = CharcoalBg,
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CharcoalSurface)
                        .border(1.dp, CharcoalBorder, CircleShape)
                        .clickable(onClick = onNavigateBack)
                        .testTag("settings_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Pinned Top Favorites Order Management
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader(
                                title = "상단 즐겨찾기 칩 바",
                                subtitle = "리모컨 상단에 고정 표시되는 앱 목록 (${favoriteApps.size}개)"
                            )
                        }
                        Surface(
                            color = MintGreenContainer,
                            shape = CircleShape,
                            modifier = Modifier
                                .height(40.dp)
                                .clip(CircleShape)
                                .border(1.dp, MintGreen.copy(alpha = 0.45f), CircleShape)
                                .clickable { showAddCustomAppDialog = true }
                                .testTag("settings_add_custom_app_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = MintGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("직접 추가", style = MaterialTheme.typography.labelMedium, color = MintGreen)
                            }
                        }
                    }
                }

                if (favoriteApps.isEmpty()) {
                    item {
                        Surface(
                            color = CharcoalSurface,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(18.dp))
                        ) {
                            Text(
                                text = "고정된 즐겨찾기가 없습니다. 아래 최근 실행 앱 또는 TV 앱 카탈로그에서 추가하세요.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(favoriteApps.size, key = { idx -> "fav_${favoriteApps[idx].packageName}" }) { idx ->
                        val fav = favoriteApps[idx]
                        Surface(
                            color = CharcoalSurface,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CharcoalBorder, CircleShape)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                            ) {
                                TvAppBadgeIcon(
                                    iconKey = fav.iconKey,
                                    accentHex = fav.accentHex,
                                    appName = fav.appName,
                                    size = 34.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = fav.appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onMoveFavorite(idx, -1) },
                                    enabled = idx > 0
                                ) {
                                    Icon(
                                        Icons.Rounded.ArrowUpward,
                                        contentDescription = "위로 이동",
                                        tint = if (idx > 0) TextPrimary else TextSecondary.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onMoveFavorite(idx, 1) },
                                    enabled = idx < favoriteApps.lastIndex
                                ) {
                                    Icon(
                                        Icons.Rounded.ArrowDownward,
                                        contentDescription = "아래로 이동",
                                        tint = if (idx < favoriteApps.lastIndex) TextPrimary else TextSecondary.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { onRemoveFavorite(fav.packageName) }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "즐겨찾기 제거",
                                        tint = ErrorCoral,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Recent Executed / TV-Reported Apps
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        title = "Android TV 최근 실행 앱",
                        subtitle = "TV 연결 중 감지되었거나 리모컨으로 실행한 최근 앱 기록"
                    )
                }

                if (recentApps.isEmpty()) {
                    item {
                        Surface(
                            color = CharcoalSurface,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(18.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Rounded.History, contentDescription = null, tint = TextSecondary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "아직 기록된 최근 실행 앱이 없습니다. TV에서 앱을 열거나 아래 카탈로그에서 즐겨찾기를 추가해 보세요.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(recentApps, key = { "recent_${it.packageName}" }) { recent ->
                        val isFav = favoritePackages.contains(recent.packageName)
                        Surface(
                            color = CharcoalSurface,
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CharcoalBorder, CircleShape)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                TvAppBadgeIcon(
                                    iconKey = recent.iconKey,
                                    accentHex = recent.accentHex,
                                    appName = recent.appName,
                                    size = 36.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = recent.appName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${if (recent.source == "TV") "TV 감지" else "실행"} · ${timeFormatter.format(Date(recent.lastUsedAt))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                FavoriteTogglePill(
                                    isFavorite = isFav,
                                    onClick = {
                                        if (isFav) onRemoveFavorite(recent.packageName)
                                        else onAddFavoriteFromRecent(recent)
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. Curated Android TV App Catalog
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        title = "주요 Android TV 앱 카탈로그",
                        subtitle = "아이콘 + 텍스트 칩으로 상단에 바로 추가할 수 있습니다"
                    )
                }

                items(TvAppCatalog.apps, key = { "cat_${it.packageName}" }) { app ->
                    val isFav = favoritePackages.contains(app.packageName)
                    Surface(
                        color = CharcoalSurface,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CharcoalBorder, CircleShape)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            TvAppBadgeIcon(
                                iconKey = app.iconKey,
                                accentHex = app.accentHex,
                                appName = app.appName,
                                size = 36.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = app.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            FavoriteTogglePill(
                                isFavorite = isFav,
                                onClick = { onToggleCatalogFavorite(app, isFav) }
                            )
                        }
                    }
                }

                // 4. Saved Paired TVs & Remote Options
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        title = "저장된 TV 및 리모컨 환경설정",
                        subtitle = "페어링된 기기와 입력 편의 옵션을 관리합니다"
                    )
                }

                items(pairedTvs, key = { "tv_${it.host}" }) { tv ->
                    Surface(
                        color = CharcoalSurface,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CharcoalBorder, CircleShape)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CharcoalWell),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Tv, contentDescription = null, tint = MintGreen, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = tv.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(
                                    text = tv.host,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = MonoFontFamily,
                                    color = TextSecondary
                                )
                            }
                            TextButton(onClick = { onConnectSavedTv(tv) }) {
                                Text("연결", color = MintGreen)
                            }
                            IconButton(onClick = { onForgetTv(tv.host) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "TV 삭제", tint = ErrorCoral)
                            }
                        }
                    }
                }

                item {
                    Surface(
                        color = CharcoalSurface,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("버튼 진동(햅틱) 피드백", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text("방향키 및 리모컨 버튼 터치 시 미세 진동", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Switch(
                                    checked = preferences.hapticEnabled,
                                    onCheckedChange = onToggleHaptic,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = OnMintGreen,
                                        checkedTrackColor = MintGreen
                                    )
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("TV 입력창 자동 키보드 열기", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text("TV에서 검색·입력창이 감지되면 키보드 입력 시트 자동 표시", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Switch(
                                    checked = preferences.autoShowKeyboard,
                                    onCheckedChange = onToggleAutoKeyboard,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = OnMintGreen,
                                        checkedTrackColor = MintGreen
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("키보드 전송 후 엔터 대기 시간", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text("TV 화면에 텍스트가 덮어써진 후 엔터를 실행하기까지 대기합니다", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                    Text(
                                        text = if (preferences.imeEnterDelayMs > 0) "${preferences.imeEnterDelayMs / 1000.0}초" else "엔터 없음",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MintGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                val delayOptions = listOf(
                                    1000L to "1.0초",
                                    1500L to "1.5초 (권장)",
                                    2000L to "2.0초",
                                    3000L to "3.0초",
                                    0L to "엔터 없음"
                                )
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    delayOptions.forEach { (delayMs, label) ->
                                        val isSelected = preferences.imeEnterDelayMs == delayMs
                                        Surface(
                                            color = if (isSelected) MintGreenContainer else CharcoalWell,
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) MintGreen.copy(alpha = 0.6f) else CharcoalBorder,
                                                    CircleShape
                                                )
                                                .clickable { onSetImeEnterDelayMs(delayMs) }
                                                .testTag("settings_delay_${delayMs}")
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) MintGreen else TextSecondary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


            }
        }
    }

    if (showAddCustomAppDialog) {
        AddCustomAppDialog(
            onConfirm = { name, uri, pkg ->
                onAddCustomFavorite(name, uri, pkg)
                showAddCustomAppDialog = false
            },
            onDismiss = { showAddCustomAppDialog = false }
        )
    }
}



@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun FavoriteTogglePill(
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isFavorite) MintGreen else CharcoalWell,
        shape = CircleShape,
        modifier = Modifier
            .height(38.dp)
            .clip(CircleShape)
            .border(1.dp, if (isFavorite) MintGreen else DividerSubtle, CircleShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Check else Icons.Rounded.Add,
                contentDescription = null,
                tint = if (isFavorite) OnMintGreen else TextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isFavorite) "추가됨" else "추가",
                style = MaterialTheme.typography.labelMedium,
                color = if (isFavorite) OnMintGreen else TextPrimary
            )
        }
    }
}

@Composable
private fun AddCustomAppDialog(
    onConfirm: (name: String, uri: String, pkg: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var uri by remember { mutableStateOf("") }
    var pkg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalSurface,
        title = { Text("사용자 앱 즐겨찾기 추가", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("앱 이름 (예: 티빙)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pkg,
                    onValueChange = { pkg = it },
                    label = { Text("패키지명 (예: net.cj.cjhv.gs.tving)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uri,
                    onValueChange = { uri = it },
                    label = { Text("실행 URI (비워두면 앱 바로 실행)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val effectiveUri = uri.trim().ifBlank {
                        if (pkg.isNotBlank()) "market://details?id=${pkg.trim()}" else ""
                    }
                    onConfirm(name, effectiveUri, pkg)
                },
                enabled = name.isNotBlank() && (uri.isNotBlank() || pkg.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = MintGreen, contentColor = OnMintGreen)
            ) {
                Text("추가", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = TextSecondary)
            }
        }
    )
}
