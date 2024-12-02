import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val jvmVersion = JvmTarget.JVM_21
val mainClassFile = "no.nav.tiltakspenger.meldekort.ApplicationKt"

val ktorVersion = "3.0.1"
val mockkVersion = "1.13.13"
val felleslibVersion = "0.0.286"

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

plugins {
    application
    kotlin("jvm") version "2.0.21"
    id("com.diffplug.spotless") version "6.25.0"
    id("com.github.ben-manes.versions") version "0.51.0"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

application {
    mainClass.set(mainClassFile)
}

dependencies {
    // <3
    implementation("io.arrow-kt:arrow-core:1.2.4")

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("org.jetbrains:annotations:26.0.1")
    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")

    // Felles libs
    implementation("com.github.navikt.tiltakspenger-libs:auth-core:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:auth-ktor:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:json:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:persistering-infrastruktur:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:persistering-domene:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:jobber:$felleslibVersion")

    // DB
    implementation("org.flywaydb:flyway-database-postgresql:11.0.0")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.github.seratch:kotliquery:1.9.0")

    // Http
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-http:$ktorVersion")

    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-utils:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    // Test
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.mockk:mockk-dsl-jvm:$mockkVersion")
    testImplementation("org.skyscreamer:jsonassert:1.5.3")
}

spotless {
    kotlin {
        ktlint()
            .editorConfigOverride(
                mapOf(
                    "ktlint_standard_max-line-length" to "off",
                ),
            )
    }
}

tasks {
    dependencyUpdates.configure {
        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }

    compileKotlin {
        compilerOptions {
            jvmTarget.set(jvmVersion)
        }
    }

    compileTestKotlin {
        compilerOptions {
            jvmTarget.set(jvmVersion)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }

    test {
        // JUnit 5 support
        useJUnitPlatform()
        // https://phauer.com/2018/best-practices-unit-testing-kotlin/
        systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
        // https://github.com/mockito/mockito/issues/3037#issuecomment-1588199599
        jvmArgs("-XX:+EnableDynamicAgentLoading")
    }

    jar {
        dependsOn(configurations.runtimeClasspath)

        manifest {
            attributes["Main-Class"] = mainClassFile
            attributes["Class-Path"] =
                configurations.runtimeClasspath
                    .get()
                    .joinToString(separator = " ") { file -> file.name }
        }
    }

    register<Copy>("gitHooks") {
        from(file(".scripts/pre-commit"))
        into(file(".git/hooks"))
    }

    build {
        dependsOn("gitHooks")
    }

    register("checkFlywayMigrationNames") {
        doLast {
            val migrationDir = project.file("app/src/main/resources/db/migration")
            val invalidFiles =
                migrationDir
                    .walk()
                    .filter { it.isFile && it.extension == "sql" }
                    .filterNot { it.name.matches(Regex("V[0-9]+__[\\w]+\\.sql")) }
                    .map { it.name }
                    .toList()

            if (invalidFiles.isNotEmpty()) {
                throw GradleException("Invalid migration filenames:\n${invalidFiles.joinToString("\n")}")
            } else {
                println("All migration filenames are valid.")
            }
        }
    }

    check {
        dependsOn("checkFlywayMigrationNames")
    }
}
