package foundation.e.blisslauncher.data.inject

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import foundation.e.blisslauncher.domain.inject.DomainComponent
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CompatModule::class,
        DataRepoBindingModule::class
    ]
)
interface DataComponent: DomainComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance applicationContext: Context): DataComponent
    }
}