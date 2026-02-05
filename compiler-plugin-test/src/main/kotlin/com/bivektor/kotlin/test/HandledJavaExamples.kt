package com.bivektor.kotlin.test

import java.io.IOException

class HandledJavaExamples {

    fun javaThrowerCaught() {
        try {
            JavaThrower.throwsIo()
        } catch (e: IOException) {
            // handled
        }
    }

    @Throws(IOException::class)
    fun javaThrowerRethrown() {
        JavaThrower.throwsIo()
    }

    fun lambdaJavaThrowsCaughtOutside() {
        try {
            repeat(1) {
                JavaThrower.throwsIo()
            }
        } catch (e: IOException) {
            // handled
        }
    }
}
