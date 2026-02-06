import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSetContainer

plugins {
    kotlin("jvm")
    `maven-publish`
}

val kotlinVersion: String by project
val commonProject = project(":common")

dependencies {
    compileOnly(project(":common"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
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

publishing {
    publications {
        register<MavenPublication>("github") {
            from(components["java"])
            artifactId = "kotlin-checked-exceptions-compiler-plugin-k$kotlinVersion"
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/bivektor/kotlin-checked-exceptions")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: findProperty("github.actor") as String?
                password = System.getenv("GITHUB_TOKEN") ?: findProperty("github.token") as String?
            }
        }
    }
}
