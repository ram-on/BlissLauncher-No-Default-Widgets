package foundation.e.blisslauncher.utils

import android.util.LongSparseArray

/**
 * Extension of [LongSparseArray] with some utility methods.
 */
class LongArrayMap<E> : LongSparseArray<E>(),
    Iterable<E> {
    fun containsKey(key: Long): Boolean {
        return indexOfKey(key) >= 0
    }

    val isEmpty: Boolean
        get() = size() <= 0

    override fun clone(): LongArrayMap<E> {
        return super.clone() as LongArrayMap<E>
    }

    override fun iterator(): Iterator<E> {
        return ValueIterator()
    }

    internal inner class ValueIterator : MutableIterator<E> {
        private var mNextIndex = 0
        override fun hasNext(): Boolean {
            return mNextIndex < size()
        }

        override fun next(): E {
            return valueAt(mNextIndex++)
        }

        override fun remove() {
            throw UnsupportedOperationException()
        }
    }
}