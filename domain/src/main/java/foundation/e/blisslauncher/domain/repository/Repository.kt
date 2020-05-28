package foundation.e.blisslauncher.domain.repository

/**
 * Generic Repository interface which captures the domain type to manage with generic CRUD operations.
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
     * @return [S] entity
     */
    fun <S : T> save(entity: S): S

    /**
     * Saves all given entities.
     *
     * @param entities to be saved.
     * @return [List] with the saved entities.
     */
    fun <S : T> saveAll(entities: List<S>): List<S>

    /**
     * Retrieves an entity by its id.
     *
     * @param id of the entity.
     * @return [T] the entity with the given id or null if none found.
     */
    fun findById(id: ID): T?

    /**
     * Return all entities of this type.
     *
     * @return [List] with all entities.
     */
    fun findAll(): List<T>

    /**
     * Deletes a given entity.
     */
    fun delete(entity: T)

    /**
     * Deletes the entity with the given id.
     */
    fun deleteById(id: ID)

    /**
     * Deletes all entities managed by this repository.
     */
    fun deleteAll()

    /**
     * Deletes the given entities.
     */
    fun deleteAll(entities: List<T>)
}