import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "2.2.0"
  id("com.gradleup.shadow") version "8.3.0"
  id("xyz.jpenilla.run-paper") version "2.3.1"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
}

group = "ru.mairwunnx"
version = "1.1.1"

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://oss.sonatype.org/content/groups/public/")
}

dependencies {
  compileOnly("io.papermc.paper:paper-api:1.21.6-R0.1-SNAPSHOT")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
  implementation("com.charleskorn.kaml:kaml:0.58.0")
}

/* ── shadowJar becomes default artifact ─────────────────────────── */
tasks.build { dependsOn("shadowJar") }

/* ── filter plugin.yml with project.version ─────────────────────── */
tasks.processResources {
  val props = mapOf("version" to version)
  inputs.properties(props)
  filteringCharset = "UTF-8"
  filesMatching("paper-plugin.yml") {
    expand(props)
  }
}

/* ── compiler flags ─────────────────────────────────────────────── */
tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    freeCompilerArgs.add("-Xcontext-parameters")
  }
}

tasks { runServer { minecraftVersion("1.21.8") } }
kotlin { jvmToolchain(21) }