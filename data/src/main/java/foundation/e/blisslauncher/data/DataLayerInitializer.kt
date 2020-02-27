package foundation.e.blisslauncher.data

import android.content.Context
import foundation.e.blisslauncher.data.inject.DaggerDataComponent
import foundation.e.blisslauncher.data.inject.DataComponent
import foundation.e.blisslauncher.domain.inject.DomainComponent

class DataLayerInitializer {
    fun initialize(appContext: Context): DataComponent {
        return initializeDataComponent(appContext)
    }

    private fun initializeDataComponent(appContext: Context): DataComponent {
        val dataComponent = DaggerDataComponent.factory().create(appContext)
        DomainComponent.INSTANCE = dataComponent
        return dataComponent
    }
}