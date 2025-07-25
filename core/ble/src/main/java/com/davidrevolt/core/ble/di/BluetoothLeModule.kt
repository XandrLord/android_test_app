package com.davidrevolt.core.ble.di


import android.content.Context
import com.davidrevolt.core.ble.BluetoothLeService
import com.davidrevolt.core.ble.BluetoothLeServiceImpl
import com.davidrevolt.core.ble.manger.BluetoothLeConnect
import com.davidrevolt.core.ble.manger.BluetoothLeScan
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BluetoothLeServiceModule {
    @Binds
    abstract fun bindsBluetoothLe(bluetoothLeServiceImpl: BluetoothLeServiceImpl): BluetoothLeService

    companion object {
        @Provides
        @Singleton
        fun provideBluetoothLeScanService(@ApplicationContext context: Context): BluetoothLeScan =
            BluetoothLeScan(context)

        @Provides
        @Singleton
        fun provideBluetoothLeConnectService(@ApplicationContext context: Context): BluetoothLeConnect =
            BluetoothLeConnect(context)
    }
}