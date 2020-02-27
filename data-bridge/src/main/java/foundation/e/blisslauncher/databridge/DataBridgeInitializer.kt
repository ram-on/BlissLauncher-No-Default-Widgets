package foundation.e.blisslauncher.databridge

import android.content.Context
import foundation.e.blisslauncher.data.DataLayerInitializer

object DataBridgeInitializer {
    private val dataInitializer: DataLayerInitializer by lazy { DataLayerInitializer() }

    fun initialize(appContext: Context) {
        dataInitializer.initialize(appContext)
    }
}