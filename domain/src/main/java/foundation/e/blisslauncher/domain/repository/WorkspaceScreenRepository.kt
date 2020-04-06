package foundation.e.blisslauncher.domain.repository

import foundation.e.blisslauncher.domain.entity.WorkspaceScreen

interface WorkspaceScreenRepository : Repository<WorkspaceScreen, Long> {
    fun findAllOrderedByScreenRank(): Iterable<WorkspaceScreen>
}