package com.example.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.example.data.model.DiscoveredTextServer
import com.example.data.model.DiscoveredTv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.Inet4Address

/**
 * Discovers Android TV / Google TV devices (_androidtvremote2._tcp.) and
 * TV TCP text socket servers (_tvtext._tcp.) on the same Wi-Fi network via mDNS.
 */
class TvDiscoveryManager(context: Context) {

    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var textServerDiscoveryListener: NsdManager.DiscoveryListener? = null

    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = _discoveredTvs.asStateFlow()

    private val _discoveredTextServers = MutableStateFlow<List<DiscoveredTextServer>>(emptyList())
    val discoveredTextServers: StateFlow<List<DiscoveredTextServer>> = _discoveredTextServers.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isScanningTextServers = MutableStateFlow(false)
    val isScanningTextServers: StateFlow<Boolean> = _isScanningTextServers.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private fun acquireMulticastLockIfNeeded() {
        if (multicastLock?.isHeld == true) return
        runCatching {
            val lock = wifiManager.createMulticastLock("tv_remote_mdns").apply {
                setReferenceCounted(false)
                acquire()
            }
            multicastLock = lock
        }
    }

    private fun releaseMulticastLockIfIdle() {
        if (discoveryListener != null || textServerDiscoveryListener != null) return
        runCatching {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        }
        multicastLock = null
    }

    fun startDiscovery() {
        stopRemoteDiscoveryOnly()
        _discoveredTvs.value = emptyList()
        _scanError.value = null
        acquireMulticastLockIfNeeded()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                _isScanning.value = true
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (!service.serviceType.contains("androidtvremote2")) return
                resolveService(service) { info -> addResolvedRemoteService(info) }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                _discoveredTvs.update { list ->
                    list.filterNot { it.serviceName == service.serviceName }
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                _isScanning.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isScanning.value = false
                _scanError.value = "Wi-Fi TV 검색을 시작할 수 없습니다 (코드 $errorCode)."
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isScanning.value = false
            }
        }

        discoveryListener = listener
        runCatching {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure { err ->
            discoveryListener = null
            _isScanning.value = false
            _scanError.value = err.message ?: "mDNS 검색 실패"
            releaseMulticastLockIfIdle()
        }

        startTextServerDiscovery()
    }

    fun startTextServerDiscovery() {
        stopTextServerDiscoveryOnly()
        _discoveredTextServers.value = emptyList()
        acquireMulticastLockIfNeeded()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                _isScanningTextServers.value = true
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (!service.serviceType.contains("tvtext")) return
                resolveService(service) { info -> addResolvedTextServer(info) }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                _discoveredTextServers.update { list ->
                    list.filterNot { it.serviceName == service.serviceName }
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                _isScanningTextServers.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isScanningTextServers.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isScanningTextServers.value = false
            }
        }

        textServerDiscoveryListener = listener
        runCatching {
            nsdManager.discoverServices(TEXT_SOCKET_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            textServerDiscoveryListener = null
            _isScanningTextServers.value = false
            releaseMulticastLockIfIdle()
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveService(serviceInfo: NsdServiceInfo, onResolved: (NsdServiceInfo) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching {
                nsdManager.resolveService(
                    serviceInfo,
                    appContext.mainExecutor,
                    object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            onResolved(info)
                        }
                    }
                )
            }
        } else {
            runCatching {
                nsdManager.resolveService(
                    serviceInfo,
                    object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            onResolved(info)
                        }
                    }
                )
            }
        }
    }

    private fun extractIpv4Address(info: NsdServiceInfo): String? {
        val ipv4 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            info.hostAddresses.firstOrNull { it is Inet4Address } ?: info.hostAddresses.firstOrNull()
        } else {
            @Suppress("DEPRECATION")
            info.host
        } ?: return null
        return ipv4.hostAddress
    }

    private fun addResolvedRemoteService(info: NsdServiceInfo) {
        val hostAddress = extractIpv4Address(info) ?: return
        val port = if (info.port > 0) info.port else 6466
        val tv = DiscoveredTv(
            serviceName = info.serviceName.ifBlank { hostAddress },
            host = hostAddress,
            port = port,
            pairingPort = port + 1
        )
        _discoveredTvs.update { current ->
            val filtered = current.filterNot { it.host == tv.host }
            filtered + tv
        }
    }

    private fun addResolvedTextServer(info: NsdServiceInfo) {
        val hostAddress = extractIpv4Address(info) ?: return
        val port = if (info.port > 0) info.port else TvTextSocketClient.DEFAULT_TEXT_PORT
        val server = DiscoveredTextServer(
            serviceName = info.serviceName.ifBlank { hostAddress },
            host = hostAddress,
            port = port
        )
        _discoveredTextServers.update { current ->
            val filtered = current.filterNot { it.host == server.host && it.port == server.port }
            filtered + server
        }
    }

    private fun stopRemoteDiscoveryOnly() {
        discoveryListener?.let { listener ->
            runCatching { nsdManager.stopServiceDiscovery(listener) }
        }
        discoveryListener = null
        _isScanning.value = false
    }

    private fun stopTextServerDiscoveryOnly() {
        textServerDiscoveryListener?.let { listener ->
            runCatching { nsdManager.stopServiceDiscovery(listener) }
        }
        textServerDiscoveryListener = null
        _isScanningTextServers.value = false
    }

    fun stopTextServerDiscovery() {
        stopTextServerDiscoveryOnly()
        releaseMulticastLockIfIdle()
    }

    fun stopDiscovery() {
        stopRemoteDiscoveryOnly()
        stopTextServerDiscoveryOnly()
        releaseMulticastLockIfIdle()
    }

    companion object {
        const val SERVICE_TYPE = "_androidtvremote2._tcp."
        const val TEXT_SOCKET_SERVICE_TYPE = "_tvtext._tcp."
    }
}
