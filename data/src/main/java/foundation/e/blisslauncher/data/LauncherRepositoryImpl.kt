package foundation.e.blisslauncher.data

import android.content.Context
import android.os.UserHandle
import foundation.e.blisslauncher.common.Utilities
import foundation.e.blisslauncher.common.compat.LauncherAppsCompat
import foundation.e.blisslauncher.common.util.MultiHashMap
import foundation.e.blisslauncher.domain.entity.LauncherItem
import foundation.e.blisslauncher.domain.repository.LauncherRepository
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject

class LauncherRepositoryImpl
@Inject constructor(
    private val context: Context,
    private val launcherApps: LauncherAppsCompat,
    private val launcherDatabase: LauncherDatabaseGateway
) :
    LauncherRepository {
    override fun <S : LauncherItem> save(entity: S): Single<S> {
        TODO("Not yet implemented")
    }

    override fun <S : LauncherItem> saveAll(entities: Iterable<S>): Flowable<Iterable<S>> {
        TODO("Not yet implemented")
    }

    override fun findById(id: Long): Maybe<LauncherItem> {
        TODO("Not yet implemented")
    }

    override fun findAll(): Flowable<Iterable<LauncherItem>> {
        val pmHelper = PackageManagerHelper(context, launcherApps)
        val isSafeMode = pmHelper.isSafeMode
        val isSdCardReady = Utilities.isBootCompleted()
        val pendingPackages = MultiHashMap<UserHandle, String>()
        var clearDb = false

        //TODO: GridSize Migration Task
        if(clearDb) {

        }
    }

    override fun delete(entity: LauncherItem) {
        TODO("Not yet implemented")
    }

    override fun deleteById(id: Long): Completable {
        TODO("Not yet implemented")
    }

    override fun deleteAll() {
        TODO("Not yet implemented")
    }

    override fun deleteAll(entities: Iterable<LauncherItem>) {
        TODO("Not yet implemented")
    }
}