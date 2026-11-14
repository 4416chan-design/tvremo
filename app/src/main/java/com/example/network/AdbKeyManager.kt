package com.example.network

import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * Manages a persistent RSA-2048 ADB authentication keypair in app-private storage,
 * formats the 524-byte Android RSAPublicKey struct for AUTH_RSAPUBLICKEY,
 * and signs ADB SHA-1 challenge tokens using PKCS#1 v1.5 Type 1 padding.
 */
class AdbKeyManager(private val storageDir: File) {

    private val privateKeyFile = File(storageDir, "adb_rsa_private.der")
    private val publicKeyFile = File(storageDir, "adb_rsa_public.der")

    @Volatile
    private var cachedPrivateKey: PrivateKey? = null

    @Volatile
    private var cachedPublicKey: RSAPublicKey? = null

    @Synchronized
    fun getOrCreateKeyPair(): Pair<PrivateKey, RSAPublicKey> {
        cachedPrivateKey?.let { priv ->
            cachedPublicKey?.let { pub -> return priv to pub }
        }

        if (privateKeyFile.exists() && publicKeyFile.exists()) {
            val loaded = runCatching {
                val kf = KeyFactory.getInstance("RSA")
                val priv = kf.generatePrivate(PKCS8EncodedKeySpec(privateKeyFile.readBytes()))
                val pub = kf.generatePublic(X509EncodedKeySpec(publicKeyFile.readBytes())) as RSAPublicKey
                priv to pub
            }.getOrNull()
            if (loaded != null) {
                cachedPrivateKey = loaded.first
                cachedPublicKey = loaded.second
                return loaded
            }
        }

        val generator = KeyPairGenerator.getInstance("RSA").apply {
            initialize(KEY_SIZE_BITS)
        }
        val keyPair = generator.generateKeyPair()
        val priv = keyPair.private
        val pub = keyPair.public as RSAPublicKey

        storageDir.mkdirs()
        privateKeyFile.writeBytes(priv.encoded)
        publicKeyFile.writeBytes(pub.encoded)

        cachedPrivateKey = priv
        cachedPublicKey = pub
        return priv to pub
    }

    /**
     * Signs a 20-byte ADB AUTH_TOKEN challenge directly with PKCS#1 v1.5 SHA-1 DigestInfo padding
     * using raw RSA private exponent operation ("RSA/ECB/NoPadding").
     */
    fun signToken(token: ByteArray): ByteArray {
        val (privateKey, _) = getOrCreateKeyPair()
        val padded = buildPkcs1Sha1PaddedBlock(token)
        val cipher = Cipher.getInstance("RSA/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, privateKey)
        return cipher.doFinal(padded)
    }

    /**
     * Returns Base64(Android 524-byte RSAPublicKey struct) + " tvremote@android\0".
     */
    fun buildAdbPublicKeyPayload(): ByteArray {
        val (_, publicKey) = getOrCreateKeyPair()
        val structBytes = buildAndroidRsaPublicKeyStruct(publicKey)
        val b64 = encodeBase64(structBytes)
        return "$b64 tvremote@android\u0000".toByteArray(Charsets.US_ASCII)
    }

    companion object {
        private const val KEY_SIZE_BITS = 2048
        private const val MODULUS_WORDS = 64 // 2048 / 32
        private const val KEY_BYTES = 256    // 2048 / 8

        // ASN.1 DER DigestInfo prefix for SHA-1 (15 bytes)
        private val SHA1_DIGEST_INFO_PREFIX = byteArrayOf(
            0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e,
            0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14
        )

        fun buildPkcs1Sha1PaddedBlock(token: ByteArray): ByteArray {
            val digestLen = SHA1_DIGEST_INFO_PREFIX.size + token.size
            val paddingLen = KEY_BYTES - 3 - digestLen
            require(paddingLen >= 8) { "Token too large for RSA-2048 PKCS#1 padding" }

            val block = ByteArray(KEY_BYTES)
            block[0] = 0x00
            block[1] = 0x01
            for (i in 2 until 2 + paddingLen) {
                block[i] = 0xFF.toByte()
            }
            block[2 + paddingLen] = 0x00
            System.arraycopy(SHA1_DIGEST_INFO_PREFIX, 0, block, 3 + paddingLen, SHA1_DIGEST_INFO_PREFIX.size)
            System.arraycopy(token, 0, block, 3 + paddingLen + SHA1_DIGEST_INFO_PREFIX.size, token.size)
            return block
        }

        /**
         * Builds Android libmincrypt RSAPublicKey wire struct (exact 524 bytes, little-endian):
         * int len (64), uint32 n0inv, uint32 n[64], uint32 rr[64], int exponent.
         */
        fun buildAndroidRsaPublicKeyStruct(publicKey: RSAPublicKey): ByteArray {
            val n = publicKey.modulus
            val r32 = BigInteger.ONE.shiftLeft(32)
            val n0inv = r32.subtract(n.mod(r32).modInverse(r32)).toLong() and 0xFFFFFFFFL
            val r = BigInteger.ONE.shiftLeft(KEY_SIZE_BITS)
            val rr = r.multiply(r).mod(n)

            val buffer = ByteBuffer.allocate((2 + MODULUS_WORDS * 2 + 1) * 4)
                .order(ByteOrder.LITTLE_ENDIAN)

            buffer.putInt(MODULUS_WORDS)
            buffer.putInt(n0inv.toInt())

            writeLittleEndianWords(buffer, n, MODULUS_WORDS)
            writeLittleEndianWords(buffer, rr, MODULUS_WORDS)

            buffer.putInt(publicKey.publicExponent.toInt())
            return buffer.array()
        }

        private fun writeLittleEndianWords(buffer: ByteBuffer, value: BigInteger, wordCount: Int) {
            var remaining = value
            val mask32 = BigInteger.valueOf(0xFFFFFFFFL)
            repeat(wordCount) {
                buffer.putInt(remaining.and(mask32).toInt())
                remaining = remaining.shiftRight(32)
            }
        }

        private const val B64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

        fun encodeBase64(data: ByteArray): String {
            val out = StringBuilder(((data.size + 2) / 3) * 4)
            var i = 0
            while (i < data.size) {
                val b0 = data[i].toInt() and 0xFF
                val hasB1 = i + 1 < data.size
                val b1 = if (hasB1) data[i + 1].toInt() and 0xFF else 0
                val hasB2 = i + 2 < data.size
                val b2 = if (hasB2) data[i + 2].toInt() and 0xFF else 0

                val triple = (b0 shl 16) or (b1 shl 8) or b2
                out.append(B64_ALPHABET[(triple ushr 18) and 0x3F])
                out.append(B64_ALPHABET[(triple ushr 12) and 0x3F])
                out.append(if (hasB1) B64_ALPHABET[(triple ushr 6) and 0x3F] else '=')
                out.append(if (hasB2) B64_ALPHABET[triple and 0x3F] else '=')
                i += 3
            }
            return out.toString()
        }
    }
}
