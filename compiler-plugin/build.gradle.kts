plugins {
    kotlin("jvm")
}

val kotlinVersion: String by project

dependencies {
    implementation(project(":common"))
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Use the standard JAR; compiler plugin classpath should be configured via Gradle configs.
