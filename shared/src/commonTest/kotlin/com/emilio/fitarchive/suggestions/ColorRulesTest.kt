package com.emilio.fitarchive.suggestions

import com.emilio.fitarchive.model.GarmentColor.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColorRulesTest {
    @Test
    fun vacioOUnSoloColorCombina() {
        assertEquals(1.0, ColorRules.evaluar(emptyList()).score)
        assertEquals(1.0, ColorRules.evaluar(listOf(RED, RED)).score)
        assertTrue(ColorRules.combinan(listOf(BLUE)))
    }

    @Test
    fun neutrosCombinanConTodo() {
        assertTrue(ColorRules.combinan(listOf(BLACK, RED)))
        assertTrue(ColorRules.combinan(listOf(WHITE, PURPLE, BEIGE)))
        assertEquals(1.0, ColorRules.evaluar(listOf(NAVY, GREY, WHITE)).score)
    }

    @Test
    fun analogosCombinan() {
        assertTrue(ColorRules.combinan(listOf(YELLOW, GREEN)))
        assertTrue(ColorRules.combinan(listOf(PINK, BURGUNDY)))
    }

    @Test
    fun complementariosCombinan() {
        assertTrue(ColorRules.combinan(listOf(BLUE, ORANGE)))
        assertTrue(ColorRules.combinan(listOf(YELLOW, PURPLE)))
    }

    @Test
    fun chocquesConocidosNoCombinan() {
        val r = ColorRules.evaluar(listOf(RED, PINK))
        assertFalse(r.combina)
        assertEquals(listOf(RED to PINK), r.choques)
        assertFalse(ColorRules.combinan(listOf(RED, GREEN)))
        assertFalse(ColorRules.combinan(listOf(ORANGE, PURPLE)))
    }

    @Test
    fun choqueSeMantieneConNeutrosAnadidos() {
        assertFalse(ColorRules.combinan(listOf(RED, PINK, BLACK)))
    }

    @Test
    fun demasiadosCromaticosPenalizan() {
        val dos = ColorRules.evaluar(listOf(BLUE, ORANGE)).score
        val cuatro = ColorRules.evaluar(listOf(BLUE, ORANGE, YELLOW, PURPLE)).score
        assertTrue(cuatro < dos)
    }

    @Test
    fun multiConCromaticoNoCombina() {
        assertFalse(ColorRules.combinan(listOf(MULTI, RED)))
        assertTrue(ColorRules.combinan(listOf(MULTI, BLACK)))
    }

    @Test
    fun puntuacionSiempreEnRango() {
        val todos = entries
        for (a in todos) for (b in todos) {
            val s = ColorRules.puntuacionPareja(a, b)
            assertTrue(s in 0.0..1.0, "$a-$b = $s")
            assertEquals(s, ColorRules.puntuacionPareja(b, a), "simetría $a-$b")
        }
    }
}
