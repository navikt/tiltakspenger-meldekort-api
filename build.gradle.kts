val javaVersion = JavaVersion.VERSION_21
val mockkVersion = "1.13.8"
val ktorVersion = "2.3.7"

plugins {
    application
    kotlin("jvm") version "1.9.21"
    // id("ca.cutterslade.analyze") version "1.9.1"
    id("com.diffplug.spotless") version "6.23.3"
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(kotlin("stdlib"))
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("org.jetbrains:annotations:24.1.0")
    // implementation("com.github.navikt:rapids-and-rivers:2022112407251669271100.df879df951cf")
    implementation("com.natpryce:konfig:1.6.10.0")

    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.mockk:mockk-dsl-jvm:$mockkVersion")
    testImplementation("org.skyscreamer:jsonassert:1.5.1")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-forwarded-header:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-http:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-utils:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")

    // DB
    implementation("org.flywaydb:flyway-core:10.2.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.3.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("com.github.seratch:kotliquery:1.9.0")
}

configurations.all {
    // exclude JUnit 4
    exclude(group = "junit", module = "junit")
}

application {
    mainClass.set("no.nav.tiltakspenger.meldekort.api.ApplicationKt")
}

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

spotless {
    kotlin {
        ktlint("0.48.2")
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = javaVersion.toString()
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = javaVersion.toString()
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
    test {
        // JUnit 5 support
        useJUnitPlatform()
        // https://phauer.com/2018/best-practices-unit-testing-kotlin/
        systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
    }
    /*
    analyzeClassesDependencies {
        warnUsedUndeclared = true
        warnUnusedDeclared = true
    }
    analyzeTestClassesDependencies {
        warnUsedUndeclared = true
        warnUnusedDeclared = true
    }
     */
}
