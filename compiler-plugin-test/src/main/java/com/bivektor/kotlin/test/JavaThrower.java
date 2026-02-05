package com.bivektor.kotlin.test;

import java.io.IOException;

public class JavaThrower {
    public static void throwsIo() throws IOException {
        throw new IOException("boom");
    }
}
