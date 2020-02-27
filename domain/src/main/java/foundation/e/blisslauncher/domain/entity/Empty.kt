package foundation.e.blisslauncher.domain.entity

/**
 * The entity with only one value: the `Empty` object. Can be used in interactors those are intended to return void.
 */
object Empty : Entity {
    override fun toString(): String = "BlissLauncher.Empty"
}