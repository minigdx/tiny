package com.github.minigdx.tiny.util

import kotlin.math.max

class MutableFixedSizeList<T>(val maxSize: Int) : MutableList<T> {
    private val delegate: MutableList<T> = ArrayList<T>(maxSize)

    override val size: Int
        get() {
            return delegate.size
        }

    private fun evictOldElements() {
        val eltToRemove = max(0, delegate.size - maxSize)
        (0 until eltToRemove).forEach { _ ->
            removeAt(0)
        }
    }

    override fun contains(element: T): Boolean = delegate.contains(element)

    override fun containsAll(elements: Collection<T>): Boolean = delegate.containsAll(elements)

    override fun add(element: T): Boolean {
        val result = delegate.add(element)
        evictOldElements()
        return result
    }

    override fun add(
        index: Int,
        element: T,
    ) {
        delegate.add(index, element)
        evictOldElements()
    }

    override fun addAll(
        index: Int,
        elements: Collection<T>,
    ): Boolean {
        val result = delegate.addAll(index, elements)
        evictOldElements()
        return result
    }

    override fun addAll(elements: Collection<T>): Boolean {
        if (count() + elements.size > maxSize) return false
        return delegate.addAll(elements)
    }

    override fun clear() = delegate.clear()

    override fun get(index: Int): T = delegate.get(index)

    override fun isEmpty(): Boolean = delegate.isEmpty()

    override fun iterator(): MutableIterator<T> = delegate.iterator()

    override fun listIterator(): MutableListIterator<T> = delegate.listIterator()

    override fun listIterator(index: Int): MutableListIterator<T> = delegate.listIterator(index)

    override fun removeAt(index: Int): T = delegate.removeAt(index)

    override fun subList(
        fromIndex: Int,
        toIndex: Int,
    ): MutableList<T> = delegate.subList(fromIndex, toIndex)

    override fun set(
        index: Int,
        element: T,
    ): T = delegate.set(index, element)

    override fun retainAll(elements: Collection<T>): Boolean = delegate.retainAll(elements)

    override fun removeAll(elements: Collection<T>): Boolean = delegate.removeAll(elements)

    override fun remove(element: T): Boolean = delegate.remove(element)

    override fun lastIndexOf(element: T): Int = delegate.lastIndexOf(element)

    override fun indexOf(element: T): Int = delegate.indexOf(element)
}
