// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

package consulting.sw.logiscanner.repo

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.util.Base64
import java.util.concurrent.LinkedBlockingQueue

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

    @Test
    fun loadTajikistanInvoiceFetchesParcelLabelAndReportsMissingData() {
        val label = "SIZE 58 mm,40 mm\r\nPRINT 1,1\r\n".toByteArray()
        val encoded = Base64.getEncoder().encodeToString(label)
        val requests = LinkedBlockingQueue<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/ScanJobs/3/monitor/parcels/") { exchange ->
            requests += "${exchange.requestURI}|${exchange.requestHeaders.getFirst("Authorization")}"
            val valid = exchange.requestURI.path.endsWith("/7/label")
            val body = if (valid) {
                """{"template":"TJ_EXPORT","contentBase64":"$encoded","language":"TSPL","widthMm":58,"heightMm":40}"""
            } else {
                """{"msg":"Invoice data missing"}"""
            }.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(if (valid) 200 else 422, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val repo = ScanJobMonitorRepository("http://127.0.0.1:${server.address.port}/api/", "token")
            assertArrayEquals(label, runBlocking { repo.loadTajikistanInvoice(3, 7) })
            val failure = assertThrows(IOException::class.java) {
                runBlocking { repo.loadTajikistanInvoice(3, 8) }
            }
            assertEquals("Invoice data missing", failure.message)
            assertEquals(
                listOf(
                    "/api/ScanJobs/3/monitor/parcels/7/label?template=TJ_EXPORT|Bearer token",
                    "/api/ScanJobs/3/monitor/parcels/8/label?template=TJ_EXPORT|Bearer token"
                ),
                requests.toList()
            )
        } finally {
            server.stop(0)
        }
    }
}
