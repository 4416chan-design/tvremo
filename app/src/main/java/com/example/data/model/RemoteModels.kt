package com.example.data.model

data class DiscoveredTv(
    val serviceName: String,
    val host: String,
    val port: Int = 6466,
    val pairingPort: Int = 6467,
    val isSaved: Boolean = false
)

sealed class TvConnectionState {
    data object Disconnected : TvConnectionState()
    data class Connecting(val tvName: String, val host: String) : TvConnectionState()
    data class PairingRequired(
        val tvName: String,
        val host: String,
        val pairingPort: Int,
        val remotePort: Int,
        val errorMessage: String? = null
    ) : TvConnectionState()
    data class Connected(
        val tvName: String,
        val host: String,
        val remotePort: Int,
        val currentAppPackage: String? = null,
        val currentVolume: Int? = null,
        val maxVolume: Int? = null,
        val isMuted: Boolean = false
    ) : TvConnectionState()
    data class Error(val message: String, val host: String? = null) : TvConnectionState()
}

data class TvImePromptState(
    val isTvImeVisible: Boolean = false,
    val currentText: String = "",
    val fieldCounter: Int = 0,
    val imeCounter: Int = 0
)
