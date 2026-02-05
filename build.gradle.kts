subprojects {
  repositories {
    mavenCentral()
  }

  tasks.withType<Jar>().configureEach {
    archiveBaseName.set("bivektor-kotlin-checked-exceptions-${project.name}")
  }
}
