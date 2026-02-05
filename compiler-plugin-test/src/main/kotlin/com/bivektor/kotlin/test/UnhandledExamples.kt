package com.bivektor.kotlin.test

import java.io.CharConversionException
import java.io.IOException

class UnhandledExamples() {

    // Unhandled in constructor body
    constructor(value: Int) : this() {
        JavaThrower.throwsIo()
    }

    // Unhandled in init block
    init {
        callThrows()
    }

    // Unhandled in property initializer
    val initializedProp: Unit = JavaThrower.throwsIo()

    private var mutableProp: Int
        get() {
            throw IOException("boom")
        }
        set(value) {
            throw IOException("boom")
        }

    private val getterOnlyProp: Int
        get() {
            throw IOException("boom")
        }

    @Throws(CharConversionException::class)
    fun callThrows(): Any {
        throw CharConversionException("some message")
    }

    fun kotlinThrowsNotCaught() {
        callThrows()
    }

    fun javaThrowerNotCaught() {
        JavaThrower.throwsIo()
    }

    fun lambdaKotlinThrows() {
        repeat(1) {
            callThrows()
        }
    }

    fun lambdaJavaThrows() {
        repeat(1) {
            JavaThrower.throwsIo()
        }
    }

    fun accessPropertiesNotCaught() {
        val value = getterOnlyProp
        mutableProp = value
    }

    // Scope functions
    fun letKotlinThrows(): Any {
        return "test".let { callThrows()}
    }

    fun runKotlinThrows() {
        run(::callThrows)
    }

    fun applyJavaThrows() {
        "test".apply { JavaThrower.throwsIo() }
    }

    fun alsoJavaThrows() {
        "test".also { JavaThrower.throwsIo() }
    }

    // Lazy delegate
    fun lazyBlockThrows() {
        val lazyVal by lazy { callThrows() }
        println(lazyVal)
    }

    // Method references
    fun kotlinMethodRefCall() {
        val ref = ::callThrows
        ref()
    }

    fun javaMethodRefCall() {
        val ref = JavaThrower::throwsIo
        ref()
    }
}
