// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SettingsStoreTest {

    @Test
    fun externalScannerEnabledDefaultsFalseWhenNoValueWasSaved() = runTest {
        val store = SettingsStore(testDataStore())

        assertFalse(store.externalScannerEnabled().first())
    }

    @Test
    fun setExternalScannerEnabledPersistsTrue() = runTest {
        val store = SettingsStore(testDataStore())

        store.setExternalScannerEnabled(true)

        assertTrue(store.externalScannerEnabled().first())
    }

    @Test
    fun setExternalScannerEnabledPersistsFalseAfterTrue() = runTest {
        val store = SettingsStore(testDataStore())

        store.setExternalScannerEnabled(true)
        store.setExternalScannerEnabled(false)

        assertFalse(store.externalScannerEnabled().first())
    }

    @Test
    fun externalScannerEnabledFallsBackToFalseOnIOException() = runTest {
        val store = SettingsStore(ThrowingPreferenceDataStore())

        assertFalse(store.externalScannerEnabled().first())
    }

    @Test
    fun printerAutoPrintEnabledDefaultsFalseWhenNoValueWasSaved() = runTest {
        val store = SettingsStore(testDataStore())

        assertFalse(store.printerAutoPrintEnabled().first())
    }

    @Test
    fun setPrinterAutoPrintEnabledPersistsValue() = runTest {
        val store = SettingsStore(testDataStore())

        store.setPrinterAutoPrintEnabled(true)

        assertTrue(store.printerAutoPrintEnabled().first())
    }

    @Test
    fun printerBluetoothAddressDefaultsNullWhenNoValueWasSaved() = runTest {
        val store = SettingsStore(testDataStore())

        assertNull(store.printerBluetoothAddress().first())
    }

    @Test
    fun setPrinterBluetoothAddressPersistsAndClearsValue() = runTest {
        val store = SettingsStore(testDataStore())

        store.setPrinterBluetoothAddress("AA:BB:CC:DD:EE:FF")
        assertEquals("AA:BB:CC:DD:EE:FF", store.printerBluetoothAddress().first())

        store.setPrinterBluetoothAddress(null)
        assertNull(store.printerBluetoothAddress().first())
    }

    private fun testDataStore(): DataStore<Preferences> = FakePreferenceDataStore()

    private class FakePreferenceDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(preferencesOf())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val next = transform(state.value)
            state.value = next
            return next
        }
    }

    private class ThrowingPreferenceDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow {
            throw IOException("boom")
        }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            error("Not used in this test")
        }
    }
}
