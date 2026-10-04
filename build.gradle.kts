import io.papermc.paperweight.checkstyle.PaperCheckstyleExt
import io.papermc.paperweight.checkstyle.tasks.MergeCheckstyleConfigs
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("io.papermc.paperweight.patcher") version "2.0.0-beta.24"
}

paperweight {
    upstreams.paper {
        ref = providers.gradleProperty("paperRef")

        patchFile {
            path = "paper-server/build.gradle.kts"
            outputFile = file("reed-server/build.gradle.kts")
            patchFile = file("reed-server/build.gradle.kts.patch")
        }
        patchFile {
            path = "paper-api/build.gradle.kts"
            outputFile = file("reed-api/build.gradle.kts")
            patchFile = file("reed-api/build.gradle.kts.patch")
        }
        patchDir("reedPaperApi") {
            upstreamPath = "paper-api"
            excludes = setOf("build.gradle.kts")
            patchesDir = file("reed-api/paper-patches")
            outputDir = file("paper-api")
        }
        patchDir("paperCheckstyle") {
            upstreamPath = "paper-checkstyle"
            patchesDir = file("build-data/paper-checkstyle-patches")
            outputDir = file("paper-checkstyle")
        }
        patchDir("paperCheckstyleConfig") {
            upstreamPath = ".checkstyle"
            patchesDir = file("build-data/paper-checkstyle-config-patches")
            outputDir = file(".checkstyle")
        }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    if (name != "reed-server") {
        apply(plugin = "io.papermc.paperweight.paper-checkstyle")
        extensions.configure<PaperCheckstyleExt> {
            typeUseAnnotationsFile.set(rootProject.layout.projectDirectory.file(".checkstyle/type_use_annotations.txt"))
        }
    }
    if (name == "reed-api") {
        tasks.withType<MergeCheckstyleConfigs>().configureEach {
            overrideConfigFile.set(rootProject.layout.projectDirectory.file("paper-api/.checkstyle/checkstyle.xml"))
        }
    }
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.release = 25
        options.isFork = true
        options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-removal"))
    }
    tasks.withType<Javadoc>().configureEach {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources>().configureEach {
        filteringCharset = Charsets.UTF_8.name()
    }
    tasks.withType<Test>().configureEach {
        testLogging {
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events(TestLogEvent.STANDARD_OUT)
        }
    }

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
    }

    extensions.configure<PublishingExtension> {
        repositories {
            maven("https://artifactory.papermc.io/artifactory/releases/") {
                name = "paperReleases"
                credentials(PasswordCredentials::class)
            }
        }
    }
}
