package com.example.ui.components

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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.CharcoalWell
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenContainer
import com.example.ui.theme.OnMintGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Native Android TV v2 remote text input sheet.
 * Synchronizes focused TV input text, allows full-field overwrite,
 * and waits a configurable delay before transmitting ENTER so the TV
 * input method has time to commit the text without prematurely closing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KeyboardInputSheet(
    tvName: String,
    initialText: String = "",
    isTvImeVisible: Boolean = false,
    savedEnterDelayMs: Long = 1500L,
    onSendText: (text: String, submitWithEnter: Boolean, enterDelayMs: Long) -> Unit,
    onSendEnterOnly: () -> Unit,
    onRefreshTvText: () -> Unit,
    onSaveEnterDelay: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }

    var fieldValue by remember(initialText) {
        mutableStateOf(TextFieldValue(initialText, selection = TextRange(initialText.length)))
    }

    var selectedDelayMs by remember(savedEnterDelayMs) {
        mutableLongStateOf(savedEnterDelayMs)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun submitText(withEnter: Boolean) {
        val committed = fieldValue.text.trim()
        if (committed.isEmpty()) return
        fieldValue = fieldValue.copy(composition = null)
        onSendText(committed, withEnter, selectedDelayMs)
        onDismiss()
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
            // Header: Keyboard Icon + Title & TV Status | Close 'X'
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Keyboard,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "키보드 입력",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    val statusText = if (isTvImeVisible) {
                        "TV 입력창 감지됨 · 한글/영문 지원"
                    } else {
                        "$tvName · TV 검색창을 열면 자동 연결됩니다"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isTvImeVisible) MintGreen else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

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

            Spacer(modifier = Modifier.height(14.dp))

            // TV Current Focused Input Synchronizer Banner
            Surface(
                color = CharcoalSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tv,
                        contentDescription = null,
                        tint = if (initialText.isNotBlank()) MintGreen else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TV 포커스된 입력창 내용",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        val displayTvText = initialText.ifBlank { "(비어있음)" }
                        Text(
                            text = displayTvText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (initialText.isNotBlank()) TextPrimary else TextSecondary.copy(alpha = 0.6f),
                            fontWeight = if (initialText.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (initialText.isNotBlank()) {
                        Surface(
                            color = MintGreenContainer,
                            shape = CircleShape,
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(1.dp, MintGreen.copy(alpha = 0.6f), CircleShape)
                                .clickable {
                                    fieldValue = TextFieldValue(initialText, selection = TextRange(initialText.length))
                                }
                                .testTag("keyboard_copy_tv_text_button")
                        ) {
                            Text(
                                text = "가져오기",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MintGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CharcoalWell)
                            .border(1.dp, CharcoalBorder, CircleShape)
                            .clickable(onClick = onRefreshTvText)
                            .testTag("keyboard_refresh_tv_text_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "TV 텍스트 새로고침",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mobile Editable Text Field
            OutlinedTextField(
                value = fieldValue,
                onValueChange = { next ->
                    fieldValue = next
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
                    .focusRequester(focusRequester)
                    .testTag("tv_keyboard_input_field"),
                placeholder = {
                    Text(
                        text = "$tvName 에 덮어쓸 텍스트 입력 (한글·영문)",
                        color = TextSecondary
                    )
                },
                maxLines = 4,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CharcoalWell,
                    unfocusedContainerColor = CharcoalWell,
                    focusedBorderColor = MintGreen,
                    unfocusedBorderColor = CharcoalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = MintGreen
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = if (selectedDelayMs > 0) ImeAction.Send else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onSend = { submitText(withEnter = selectedDelayMs > 0) },
                    onDone = { submitText(withEnter = false) }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Enter Delay Selector (Prevents TV IME from closing before text is committed)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "전송 후 엔터키 대기 시간",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (selectedDelayMs > 0) "${selectedDelayMs / 1000.0}초 후 엔터" else "엔터 안 함",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedDelayMs > 0) MintGreen else TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                val delayOptions = listOf(
                    1000L to "1.0초",
                    1500L to "1.5초 (권장)",
                    2000L to "2.0초",
                    3000L to "3.0초",
                    0L to "엔터 없음"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    delayOptions.forEach { (delayMs, label) ->
                        val isSelected = selectedDelayMs == delayMs
                        Surface(
                            color = if (isSelected) MintGreenContainer else CharcoalSurface,
                            shape = CircleShape,
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    if (isSelected) MintGreen.copy(alpha = 0.6f) else DividerSubtle,
                                    CircleShape
                                )
                                .clickable {
                                    selectedDelayMs = delayMs
                                    onSaveEnterDelay(delayMs)
                                }
                                .testTag("delay_option_${delayMs}")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MintGreen else TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Buttons: 전체 지우기 | 텍스트만 덮어쓰기 | TV 엔터
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = CharcoalSurface,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .border(1.dp, CharcoalBorder, CircleShape)
                        .clickable { fieldValue = TextFieldValue("") }
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
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "지우기",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }

                val canSend = fieldValue.text.trim().isNotEmpty()
                Surface(
                    color = CharcoalSurface,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .border(1.dp, if (canSend) MintGreen.copy(alpha = 0.4f) else CharcoalBorder, CircleShape)
                        .clickable(enabled = canSend) { submitText(withEnter = false) }
                        .testTag("keyboard_send_text_only_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "텍스트만 덮어쓰기",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (canSend) TextPrimary else TextSecondary.copy(alpha = 0.4f)
                        )
                    }
                }

                Surface(
                    color = CharcoalSurface,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .border(1.dp, CharcoalBorder, CircleShape)
                        .clickable(onClick = {
                            onSendEnterOnly()
                            onDismiss()
                        })
                        .testTag("keyboard_send_enter_only_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardReturn,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TV 엔터",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Full Bullnose Pill Button: 전송 및 엔터
            val canSendPrimary = fieldValue.text.trim().isNotEmpty()
            val primaryLabel = if (selectedDelayMs > 0) {
                "전송 및 엔터 (${selectedDelayMs / 1000.0}초 후)"
            } else {
                "전송 (엔터 없음)"
            }

            Button(
                onClick = { submitText(withEnter = selectedDelayMs > 0) },
                enabled = canSendPrimary,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canSendPrimary) MintGreen else CharcoalSurfaceElevated,
                    contentColor = if (canSendPrimary) OnMintGreen else TextPrimary,
                    disabledContainerColor = CharcoalSurfaceElevated,
                    disabledContentColor = TextSecondary
                ),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("keyboard_send_search_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = primaryLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
