package com.example

import com.example.data.model.HarborStreamParser
import com.example.data.model.StremioStreamItem
import com.example.data.model.StreamResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testHarborStreamParser_4K_Debrid_HDR() {
    val streamItem = StremioStreamItem(
      name = "[RD+] Torrentio\n4K UHD",
      title = "Dune Part Two (2024)\n⚙️ 2160p | 💾 18.4 GB | 👤 320 | HDR10 | DV | Atmos",
      url = "https://example.com/stream.mp4"
    )

    val parsed = HarborStreamParser.parse(streamItem, "Torrentio")

    assertEquals(StreamResolution.RES_4K, parsed.resolution)
    assertTrue(parsed.isDebrid)
    assertEquals("Real-Debrid", parsed.debridProvider)
    assertTrue(parsed.isHdr)
    assertTrue(parsed.isDolbyVision)
    assertEquals(320, parsed.seeders)
    assertEquals("18.4 GB", parsed.fileSizeFormatted)
    assertTrue(parsed.harborScore > 100)
  }

  @Test
  fun testHarborStreamParser_1080p_Ranking() {
    val highQuality = StremioStreamItem(
      name = "[RD+] Real-Debrid 4K",
      title = "4K UHD HDR Remux | 💾 24 GB",
      url = "https://example.com/4k.mp4"
    )
    val standard = StremioStreamItem(
      name = "Torrentio 720p",
      title = "720p HD | 💾 1.2 GB | 👤 15",
      url = "https://example.com/720p.mp4"
    )

    val parsedHigh = HarborStreamParser.parse(highQuality, "Torrentio")
    val parsedStd = HarborStreamParser.parse(standard, "Torrentio")

    assertTrue(parsedHigh.harborScore > parsedStd.harborScore)
    assertEquals(StreamResolution.RES_720P, parsedStd.resolution)
  }

  @Test
  fun testHarborStreamParser_TorrentInfoHash() {
    val streamItem = StremioStreamItem(
      name = "Torrentio\n1080p",
      title = "Dune.Part.Two.2024.1080p.BluRay.x264\n💾 10.5 GB | 👤 85",
      infoHash = "4a5b6c7d8e9f0123456789abcdef0123456789ab",
      fileIdx = 0
    )
    val parsed = HarborStreamParser.parse(streamItem, "Torrentio")
    assertEquals(StreamResolution.RES_1080P, parsed.resolution)
    assertEquals(85, parsed.seeders)
    assertEquals("10.5 GB", parsed.fileSizeFormatted)
  }
}

