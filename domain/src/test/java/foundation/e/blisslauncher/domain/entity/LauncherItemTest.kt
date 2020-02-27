package foundation.e.blisslauncher.domain.entity

import org.junit.Test

class LauncherItemTest {

    lateinit var launcherItem: LauncherItem

    @org.junit.Before
    fun setUp() {
        launcherItem = LauncherItem()
    }

    @Test
    fun testDumpProperties() {
        launcherItem.toString()
    }
}