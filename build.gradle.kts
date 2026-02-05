val artifactSuffix: String by project

subprojects {
  repositories {
    mavenCentral()
  }

  tasks.withType<Jar>().configureEach {
    archiveBaseName.set("bivektor-kotlin-checked-exceptions-${project.name}-$artifactSuffix")
  }
}
