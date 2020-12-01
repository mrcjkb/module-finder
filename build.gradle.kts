import java.text.SimpleDateFormat
import java.util.*

plugins {
  kotlin("jvm") version "1.4.20"
  id("org.jetbrains.kotlin.plugin.jpa") version "1.4.20"
  id("org.jetbrains.kotlin.plugin.noarg") version "1.4.20"
  java
  idea
  id ("java-library")
  id("com.palantir.git-version") version "0.12.3"
  `java-gradle-plugin`
  id("com.gradle.plugin-publish") version "0.12.0"
  `maven-publish`
  signing
}

group = "com.github.mrcjkb"

val gitVersion: groovy.lang.Closure<String> by extra

allprojects {
  version = gitVersion()
          .replace(".dirty", "")
          .replace("-", ".")
          .replaceAfter("SNAPSHOT", "")
  description = "JavaFX FileChooser and DirectoryChooser adapter that can be used in a Swing application."
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(gradleApi())
}

gradlePlugin {
  plugins {
    create("module-finder") {
      id = "$group.module-finder"
      displayName = "Module Finder"
      implementationClass = "$group.gradle.modulefinder.ModuleFinderPlugin"
      description = "Enables the use of non-modular Java dependencies without an \"Automatic Module Name\" attribute in their manifest."
    }
  }
}

pluginBundle {
  website = "https://github.com/MrcJkb/gradle-module-finder"
  vcsUrl = "https:git@github.com:MrcJkb/gradle-module-finder.git"
  tags = listOf("java", "modularity", "jigsaw", "jpms", "automatic-module-name")
}

tasks.jar {
  val javaVersion = System.getProperty("java.version")
  val javaVendor = System.getProperty("java.vendor")
  val javaVmVersion = System.getProperty("java.vm.version")
  val osName = System.getProperty("os.name")
  val osArchitecture = System.getProperty("os.arch")
  val osVersion = System.getProperty("os.version")
  manifest {
    attributes["Library"] = rootProject.name
    attributes["Version"] = archiveVersion
    attributes["Website"] = "https://github.com/MrcJkb/gradle-module-finder"
    attributes["Built-By"] = System.getProperty("user.name")
    attributes["Build-Timestamp"] = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Date())
    attributes["Created-by"] = "Gradle ${gradle.gradleVersion}"
    attributes["Build-OS"] = "$osName $osArchitecture $osVersion"
    attributes["Build-Jdk"] = "$javaVersion ($javaVendor $javaVmVersion)"
    attributes["Build-OS"] = "$osName $osArchitecture $osVersion"
  }
}

tasks.compileJava {
  // Workaround for adding the src/kotlin classes to the java modulepath
  options.compilerArgs = listOf("--patch-module", "mrcjkb.jfxfilechooseradapter.impl=${sourceSets.main.get().output.asPath}")
}

val fakeJavadocJar by tasks.creating(Jar::class) {
  manifest {
    attributes["Info"] = "This is a Kotlin project and contains no Javadoc."
  }
  archiveClassifier.set("javadoc")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
  withSourcesJar()
  artifacts.add("archives", fakeJavadocJar)
}

val isReleaseVersion = !version.toString().endsWith("SNAPSHOT")

configurePublication(rootProject)

fun configurePublication(project: Project) {
  publishing {
    publications {
      create<MavenPublication>(project.name) {
        groupId = group.toString()
        artifactId = project.name
        version = version
        from(project.components["java"])
        project.tasks.findByName("fakeJavadocJar")?.let {
          // Add fake javadoc Jar for Kotlin projects.
          artifact(it)
        }
        versionMapping {
          usage("java-api") {
            fromResolutionOf("runtimeClasspath")
          }
          usage("java-runtime") {
            fromResolutionResult()
          }
        }
        pom {
          name.set(project.name)
          description.set(project.description)
          url.set("https://github.com/MrcJkb/gradle-module-finder/")
          developers() {
            developer {
              id.set("MrcJkb")
              name.set("Marc Jakobi")
            }
          }
          issueManagement {
            system.set("GitHub")
            url.set("https://github.com/MrcJkb/gradle-module-finder/issues")
          }
          scm {
            url.set("https://github.com/MrcJkb/gradle-module-finder/")
            connection.set("scm:git:git@github.com:MrcJkb/gradle-module-finder.git")
            developerConnection.set("scm:git:ssh://git@github.com:MrcJkb/gradle-module-finder.git")
          }
          licenses {
            license {
              name.set("GPLv2")
              url.set("https://github.com/MrcJkb/gradle-module-finder/blob/main/LICENSE")
              distribution.set("repo")
            }
          }
        }
      }
      repositories {
        maven {
          val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
          val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
          url = if (isReleaseVersion) releasesRepoUrl else snapshotsRepoUrl
          credentials {
            username = project.properties["ossrhUser"]?.toString() ?: "Unknown user"
            password = project.properties["ossrhPassword"]?.toString() ?: "Unknown password"
          }
        }
      }
    }
  }
  signing {
    if (isReleaseVersion) {
      sign(publishing.publications[project.name])
    }
  }
}