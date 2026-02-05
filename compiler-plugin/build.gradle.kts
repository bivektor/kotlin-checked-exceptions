import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSetContainer

plugins {
    kotlin("jvm")
}

val kotlinVersion: String by project
val commonProject = project(":common")

dependencies {
    compileOnly(project(":common"))
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(commonProject.tasks.named("classes"))
    val commonSourceSets = commonProject.extensions.getByType<SourceSetContainer>()
    from(commonSourceSets["main"].output)
}
