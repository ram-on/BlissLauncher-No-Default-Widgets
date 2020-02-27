package foundation.e.blisslauncher.base.presentation

interface BaseIntent<T> {
    fun reduce(oldState: T): T
}

/**
 *
 * NOTE: Magic of extension functions, (T)->T and T.()->T interchangeable.
 */
fun <T> intent(block: T.() -> T): BaseIntent<T> = object :
    BaseIntent<T> {
    override fun reduce(oldState: T): T = block(oldState)
}

/**
 * By delegating work to other models, repositories or services, we
 * end up with situations where we don't need to update our ModelStore
 * state until the delegated work completes.
 *
 * Use the `sideEffect {}` DSL function for those situations.
 */
fun <T> sideEffect(block: T.() -> Unit): BaseIntent<T> = object :
    BaseIntent<T> {
    override fun reduce(oldState: T): T = oldState.apply(block)
}