// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.repo

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanJobMonitorRepositoryTest {

    @Test
    fun buildScanJobMonitorHubUrl_usesServerRoot() {
        assertEquals(
            "http://192.168.11.140:8080/hubs/scan-jobs",
            buildScanJobMonitorHubUrl("http://192.168.11.140:8080/")
        )
    }

    @Test
    fun buildScanJobMonitorHubUrl_removesApiSuffix() {
        assertEquals(
            "https://example.test/logibooks/hubs/scan-jobs",
            buildScanJobMonitorHubUrl("https://example.test/logibooks/api/")
        )
    }
}
