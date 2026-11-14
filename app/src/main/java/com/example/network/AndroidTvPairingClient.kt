package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket

/**
 * Handles Android TV Remote Protocol v2 pairing handshake on TCP port 6467 over mTLS.
 */
class AndroidTvPairingClient(private val certStore: ClientCertStore) {

    private var socket: SSLSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var serverCert: X509Certificate? = null

    suspend fun startPairing(host: String, pairingPort: Int, clientName: String = "TV 리모컨") =
        withContext(Dispatchers.IO) {
            close()
            var capturedCert: X509Certificate? = null
            val sslContext = certStore.createSslContext { cert -> capturedCert = cert }
            val sslSocket = (sslContext.socketFactory.createSocket() as SSLSocket).apply {
                soTimeout = 15_000
                connect(InetSocketAddress(host, pairingPort), 8_000)
                startHandshake()
            }
            socket = sslSocket
            input = sslSocket.inputStream
            output = sslSocket.outputStream
            serverCert = capturedCert ?: (sslSocket.session.peerCertificates.firstOrNull() as? X509Certificate)

            val out = output!!
            val inp = input!!

            // 1. PairingRequest (field 10)
            val requestMsg = ProtobufWire.encode {
                writeVarintField(1, 2) // protocol_version = 2
                writeVarintField(2, 200) // STATUS_OK
                writeMessageField(10) {
                    writeStringField(1, "com.aistudio.tvremote") // service_name
                    writeStringField(2, clientName) // client_name
                }
            }
            ProtobufWire.writeFramedMessage(out, requestMsg)
            assertStatusOk(ProtobufWire.readFramedMessage(inp), "페어링 요청 응답 실패")

            // 2. PairingOption (field 20)
            val optionMsg = ProtobufWire.encode {
                writeVarintField(1, 2)
                writeVarintField(2, 200)
                writeMessageField(20) {
                    writeMessageField(1) { // input_encodings
                        writeVarintField(1, 3) // ENCODING_TYPE_HEXADECIMAL
                        writeVarintField(2, 6) // symbol_length = 6
                    }
                    writeVarintField(3, 1) // preferred_role = ROLE_TYPE_INPUT
                }
            }
            ProtobufWire.writeFramedMessage(out, optionMsg)
            assertStatusOk(ProtobufWire.readFramedMessage(inp), "페어링 옵션 협상 실패")

            // 3. PairingConfiguration (field 30) -> triggers 6-char hex PIN on TV screen
            val configMsg = ProtobufWire.encode {
                writeVarintField(1, 2)
                writeVarintField(2, 200)
                writeMessageField(30) {
                    writeMessageField(1) { // encoding
                        writeVarintField(1, 3) // ENCODING_TYPE_HEXADECIMAL
                        writeVarintField(2, 6)
                    }
                    writeVarintField(2, 1) // client_role = ROLE_TYPE_INPUT
                }
            }
            ProtobufWire.writeFramedMessage(out, configMsg)
            assertStatusOk(ProtobufWire.readFramedMessage(inp), "TV PIN 화면 표시 실패")
        }

    suspend fun submitPin(pinHex: String) = withContext(Dispatchers.IO) {
        val cert = serverCert ?: throw IllegalStateException("TV 인증서 정보를 찾을 수 없습니다.")
        val out = output ?: throw IllegalStateException("페어링 연결이 끊어졌습니다.")
        val inp = input ?: throw IllegalStateException("페어링 연결이 끊어졌습니다.")

        val secretBytes = certStore.computePairingSecret(cert, pinHex)

        val secretMsg = ProtobufWire.encode {
            writeVarintField(1, 2)
            writeVarintField(2, 200)
            writeMessageField(40) { // pairing_secret
                writeBytesField(1, secretBytes)
            }
        }
        ProtobufWire.writeFramedMessage(out, secretMsg)
        assertStatusOk(ProtobufWire.readFramedMessage(inp), "PIN 인증에 실패했습니다. 코드를 다시 확인해 주세요.")
        close()
    }

    private fun assertStatusOk(frame: ByteArray, failurePrefix: String) {
        val fields = ProtobufWire.parseFields(frame)
        val status = fields.firstOrNull { it.number == 2 }?.varint?.toInt() ?: 0
        if (status != 200) {
            throw IllegalStateException("$failurePrefix (상태 코드: $status)")
        }
    }

    fun close() {
        runCatching { socket?.close() }
        socket = null
        input = null
        output = null
    }
}
