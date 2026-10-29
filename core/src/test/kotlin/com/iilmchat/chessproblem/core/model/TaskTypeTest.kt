package com.iilmchat.chessproblem.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaskTypeTest {
    @Test
    fun fromIdMapsAllTypes() {
        assertEquals(TaskType.MATE, TaskType.fromId("mate"))
        assertEquals(TaskType.WIN, TaskType.fromId("Win"))
        assertEquals(TaskType.DRAW, TaskType.fromId("draw"))
    }

    @Test
    fun fromIdRejectsUnknown() {
        assertFailsWith<IllegalStateException> { TaskType.fromId("nope") }
    }
}
