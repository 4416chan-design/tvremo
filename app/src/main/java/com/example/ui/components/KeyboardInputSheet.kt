package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DiscoveredTextServer
import com.example.data.model.TextSendState
import com.example.network.TvTextSocketClient
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.CharcoalWell
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenContainer
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.OnMintGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

/**
 * UTF-8 TCP socket text input sheet for Android TV.
 * Automatically binds to the connected TV's IP address (or NSD / manual override)
 * and transmits single-line UTF-8 text terminated with `\n` so the TV triggers Enter.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KeyboardInputSheet(
    tvName: String,
    connectedTvHost: String?,
    targetHost: String?,
    targetPort: Int,
    hostOverride: String,
    discoveredTextServers: List<DiscoveredTextServer>,
    isScanningTextServers: Boolean,
    textSendState: TextSendState,
    onSendText: (String) -> Unit,
    onClearStatus: () -> Unit,
    onUpdateTcpTarget: (hostOverride: String, port: Int) -> Unit,
    onRefreshTextServers: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }

    var fieldValue by remember {
        mutableStateOf(TextFieldValue("", selection = TextRange(0)))
    }
    var showTargetEditor by remember { mutableStateOf(false) }
    var draftHost by remember(hostOverride) { mutableStateOf(hostOverride) }
    var draftPort by remember(targetPort) { mutableStateOf(targetPort.toString()) }
    var targetInputError by remember { mutableStateOf<String?>(null) }

    val isSending = textSendState is TextSendState.Sending

    LaunchedEffect(textSendState) {
        if (textSendState is TextSendState.Success) {
            fieldValue = TextFieldValue("", selection = TextRange(0))
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun submitCurrentText() {
        if (isSending) return
        val committed = fieldValue.text.trim()
        if (committed.isEmpty()) return
        fieldValue = fieldValue.copy(composition = null)
        onSendText(committed)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = CharcoalBg,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header: Keyboard Icon + Title & TCP Target | Port/Target Pill | Close 'X'
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Keyboard,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "키보드 입력",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    val targetSourceLabel = when {
                        hostOverride.isNotBlank() -> "직접 지정"
                        !connectedTvHost.isNullOrBlank() -> "연결된 TV 자동"
                        discoveredTextServers.isNotEmpty() -> "NSD 자동 탐색"
                        else -> null
                    }
                    val statusText = if (!targetHost.isNullOrBlank()) {
                        "TCP · $targetHost:$targetPort (${targetSourceLabel ?: "자동"})"
                    } else {
                        "TV를 연결하거나 대상 IP를 설정하세요"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = if (!targetHost.isNullOrBlank()) MonoFontFamily else MaterialTheme.typography.labelSmall.fontFamily,
                        color = if (!targetHost.isNullOrBlank()) MintGreen else WarningAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = if (showTargetEditor) MintGreenContainer else CharcoalSurface,
                    shape = CircleShape,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(CircleShape)
                        .border(
                            1.dp,
                            if (showTargetEditor) MintGreen.copy(alpha = 0.6f) else CharcoalBorder,
                            CircleShape
                        )
                        .clickable { showTargetEditor = !showTargetEditor }
                        .testTag("keyboard_tcp_target_toggle")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "TCP 대상 설정",
                            tint = if (showTargetEditor) MintGreen else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = ":$targetPort",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = MonoFontFamily,
                            color = if (showTargetEditor) MintGreen else TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CharcoalSurface)
                        .border(1.dp, CharcoalBorder, CircleShape)
                        .clickable(onClick = onDismiss)
                        .testTag("keyboard_close_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "닫기",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Collapsible TCP Target IP, Port & NSD Server Selector
            AnimatedVisibility(visible = showTargetEditor) {
                Surface(
                    color = CharcoalSurface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "TCP 소켓 전송 대상",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = CharcoalWell,
                                shape = CircleShape,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(1.dp, CharcoalBorder, CircleShape)
                                    .clickable(onClick = onRefreshTextServers)
                                    .testTag("refresh_nsd_text_servers_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "NSD 다시 검색",
                                        tint = if (isScanningTextServers) MintGreen else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isScanningTextServers) "NSD 검색중" else "NSD 검색",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isScanningTextServers) MintGreen else TextSecondary
                                    )
                                }
                            }
                        }

                        if (!connectedTvHost.isNullOrBlank() || discoveredTextServers.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!connectedTvHost.isNullOrBlank()) {
                                    val isUsingConnected = hostOverride.isBlank()
                                    Surface(
                                        color = if (isUsingConnected) MintGreenContainer else CharcoalWell,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .border(
                                                1.dp,
                                                if (isUsingConnected) MintGreen.copy(alpha = 0.6f) else CharcoalBorder,
                                                CircleShape
                                            )
                                            .clickable {
                                                draftHost = ""
                                                val parsedPort = draftPort.toIntOrNull()
                                                    ?.takeIf { TvTextSocketClient.isValidPort(it) }
                                                    ?: targetPort
                                                targetInputError = null
                                                onUpdateTcpTarget("", parsedPort)
                                            }
                                    ) {
                                        Text(
                                            text = "연결된 TV ($connectedTvHost)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = MonoFontFamily,
                                            color = if (isUsingConnected) MintGreen else TextPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                discoveredTextServers.forEach { server ->
                                    val selected = targetHost == server.host && targetPort == server.port
                                    Surface(
                                        color = if (selected) MintGreenContainer else CharcoalWell,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .border(
                                                1.dp,
                                                if (selected) MintGreen.copy(alpha = 0.6f) else CharcoalBorder,
                                                CircleShape
                                            )
                                            .clickable {
                                                val overrideValue = if (server.host == connectedTvHost) "" else server.host
                                                draftHost = overrideValue
                                                draftPort = server.port.toString()
                                                targetInputError = null
                                                onUpdateTcpTarget(overrideValue, server.port)
                                            }
                                    ) {
                                        Text(
                                            text = "${server.serviceName} (${server.host}:${server.port})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = MonoFontFamily,
                                            color = if (selected) MintGreen else TextPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = draftHost,
                                onValueChange = {
                                    draftHost = it
                                    targetInputError = null
                                },
                                label = { Text("TV IP (비우면 자동)") },
                                placeholder = {
                                    Text(
                                        text = connectedTvHost ?: "192.168.0.15",
                                        fontFamily = MonoFontFamily
                                    )
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = CharcoalWell,
                                    unfocusedContainerColor = CharcoalWell,
                                    focusedBorderColor = MintGreen,
                                    unfocusedBorderColor = CharcoalBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("tcp_override_host_input")
                            )

                            OutlinedTextField(
                                value = draftPort,
                                onValueChange = { raw ->
                                    if (raw.length <= 5 && raw.all { it.isDigit() }) {
                                        draftPort = raw
                                        targetInputError = null
                                    }
                                },
                                label = { Text("포트") },
                                placeholder = { Text("8080", fontFamily = MonoFontFamily) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = CharcoalWell,
                                    unfocusedContainerColor = CharcoalWell,
                                    focusedBorderColor = MintGreen,
                                    unfocusedBorderColor = CharcoalBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("tcp_port_input")
                            )
                        }

                        if (!targetInputError.isNullOrBlank()) {
                            Text(
                                text = targetInputError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = ErrorCoral
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val parsedPort = draftPort.toIntOrNull()
                                    if (parsedPort == null || !TvTextSocketClient.isValidPort(parsedPort)) {
                                        targetInputError = "포트는 1~65535 사이 숫자로 입력해 주세요."
                                    } else {
                                        targetInputError = null
                                        onUpdateTcpTarget(draftHost.trim(), parsedPort)
                                        showTargetEditor = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MintGreen,
                                    contentColor = OnMintGreen
                                ),
                                shape = CircleShape,
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("tcp_target_apply_button")
                            ) {
                                Text("적용", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = fieldValue,
                onValueChange = { next ->
                    fieldValue = next
                    if (textSendState !is TextSendState.Idle) {
                        onClearStatus()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .focusRequester(focusRequester)
                    .testTag("tv_keyboard_input_field"),
                placeholder = {
                    Text(
                        text = "$tvName 에 전송할 한글·영문 텍스트를 입력하세요...",
                        color = TextSecondary
                    )
                },
                maxLines = 5,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CharcoalWell,
                    unfocusedContainerColor = CharcoalWell,
                    focusedBorderColor = CharcoalBorder,
                    unfocusedBorderColor = CharcoalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = MintGreen
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { submitCurrentText() }
                )
            )

            // Inline send feedback banner
            when (textSendState) {
                is TextSendState.Sending -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MintGreen,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${textSendState.targetHost}:${textSendState.targetPort} 으로 전송 중...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                is TextSendState.Success -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MintGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "전송 완료 · \"${textSendState.sentText}\" + 엔터",
                            style = MaterialTheme.typography.bodySmall,
                            color = MintGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                is TextSendState.Error -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = textSendState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorCoral
                        )
                    }
                }
                TextSendState.Idle -> Unit
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Full Bullnose Pill Buttons: 전체 지우기 | 전송
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = CharcoalSurface,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(CircleShape)
                        .border(1.dp, CharcoalBorder, CircleShape)
                        .clickable(enabled = !isSending) {
                            fieldValue = TextFieldValue("")
                            onClearStatus()
                        }
                        .testTag("keyboard_clear_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Backspace,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "전체 지우기",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                    }
                }

                val canSend = fieldValue.text.trim().isNotEmpty() && !isSending
                Button(
                    onClick = { submitCurrentText() },
                    enabled = canSend,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canSend) MintGreen else CharcoalSurfaceElevated,
                        contentColor = if (canSend) OnMintGreen else TextPrimary,
                        disabledContainerColor = CharcoalSurfaceElevated,
                        disabledContentColor = TextSecondary
                    ),
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("keyboard_send_search_button")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = MintGreen,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSending) "전송 중" else "전송",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
