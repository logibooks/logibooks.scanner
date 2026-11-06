package consulting.sw.logiscanner.ui

import consulting.sw.logiscanner.net.ScanJobMonitorAreas
import consulting.sw.logiscanner.net.ScanJobMonitorBox
import consulting.sw.logiscanner.net.ScanJobMonitorFollowTarget
import consulting.sw.logiscanner.net.ScanJobMonitorSnapshot
import consulting.sw.logiscanner.net.ScanResultItem
import consulting.sw.logiscanner.net.ScannedItemSources
import consulting.sw.logiscanner.repo.ScanJobMonitorScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LookupBoxScanTest {
    @Test
    fun knownBoxWithoutMatchesUsesCoreScanStateAndBoxFollowTarget() {
        val target = ScanJobMonitorFollowTarget(area = ScanJobMonitorAreas.BOX, boxId = 7)
        val result = ScanResultItem(
            count = 0, parcelCount = 0, boxCount = 1,
            scanSource = ScannedItemSources.BOX_STICKER,
            itemNumbers = listOf("BOX-7"), extData = null, hasIssues = false,
            followTarget = target
        )
        val display = localScanResultDisplay(
            lastCode = "BOX-7", lastParcelCount = result.parcelCount,
            lastBoxCount = result.boxCount, lastScanSource = result.scanSource,
            lastItemNumbers = result.itemNumbers, lastExtData = result.extData,
            lastExtId = null, lastScanTime = null
        )!!
        assertEquals(LocalScanResultNumberKind.BOX, display.numberKind)
        assertEquals(1, display.boxCount)
        assertEquals(0, display.parcelCount)
        assertEquals(listOf("BOX-7"), display.itemNumbers)
        // Zero Lookup matches keep the existing normal no-match feedback, not a server error.
        assertEquals(ScanResultColor.NOT_FOUND, determineScanResultColor(result))
        assertEquals(
            LocalScanFollowAction.OpenDetail(ScanJobMonitorScope(ScanJobMonitorAreas.BOX, boxId = 7), null),
            localScanFollowAction(true, result.followTarget)
        )
        val box = ScanJobMonitorBox(
            area = ScanJobMonitorAreas.BOX, boxId = 7, boxCode = "BOX-7", boxStickerScanned = true,
            boxScannedUserName = "Оператор", boxScannedTime = "2026-10-24T12:00:00Z"
        )
        val snapshot = ScanJobMonitorSnapshot(scanJobId = 1, boxes = listOf(box))
        val displayedBox = localScanResultBox(display, snapshot, null)!!
        assertTrue(displayedBox.boxStickerScanned)
        assertEquals("Оператор", displayedBox.boxScannedUserName)
        assertEquals(box.boxScannedTime, displayedBox.boxScannedTime)
    }
}
