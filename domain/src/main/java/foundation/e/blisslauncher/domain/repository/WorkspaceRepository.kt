package foundation.e.blisslauncher.domain.repository

import foundation.e.blisslauncher.domain.dto.WorkspaceModel

interface WorkspaceRepository {
    fun loadWorkspace(): WorkspaceModel
}