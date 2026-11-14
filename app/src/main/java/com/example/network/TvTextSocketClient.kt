package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Pure Kotlin TCP socket text transmitter for Android TV text input servers.
 * Sends UTF-8 single-line text terminated by `\n` so a TV-side `BufferedReader.readLine()`
 * receives Korean/Unicode strings intact and triggers an Enter/Search action.
 */
class TvTextSocketClient {

    suspend fun sendText(
        host: String,
        port: Int = DEFAULT_TEXT_PORT,
        text: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanHost = host.trim()
        if (cleanHost.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("TV IP 주소를 먼저 입력하거나 TV를 연결해 주세요.")
            )
        }
        if (!isValidPort(port)) {
            return@withContext Result.failure(
                IllegalArgumentException("올바른 포트 번호(1~65535)를 입력해 주세요: $port")
            )
        }

        val sanitized = sanitizeLineText(text)
        if (sanitized.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("전송할 텍스트를 입력해 주세요.")
            )
        }

        try {
            Socket().use { socket ->
                socket.tcpNoDelay = true
                socket.soTimeout = SOCKET_TIMEOUT_MS
                socket.connect(InetSocketAddress(cleanHost, port), CONNECT_TIMEOUT_MS)

                val writer = BufferedWriter(
                    OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)
                )
                writer.write(sanitized)
                writer.write("\n")
                writer.flush()
            }
            Result.success(sanitized)
        } catch (_: UnknownHostException) {
            Result.failure(IllegalStateException("TV IP 주소를 찾을 수 없습니다: $cleanHost"))
        } catch (_: ConnectException) {
            Result.failure(
                IllegalStateException("TV 소켓 서버($cleanHost:$port)에 연결할 수 없습니다. TV 수신 앱을 확인해 주세요.")
            )
        } catch (_: SocketTimeoutException) {
            Result.failure(
                IllegalStateException("TV 연결 시간 초과($cleanHost:$port). 같은 Wi-Fi 및 포트를 확인해 주세요.")
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("TV 연결 실패: ${e.message ?: e.javaClass.simpleName}")
            )
        }
    }

    companion object {
        const val DEFAULT_TEXT_PORT = 8080
        const val CONNECT_TIMEOUT_MS = 2_500
        const val SOCKET_TIMEOUT_MS = 3_000

        fun isValidPort(port: Int): Boolean = port in 1..65535

        fun sanitizeLineText(text: String): String {
            return text
                .replace("\r\n", " ")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim()
        }
    }
}
