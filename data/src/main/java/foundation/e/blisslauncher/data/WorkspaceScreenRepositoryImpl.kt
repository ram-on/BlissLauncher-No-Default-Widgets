package foundation.e.blisslauncher.data

import foundation.e.blisslauncher.domain.entity.WorkspaceScreen
import foundation.e.blisslauncher.domain.repository.WorkspaceScreenRepository
import javax.inject.Inject

class WorkspaceScreenRepositoryImpl
@Inject constructor(private val launcherDatabase: LauncherDatabaseGateway) :
    WorkspaceScreenRepository {

    override fun findAllOrderedByScreenRank(): List<Long> {
        return launcherDatabase.loadWorkspaceScreensInOrder()
    }

    override fun generateNewScreenId(): Long {
        return launcherDatabase.generateNewScreenId()
    }

    override fun <S : WorkspaceScreen> save(entity: S): S {
        TODO("Not yet implemented")
    }

    override fun <S : WorkspaceScreen> saveAll(entities: List<S>): List<S> {
        TODO("Not yet implemented")
    }

    override fun findById(id: Long): WorkspaceScreen? {
        TODO("Not yet implemented")
    }

    override fun findAll(): List<WorkspaceScreen> {
        TODO("Not yet implemented")
    }

    override fun delete(entity: WorkspaceScreen) {
        TODO("Not yet implemented")
    }

    override fun deleteById(id: Long) {
        TODO("Not yet implemented")
    }

    override fun deleteAll() {
        TODO("Not yet implemented")
    }

    override fun deleteAll(entities: List<WorkspaceScreen>) {
        TODO("Not yet implemented")
    }
}