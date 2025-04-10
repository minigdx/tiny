package com.github.minigdx.tiny.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MutableFixedSizeListTest {
    @Test
    fun add_it_adds_an_element() {
        val list = MutableFixedSizeList<Int>(3)
        list.add(1)
        assertEquals(1, list.size)
        assertTrue(list.contains(1))
    }

    @Test
    fun add_when_it_overflow_it_removes_old_elements() {
        val list = MutableFixedSizeList<Int>(3)
        list.add(1)
        list.add(2)
        list.add(3)
        list.add(4)
        assertEquals(3, list.size)
        assertTrue(list.containsAll(listOf(2, 3, 4)))
        assertFalse(list.contains(1))
    }
}
