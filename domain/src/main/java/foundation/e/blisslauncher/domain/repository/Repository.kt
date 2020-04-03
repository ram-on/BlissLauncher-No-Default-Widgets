package foundation.e.blisslauncher.domain.repository

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

/**
 * Generic Reactive Repository interface which captures the domain type to manage with generic CRUD operations.
 *
 * @param <T> the domain type which the repository manages.
 * @param <ID> the type of the id of the entity which the repository manages.
 *
 * @author Amit Kumar
 */
interface Repository<T, ID> {

    /**
     * Saves a given entity and return the instance for further operations.
     *
     * @param entity to be saved.
     * @return [Single] emitting the saved entity.
     */
    fun <S : T> save(entity: S): Single<S>

    /**
     * Saves all given entities.
     *
     * @param entities to be saved.
     * @return [Flowable] emitting the saved entities.
     */
    fun <S : T> saveAll(entities: Iterable<S>): Flowable<Iterable<S>>

    /**
     * Retrieves an entity by its id.
     *
     * @param id of the entity.
     * @return [Maybe] emitting the entity with the given id or {@link Maybe#empty()} if none found.
     */
    fun findById(id: ID): Maybe<T>

    /**
     * Return all entities of this type.
     *
     * @return [Flowable] emitting all entities.
     */
    fun findAll(): Flowable<Iterable<T>>

    /**
     * Deletes a given entity.
     *
     * @return [Completable] signaling when operation has completed.
     */
    fun delete(entity: T)

    /**
     * Deletes the entity with the given id.
     *
     * @return [Completable] signaling when operation has completed.
     */
    fun deleteById(id: ID): Completable

    /**
     * Deletes all entities managed by this repository.
     *
     * @return [Completable] signaling when operation has completed.
     */
    fun deleteAll()

    /**
     * Deletes the given entities.
     *
     * @return [Completable] signaling when operation has completed.
     */
    fun deleteAll(entities: Iterable<T>)
}