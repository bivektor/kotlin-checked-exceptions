package com.bivektor.kotlin.test

import java.io.IOException

class HandledPropertyExamples {

    @get:Throws(IOException::class)
    @set:Throws(IOException::class)
    private var mutableProp: Int
        get() {
            throw IOException("boom")
        }
        set(value) {
            throw IOException("boom")
        }

    @get:Throws(IOException::class)
    private val getterOnlyProp: Int
        get() {
            throw IOException("boom")
        }

    fun accessPropertiesCaught() {
        try {
            val value = getterOnlyProp
            mutableProp = value
        } catch (e: IOException) {
            // handled
        }
    }

    @Throws(IOException::class)
    fun accessPropertiesRethrown() {
        val value = getterOnlyProp
        mutableProp = value
    }
}
