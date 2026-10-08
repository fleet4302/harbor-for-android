package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.StremioApiClient
import com.example.data.local.HarborDatabase
import com.example.data.repository.AddonRepository
import com.example.data.repository.StreamResolverRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Harbor", appName)
  }

  @Test
  fun `zero streams returned before linking any source`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("harbor_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("harbor_stremio_account", Context.MODE_PRIVATE).edit().clear().commit()

    val database = HarborDatabase.getInstance(context)
    val apiClient = StremioApiClient()
    val addonRepo = AddonRepository(context, database.addonDao(), apiClient)
    val streamResolver = StreamResolverRepository(context, addonRepo, apiClient)

    // Verify unlinked state
    assertFalse(streamResolver.isStreamSourceLinked())

    // Must return strictly empty list (no fake streams, no unlinked streams)
    val streams = streamResolver.resolveStreams("movie", "tt15239678")
    assertTrue(streams.isEmpty())
  }

  @Test
  fun `linking Torrentio updates isStreamSourceLinked`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = HarborDatabase.getInstance(context)
    val apiClient = StremioApiClient()
    val addonRepo = AddonRepository(context, database.addonDao(), apiClient)
    val streamResolver = StreamResolverRepository(context, addonRepo, apiClient)

    streamResolver.linkTorrentio()
    assertTrue(streamResolver.isStreamSourceLinked())

    streamResolver.unlinkTorrentio()
    assertFalse(streamResolver.isStreamSourceLinked())
  }
}
