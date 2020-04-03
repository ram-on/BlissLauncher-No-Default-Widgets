package foundation.e.blisslauncher.base.presentation

/**
 * Intent that are used to change state.
 * It can either reduce to a new state or another intent which resolves the new state.
 */
interface BaseIntent<T> {
    fun reduce(oldState: T): T
}

typealias StateReducer<T> = T.() -> T
typealias UnitReducer<T> = T.() -> Unit

/**
 *
 * NOTE: Magic of extension functions, (T)->T and T.()->T interchangeable.
 */
fun <T> intent(block: StateReducer<T>): BaseIntent<T> = object :
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
fun <T> sideEffect(block: UnitReducer<T>): BaseIntent<T> = object :
    BaseIntent<T> {
    override fun reduce(oldState: T): T = oldState.apply(block)
}