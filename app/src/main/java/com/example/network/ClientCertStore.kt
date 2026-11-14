package com.example.network

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.net.ssl.SSLContext
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager
import java.net.Socket
import java.security.Principal
import javax.net.ssl.SSLEngine

/**
 * Manages the RSA-2048 client certificate used for mTLS with Android TV
 * and computes SHA-256 pairing verification secrets.
 */
class ClientCertStore(context: Context) {

    private val keyFile = File(context.filesDir, "tv_remote_client.pk8")
    private val certFile = File(context.filesDir, "tv_remote_client.der")

    @Volatile
    private var cachedPrivateKey: PrivateKey? = null

    @Volatile
    private var cachedCert: X509Certificate? = null

    @Synchronized
    fun getOrCreateCredentials(): Pair<PrivateKey, X509Certificate> {
        cachedPrivateKey?.let { pk ->
            cachedCert?.let { cert -> return pk to cert }
        }

        if (keyFile.exists() && certFile.exists()) {
            runCatching {
                val keySpec = PKCS8EncodedKeySpec(keyFile.readBytes())
                val pk = KeyFactory.getInstance("RSA").generatePrivate(keySpec)
                val cert = CertificateFactory.getInstance("X.509")
                    .generateCertificate(certFile.inputStream()) as X509Certificate
                cachedPrivateKey = pk
                cachedCert = cert
                return pk to cert
            }
        }

        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        val keyPair = kpg.generateKeyPair()
        val certDer = buildSelfSignedX509Der(keyPair)
        val cert = CertificateFactory.getInstance("X.509")
            .generateCertificate(certDer.inputStream()) as X509Certificate

        keyFile.writeBytes(keyPair.private.encoded)
        certFile.writeBytes(cert.encoded)
        cachedPrivateKey = keyPair.private
        cachedCert = cert
        return keyPair.private to cert
    }

    fun createSslContext(onServerCertCaptured: (X509Certificate) -> Unit = {}): SSLContext {
        val (privateKey, clientCert) = getOrCreateCredentials()

        val keyManager = object : X509ExtendedKeyManager() {
            override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> =
                arrayOf("tv_remote_client")

            override fun chooseClientAlias(
                keyType: Array<out String>?,
                issuers: Array<out Principal>?,
                socket: Socket?
            ): String = "tv_remote_client"

            override fun chooseEngineClientAlias(
                keyType: Array<out String>?,
                issuers: Array<out Principal>?,
                engine: SSLEngine?
            ): String = "tv_remote_client"

            override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = null

            override fun chooseServerAlias(
                keyType: String?,
                issuers: Array<out Principal>?,
                socket: Socket?
            ): String? = null

            override fun getCertificateChain(alias: String?): Array<X509Certificate> =
                arrayOf(clientCert)

            override fun getPrivateKey(alias: String?): PrivateKey = privateKey
        }

        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                chain?.firstOrNull()?.let(onServerCertCaptured)
            }
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        return SSLContext.getInstance("TLSv1.2").apply {
            init(arrayOf(keyManager), arrayOf(trustManager), SecureRandom())
        }
    }

    /**
     * Computes Android TV Remote v2 pairing verification secret:
     * SHA256(Nc || Ec || Ns || Es || nonceBytes)
     * and validates the 1-byte checksum prefix of the 6-char hex PIN.
     */
    fun computePairingSecret(serverCert: X509Certificate, pinHex: String): ByteArray {
        val cleaned = pinHex.trim().uppercase(Locale.US)
        require(cleaned.length == 6 && cleaned.all { it in "0123456789ABCDEF" }) {
            "TV 화면에 표시된 6자리 16진수 PIN을 정확히 입력해 주세요."
        }
        val pinBytes = cleaned.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val nonce = pinBytes.copyOfRange(1, pinBytes.size)

        val (_, clientCert) = getOrCreateCredentials()
        val clientPub = clientCert.publicKey as RSAPublicKey
        val serverPub = serverCert.publicKey as RSAPublicKey

        val digest = MessageDigest.getInstance("SHA-256").apply {
            update(unsignedBytes(clientPub.modulus))
            update(unsignedBytes(clientPub.publicExponent))
            update(unsignedBytes(serverPub.modulus))
            update(unsignedBytes(serverPub.publicExponent))
            update(nonce)
        }.digest()

        require(digest[0] == pinBytes[0]) {
            "PIN 코드가 일치하지 않습니다. TV 화면의 코드를 다시 확인해 주세요."
        }
        return digest
    }

    private fun unsignedBytes(value: BigInteger): ByteArray {
        val raw = value.toByteArray()
        return if (raw.size > 1 && raw[0] == 0.toByte()) {
            raw.copyOfRange(1, raw.size)
        } else {
            raw
        }
    }

    // Minimal ASN.1 DER builder for a self-signed X.509 v3 RSA-2048 certificate
    private fun buildSelfSignedX509Der(keyPair: KeyPair): ByteArray {
        val now = Date(System.currentTimeMillis() - 86_400_000L)
        val expiry = Date(System.currentTimeMillis() + 3650L * 86_400_000L)

        val serial = derInteger(BigInteger(64, SecureRandom()).abs().add(BigInteger.ONE).toByteArray())
        // SHA256withRSA OID 1.2.840.113549.1.1.11
        val sigAlgId = derSequence(
            derOid(byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x0B)),
            byteArrayOf(0x05, 0x00)
        )
        // CN=androidtvremote
        val cnAttr = derSequence(
            derOid(byteArrayOf(0x55, 0x04, 0x03)),
            derUtf8String("androidtvremote")
        )
        val name = derSequence(derSet(cnAttr))
        val validity = derSequence(derUtcTime(now), derUtcTime(expiry))
        val version = derExplicit(0, derInteger(byteArrayOf(0x02))) // v3 = 2

        val tbsCert = derSequence(
            version,
            serial,
            sigAlgId,
            name,
            validity,
            name,
            keyPair.public.encoded
        )

        val sig = Signature.getInstance("SHA256withRSA").apply {
            initSign(keyPair.private)
            update(tbsCert)
        }.sign()

        val sigBitString = derBitString(sig)
        return derSequence(tbsCert, sigAlgId, sigBitString)
    }

    private fun derTag(tag: Int, content: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(tag)
        val len = content.size
        when {
            len < 128 -> out.write(len)
            len <= 0xFF -> {
                out.write(0x81)
                out.write(len)
            }
            len <= 0xFFFF -> {
                out.write(0x82)
                out.write((len ushr 8) and 0xFF)
                out.write(len and 0xFF)
            }
            else -> {
                out.write(0x83)
                out.write((len ushr 16) and 0xFF)
                out.write((len ushr 8) and 0xFF)
                out.write(len and 0xFF)
            }
        }
        out.write(content)
        return out.toByteArray()
    }

    private fun derSequence(vararg items: ByteArray): ByteArray {
        val body = ByteArrayOutputStream()
        items.forEach { body.write(it) }
        return derTag(0x30, body.toByteArray())
    }

    private fun derSet(vararg items: ByteArray): ByteArray {
        val body = ByteArrayOutputStream()
        items.forEach { body.write(it) }
        return derTag(0x31, body.toByteArray())
    }

    private fun derExplicit(tagIndex: Int, content: ByteArray): ByteArray =
        derTag(0xA0 or tagIndex, content)

    private fun derInteger(bytes: ByteArray): ByteArray = derTag(0x02, bytes)
    private fun derOid(oidBytes: ByteArray): ByteArray = derTag(0x06, oidBytes)
    private fun derUtf8String(text: String): ByteArray = derTag(0x0C, text.toByteArray(Charsets.UTF_8))

    private fun derBitString(bytes: ByteArray): ByteArray {
        val body = ByteArray(bytes.size + 1)
        body[0] = 0x00
        System.arraycopy(bytes, 0, body, 1, bytes.size)
        return derTag(0x03, body)
    }

    private fun derUtcTime(date: Date): ByteArray {
        val fmt = SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return derTag(0x17, fmt.format(date).toByteArray(Charsets.US_ASCII))
    }
}
