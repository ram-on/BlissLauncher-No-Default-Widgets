package foundation.e.blisslauncher.inject

import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import foundation.e.blisslauncher.BlissLauncher
import foundation.e.blisslauncher.domain.inject.DomainComponent
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        AndroidInjectionModule::class,
        ActivityBindsModule::class
    ],
    dependencies = [
        DomainComponent::class
    ]
)
interface AppComponent : AndroidInjector<BlissLauncher> {
    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: BlissLauncher,
            domainComponent: DomainComponent
        ): AppComponent
    }
}