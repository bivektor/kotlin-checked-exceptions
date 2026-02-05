package com.bivektor.kotlin.test

import java.io.CharConversionException
import java.io.IOException

class HandledKotlinExamples() {

    // Handled in constructor body
    constructor(value: Int) : this() {
        try {
            JavaThrower.throwsIo()
        } catch (e: IOException) {
            // handled
        }
    }

    // Handled in init block
    init {
        try {
            JavaThrower.throwsIo()
        } catch (e: IOException) {
            // handled
        }
    }

    // Handled in property initializer
    val initializedProp: Unit = try {
        JavaThrower.throwsIo()
    } catch (e: IOException) {
        // handled
    }

    @Throws(CharConversionException::class)
    fun callThrows() {
        throw CharConversionException("some message")
    }

    fun kotlinThrowsCaught() {
        try {
            callThrows()
        } catch (e: CharConversionException) {
            // handled
        }
    }

    fun kotlinThrowsCaughtSuperType() {
        try {
            callThrows()
        } catch (e: IOException) {
            // handled
        }
    }

    @Throws(IOException::class)
    fun kotlinThrowsRethrown() {
        callThrows()
    }

    fun lambdaKotlinThrowsCaughtOutside() {
        try {
            repeat(1) {
                callThrows()
            }
        } catch (e: IOException) {
            // handled
        }
    }

    @Throws(IOException::class)
    fun lambdaKotlinThrowsRethrown() {
        repeat(1) {
            callThrows()
        }
    }

    // Scope functions - caught
    fun letThrowsCaught() {
        try {
            "test".let { callThrows() }
        } catch (e: IOException) {
            // handled
        }
    }

    fun runThrowsCaught() {
        try {
            run { callThrows() }
        } catch (e: IOException) {
            // handled
        }
    }

    fun applyThrowsCaught() {
        try {
            "test".apply { JavaThrower.throwsIo() }
        } catch (e: IOException) {
            // handled
        }
    }

    fun alsoThrowsCaught() {
        try {
            "test".also { JavaThrower.throwsIo() }
        } catch (e: IOException) {
            // handled
        }
    }

    // Scope functions - rethrown
    @Throws(IOException::class)
    fun letThrowsRethrown() {
        "test".let { callThrows() }
    }

    @Throws(IOException::class)
    fun runThrowsRethrown() {
        run { JavaThrower.throwsIo() }
    }

    // Lazy delegate - caught
    fun lazyBlockThrowsCaught() {
        val lazyVal by lazy {
            try {
                callThrows()
            } catch (e: CharConversionException) {
                // handled
            }
        }
        println(lazyVal)
    }
}
