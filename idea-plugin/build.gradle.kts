plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":common"))
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
