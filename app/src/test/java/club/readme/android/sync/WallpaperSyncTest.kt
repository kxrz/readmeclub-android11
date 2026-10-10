package club.readme.android.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperSyncTest {

    @Test
    fun acceptsPlainIds() {
        assertTrue(WallpaperSync.isSafeId("3f2a9c1e-77b0-4c2d-9a51-0d6e8f1b2c3a"))
        assertTrue(WallpaperSync.isSafeId("wallpaper_42"))
    }

    @Test
    fun rejectsIdsThatEscapeTheCache() {
        assertFalse(WallpaperSync.isSafeId("../../shared_prefs/x"))
        assertFalse(WallpaperSync.isSafeId("a/b"))
        assertFalse(WallpaperSync.isSafeId(""))
        assertFalse(WallpaperSync.isSafeId("a".repeat(65)))
    }
}
