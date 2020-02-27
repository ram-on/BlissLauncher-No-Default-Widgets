package foundation.e.blisslauncher.data.shortcuts

import android.content.pm.ShortcutInfo
import foundation.e.blisslauncher.domain.repository.ShortcutRepository
import io.reactivex.Single
import javax.inject.Inject

class ShortcutsRepositoryImpl @Inject constructor() : ShortcutRepository {
    override fun getAllShortcuts(): Single<List<ShortcutInfo>> {
        return Single.just(emptyList())
    }
}