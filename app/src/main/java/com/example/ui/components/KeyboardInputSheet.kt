package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.ui.theme.MintGreen
import com.example.ui.theme.OnMintGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Native Android TV v2 remote text input sheet.
 * Transmits text directly to the TV's IME keyboard using the Android TV Remote v2 protocol
 * (RemoteImeBatchEdit, insert = 1, start/end cursor synchronization) based on kud/androidtv-remote.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardInputSheet(
    tvName: String,
    initialText: String = "",
    isTvImeVisible: Boolean = false,
    onSendText: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }

    var fieldValue by remember(initialText) {
        mutableStateOf(TextFieldValue(initialText, selection = TextRange(initialText.length)))
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun submitCurrentText() {
        val committed = fieldValue.text.trim()
        if (committed.isEmpty()) return
        fieldValue = fieldValue.copy(composition = null)
        onSendText(committed)
        fieldValue = TextFieldValue("", selection = TextRange(0))
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = fieldValue,
                onValueChange = { next ->
                    fieldValue = next
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
                    focusedBorderColor = MintGreen,
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
                        .clickable {
                            fieldValue = TextFieldValue("")
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

                val canSend = fieldValue.text.trim().isNotEmpty()
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "전송",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
