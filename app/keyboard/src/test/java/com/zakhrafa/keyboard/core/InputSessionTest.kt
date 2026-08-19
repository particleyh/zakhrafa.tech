package com.zakhrafa.keyboard.core

import android.view.inputmethod.InputConnection
import com.zakhrafa.engine.styles.EnglishStyles
import org.junit.Assert.assertEquals
import org.junit.Test

class InputSessionTest {
    private val connection = java.lang.reflect.Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java)
    ) { _, method, _ ->
        when (method.returnType) {
            Boolean::class.javaPrimitiveType -> true
            Int::class.javaPrimitiveType -> 0
            Long::class.javaPrimitiveType -> 0L
            else -> null
        }
    } as InputConnection

    @Test
    fun tracksLogicalCharactersWhenDecorationExpandsOutput() {
        val session = InputSession()
        session.commit(connection, "a", KeyboardMode.ENGLISH, EnglishStyles.all.first())
        session.commit(connection, "b", KeyboardMode.ENGLISH, EnglishStyles.all.first())

        assertEquals("ab", session.currentWord)
        session.delete(connection)
        assertEquals("a", session.currentWord)
    }

    @Test
    fun punctuationResetsTheCurrentWord() {
        val session = InputSession()
        session.commit(connection, "ا", KeyboardMode.ARABIC, null)
        session.commit(connection, "؟", KeyboardMode.ARABIC, null)

        assertEquals("", session.currentWord)
    }

    @Test
    fun spaceResetsTheCurrentWord() {
        val session = InputSession()
        session.commit(connection, "x", KeyboardMode.ENGLISH, null)
        session.commit(connection, " ", KeyboardMode.ENGLISH, null)

        assertEquals("", session.currentWord)
    }
}
