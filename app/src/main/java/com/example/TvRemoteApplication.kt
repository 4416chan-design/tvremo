package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.TvRemoteRepository
import com.example.network.AdbKeyManager
import com.example.network.ClientCertStore
import com.example.network.NetworkAdbClient
import com.example.network.TvDiscoveryManager
import com.example.network.TvTextSocketClient

class TvRemoteApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: TvRemoteRepository by lazy { TvRemoteRepository(database) }
    val certStore: ClientCertStore by lazy { ClientCertStore(this) }
    val discoveryManager: TvDiscoveryManager by lazy { TvDiscoveryManager(this) }
    val adbKeyManager: AdbKeyManager by lazy { AdbKeyManager(filesDir) }
    val adbClient: NetworkAdbClient by lazy { NetworkAdbClient(adbKeyManager) }
    val textSocketClient: TvTextSocketClient by lazy { TvTextSocketClient() }
}
