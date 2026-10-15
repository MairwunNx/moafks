import io.papermc.hangarpublishplugin.model.Platforms

plugins {
  kotlin("jvm") version "2.4.21"
  kotlin("plugin.serialization") version "2.4.21"
  id("com.gradleup.shadow") version "9.6.1"
  id("com.modrinth.minotaur") version "2.10.0"
  id("io.papermc.hangar-publish-plugin") version "0.1.4"
}

group = "ru.mairwunnx"
// The release workflow passes the version; every other build is a dev build.
version = (findProperty("releaseVersion") as String?)?.takeIf { it.isNotBlank() } ?: "dev"

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://repo.extendedclip.com/releases/")
  maven("https://repo.playeranalytics.net/releases/")
}

dependencies {
  compileOnly("io.papermc.paper:paper-api:26.3.build.180-stable")
  compileOnly("me.clip:placeholderapi:2.12.3")
  compileOnly("com.djrapitops:plan-api:5.9-R1.3")
  compileOnly("io.github.miniplaceholders:miniplaceholders-api:3.2.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
  implementation("com.charleskorn.kaml:kaml:0.104.0")
  implementation("com.maxmind.db:maxmind-db:4.2.0")

  testImplementation("io.papermc.paper:paper-api:26.3.build.180-stable")
  testImplementation(kotlin("test"))
  testImplementation(platform("org.junit:junit-bom:6.1.3"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
  jvmToolchain(25)
}

tasks.test {
  useJUnitPlatform()
}

tasks.processResources {
  val version = project.version.toString()
  inputs.property("version", version)
  filesMatching("paper-plugin.yml") {
    expand("version" to version)
  }
}

tasks.shadowJar {
  archiveClassifier = ""
  mergeServiceFiles()
  // Relocated so other plugins that ship their own Kotlin or kaml can't clash with ours.
  listOf("kotlin", "kotlinx", "com.charleskorn.kaml", "it.krzeminski.snakeyaml", "okio", "net.thauvin", "com.maxmind")
    .forEach { relocate(it, "ru.mairwunnx.moafks.libs.$it") }
  exclude("META-INF/*.kotlin_module", "META-INF/versions/*/module-info.class", "module-info.class")
  dependencies { exclude(dependency("org.jetbrains:annotations")) }
}

tasks.jar { enabled = false }
tasks.assemble { dependsOn(tasks.shadowJar) }

// `./gradlew modrinth` with MODRINTH_TOKEN set; the release workflow runs it when asked to.
modrinth {
  token = providers.environmentVariable("MODRINTH_TOKEN")
  projectId = "moafks"
  versionNumber = project.version.toString()
  versionName = "Mo’Afks ${project.version}"
  versionType = "release"
  uploadFile.set(tasks.shadowJar)
  gameVersions.addAll("26.3")
  loaders.addAll("paper", "purpur")
  // The release workflow writes the notes to a file; a local upload falls back to the GitHub release.
  changelog = (findProperty("changelogFile") as String?)?.let { file(it).readText() }
    ?: "https://github.com/MairwunNx/moafks/releases/tag/v${project.version}"
  syncBodyFrom = file("MODRINTH.md").readText()
}

tasks.modrinth {
  dependsOn(tasks.modrinthSyncBody)
}

// `./gradlew publishPluginPublicationToHangar` with HANGAR_TOKEN set; the release workflow runs it when asked to.
hangarPublish {
  publications.register("plugin") {
    version = project.version.toString()
    id = "MoAfks"
    channel = "Release"
    apiKey = providers.environmentVariable("HANGAR_TOKEN")
    changelog = (findProperty("changelogFile") as String?)?.let { file(it).readText() }
      ?: "https://github.com/MairwunNx/moafks/releases/tag/v${project.version}"
    platforms {
      register(Platforms.PAPER) {
        jar = tasks.shadowJar.flatMap { it.archiveFile }
        platformVersions = listOf("26.3")
      }
    }
  }
}
