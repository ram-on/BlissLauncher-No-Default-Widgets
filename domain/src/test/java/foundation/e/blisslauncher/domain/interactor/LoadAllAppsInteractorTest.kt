package foundation.e.blisslauncher.domain.interactor

import foundation.e.blisslauncher.common.executors.AppExecutors
import foundation.e.blisslauncher.common.executors.MainThreadExecutor
import foundation.e.blisslauncher.domain.repository.LauncherRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors

class LoadAllAppsInteractorTest {

    private lateinit var loadAllAppsInteractor: LoadAllAppsInteractor
    lateinit var launcherRepository: LauncherRepository
    lateinit var appExecutors: AppExecutors

    @MockK
    lateinit var mainThreadExecutor: MainThreadExecutor

    @Before
    fun setUp() {
        launcherRepository = mockk {
            every {
                getAllApps()
            } returns Single.just(listOf(
                mockk {
                    every {
                        title = "App1"
                    }
                },
                mockk {
                    every {
                        title = "App2"
                    }
                }
            ))
        }
        MockKAnnotations.init(this)
        appExecutors = AppExecutors(
            Executors.newSingleThreadExecutor(),
            Executors.newSingleThreadExecutor(),
            mainThreadExecutor
        )
        loadAllAppsInteractor = LoadAllAppsInteractor(launcherRepository, appExecutors)
    }

    @After
    fun tearDown() {
    }

    @Test
    fun doWorkCallsRepository() {
        launcherRepository = mockk {
            every {
                getAllApps()
            } returns Single.just(listOf(
                mockk {
                    every {
                        title = "App1"
                    }
                },
                mockk {
                    every {
                        title = "App2"
                    }
                }
            ))
        }
        loadAllAppsInteractor.doWork()
        verify(exactly = 1) { launcherRepository.getAllApps() }
    }

    @Test
    fun invokeCallsRepositoryAndCompletes() {

        loadAllAppsInteractor()
        verify(exactly = 1) { launcherRepository.getAllApps() }

        loadAllAppsInteractor(onSuccess = { Assert.assertEquals(2, it.size) })
    }
}