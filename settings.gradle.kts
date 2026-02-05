pluginManagement {
    val kotlinVersion = providers.gradleProperty("kotlinVersion").get()
    plugins {
        kotlin("jvm") version kotlinVersion
    }
}

rootProject.name = "kotlin-checked-exceptions"

include(":common")
include(":compiler-plugin")
include(":compiler-plugin-test")
