import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

val mainClassFile = "no.nav.tiltakspenger.meldekort.infra.ApplicationKt"

val ktorVersion = "3.4.3"
val mockkVersion = "1.14.11"
val felleslibVersion = "0.0.20260723155827"
val kotestVersion = "6.2.2"
val kotlinxCoroutinesVersion = "1.11.0"
val tmsVarselBuilderVersion = "2.2.0"
val tmsMikrofrontendSelectorBuilderVersion = "3.0.0"
val testContainersVersion = "2.0.5"

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

plugins {
    application
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.10"
    id("com.diffplug.spotless") version "8.8.0"
    id("com.github.ben-manes.versions") version "0.54.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.8"
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

configurations.all {
    // ekskluder JUnit 4
    exclude(group = "junit", module = "junit")
}

dependencies {
    // <3
    implementation("io.arrow-kt:arrow-core:2.2.3")

    // Lås versjonene på alle Kotlin-komponenter til samme versjon
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))

    // Lås alle io.netty:* til samme versjon. r2dbc-postgresql/reactor-netty (transitiv via
    // persistering-infrastruktur) drar inn netty 4.1.x, mens ktor-server-netty bruker 4.2.x.
    // Uten dette havner både netty-codec (4.1) og netty-codec-base (4.2) på classpath med
    // duplikate baseklasser (ByteToMessageDecoder m.fl.), som med `-cp lib/*` lastes i feil
    // rekkefølge og brekker HTTP-pipelinen.
    implementation(platform("io.netty:netty-bom:4.2.16.Final"))
    implementation("ch.qos.logback:logback-classic:1.5.38")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("org.jetbrains:annotations:26.1.0")
    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")

    // Felles libs
    implementation("com.github.navikt.tiltakspenger-libs:common:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:jobber:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:json:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:kafka:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:logging:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:meldekort:${felleslibVersion}")
    implementation("com.github.navikt.tiltakspenger-libs:meldekort-dtos:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:periodisering:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:persistering-infrastruktur:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:persistering-domene:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:texas:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:httpklient-infrastruktur:$felleslibVersion")
    implementation("com.github.navikt.tiltakspenger-libs:ktor-common:$felleslibVersion")

    implementation("no.nav.tms.varsel:kotlin-builder:$tmsVarselBuilderVersion")
    implementation("no.nav.tms.mikrofrontend.selector:builder:$tmsMikrofrontendSelectorBuilderVersion")

    // DB
    implementation("org.flywaydb:flyway-database-postgresql:12.10.0")
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("com.github.seratch:kotliquery:1.9.1")

    // Http
    implementation("io.ktor:ktor-http:$ktorVersion")

    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("io.ktor:ktor-utils:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson3:$ktorVersion")

    // Auth
    api("com.auth0:java-jwt:4.6.0")
    api("com.auth0:jwks-rsa:0.24.1")

    // Test
    testImplementation(platform("org.junit:junit-bom:6.1.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.mockk:mockk-dsl-jvm:$mockkVersion")

    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions:$kotestVersion")
    // Delte arkitekturregler; drar inn konsist transitivt (api-avhengighet). Egen versjon inntil felleslibVersion bumpes forbi httpklient-splitten.
    testImplementation("com.github.navikt.tiltakspenger-libs:konsist-regler:$felleslibVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$kotlinxCoroutinesVersion")

    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")

    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:testcontainers-postgresql:$testContainersVersion")

    testImplementation("com.github.navikt.tiltakspenger-libs:test-common:$felleslibVersion")
    testImplementation(testFixtures("com.github.navikt.tiltakspenger-libs:httpklient-infrastruktur:$felleslibVersion"))
    testImplementation("com.github.navikt.tiltakspenger-libs:ktor-test-common:$felleslibVersion")
    testImplementation("com.github.navikt.tiltakspenger-libs:persistering-test-common:$felleslibVersion")

}

spotless {
    kotlin {
        ktlint()
            .editorConfigOverride(
                mapOf(
                    "ktlint_standard_max-line-length" to "off",
                    "ktlint_standard_function-signature" to "disabled",
                    "ktlint_standard_function-expression-body" to "disabled",
                ),
            )
    }
}

kover {
    reports {
        total {
            filters {
                includes {
                    classes(
                        "no.nav.tiltakspenger.meldekort.**.*PostgresRepo*",
                        "no.nav.tiltakspenger.meldekort.arena.**",
                        "no.nav.tiltakspenger.meldekort.journalføring.infra.DokarkivClientImpl",
                        "no.nav.tiltakspenger.meldekort.journalføring.infra.PdfgenClientImpl",
                        "no.nav.tiltakspenger.meldekort.bruker.**",
                        "no.nav.tiltakspenger.meldekort.landingsside.**",
                        "no.nav.tiltakspenger.meldekort.meldekortvedtak.**",
                        "no.nav.tiltakspenger.meldekort.microfrontend.**",
                        "no.nav.tiltakspenger.meldekort.mottak.**",
                        "no.nav.tiltakspenger.meldekort.sak.**",
                    )
                }
            }
            html {
                onCheck = true
            }
            xml {
                onCheck = true
            }
            verify {
                onCheck = true
                rule("postgres repos, arena, bruker, landingsside, meldekortvedtak, microfrontend, mottak and sak packages have full line coverage") {
                    bound {
                        minValue = 100
                        coverageUnits = CoverageUnit.LINE
                        aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                    }
                }
            }
        }
    }
}

tasks.named("koverXmlReport") {
    val xmlReport = layout.buildDirectory.file("reports/kover/report.xml")
    doLast {
        val xml = xmlReport.get().asFile
        val classCount = xml.readText().split("<class ").size - 1
        if (classCount == 0) throw GradleException("Kover report contains no classes — include filters likely stale")
    }
}

tasks {
    dependencyUpdates.configure {
        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }

    kotlin {
        jvmToolchain(25)
        compilerOptions {
            freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
            freeCompilerArgs.add("-Xwarning-level=IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE:disabled")
        }
    }

    test {
        // JUnit 5-støtte
        useJUnitPlatform()
        // https://phauer.com/2018/best-practices-unit-testing-kotlin/
        systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
        // https://github.com/mockito/mockito/issues/3037#issuecomment-1588199599
        jvmArgs("-XX:+EnableDynamicAgentLoading")
        jvmArgs("-ea")
        testLogging {
            // Vi logger bare feilede og hoppede tester når Gradle kjører.
            events("skipped", "failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    register<Copy>("gitHooks") {
        group = "git hooks"
        description = "Installerer git-hooks fra .gitHooks/ til .git/hooks/."
        from(file(".gitHooks"))
        into(file(".git/hooks"))
        filePermissions { unix("rwxr-xr-x") }
    }
    // Kan stenges gracefully ned med:  pkill -TERM -f tiltakspenger.LokalMainKt
    register<JavaExec>("runLokalt") {
        group = "application"
        description = "Kjører LokalMain (test-source-settet) lokalt med postgres i docker og fakes."
        // LokalMain ligger i test-source-settet, så vi bruker test-runtime-classpath (ikke main).
        mainClass.set("no.nav.tiltakspenger.LokalMainKt")
        classpath = sourceSets["test"].runtimeClasspath
        standardInput = System.`in`
        // Graceful shutdown gir exit-kode fra signalet (SIGTERM=143, SIGINT/Ctrl+C=130). Det er normalt og skal ikke
        // markere tasken som FAILED. Vi godtar derfor disse, men lar ekte krasj (andre koder) fortsatt feile.
        isIgnoreExitValue = true
        val execResult = executionResult
        doLast {
            val exitValue = execResult.get().exitValue
            val gracefulShutdownKoder = setOf(0, 130, 143)
            if (exitValue !in gracefulShutdownKoder) {
                throw GradleException("runLokalt avsluttet med uventet exit-kode $exitValue.")
            }
        }
    }

    build {
        dependsOn("gitHooks")
    }

    register("verifyMainClassHasMain") {
        // Sikrer at klassen vi peker mainClass på faktisk har en `fun main()`,
        // slik at deploy-feil ala "Error: Main method not found" fanges opp i build, ikke i prod.
        // NB: Hele logikken må være inline i doLast for å være kompatibel med configuration cache
        // (kall til script-level funksjoner serialiserer en referanse til Gradle-scriptobjektet).
        val mainClass = mainClassFile
        val expectedPackage = mainClass.substringBeforeLast('.', missingDelimiterValue = "")
        val expectedSimpleName = mainClass.substringAfterLast('.')
        val packageDir = file("src/main/kotlin/${expectedPackage.replace('.', '/')}")
        doLast {
            if (!packageDir.isDirectory) {
                throw GradleException("Fant ikke pakke-mappa $packageDir for mainClass $mainClass.")
            }

            val jvmNameRegex = Regex("""@file:JvmName\(\s*"([^"]+)"\s*\)""")
            val mainFunRegex = Regex("""\bfun\s+main\s*\(""")

            val matchingFile = packageDir.listFiles { f -> f.isFile && f.extension == "kt" }
                .orEmpty()
                .firstOrNull { kt ->
                    val text = kt.readText()
                    if (!mainFunRegex.containsMatchIn(text)) return@firstOrNull false
                    val jvmName = jvmNameRegex.find(text)?.groupValues?.get(1)
                    val facadeName = jvmName ?: "${kt.nameWithoutExtension}Kt"
                    facadeName == expectedSimpleName
                }

            if (matchingFile == null) {
                throw GradleException(
                    "Fant ingen .kt-fil i $packageDir som inneholder `fun main(` og kompilerer til $mainClass. " +
                        "Forventet enten en fil ved navn ${expectedSimpleName.removeSuffix("Kt")}.kt " +
                        "(uten `@file:JvmName`) eller en fil med `@file:JvmName(\"$expectedSimpleName\")`. " +
                        "Dette ville feilet ved deploy.",
                )
            }
        }
    }

    named("classes") {
        dependsOn("verifyMainClassHasMain")
    }

    register("checkFlywayMigrationNames") {
        val migrationDir = project.file("src/main/resources/db/migration")
        doLast {
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


