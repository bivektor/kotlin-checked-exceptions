plugins {
    kotlin("jvm")
}

val kotlinVersion: String by project

repositories {
    mavenCentral()
}

dependencies {
    kotlinCompilerPluginClasspath(project(":compiler-plugin"))
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            listOf(
                "-Xcontext-parameters"
            )
        )
    }
}

tasks.named("compileKotlin").configure {
    dependsOn(":compiler-plugin:jar")
}
