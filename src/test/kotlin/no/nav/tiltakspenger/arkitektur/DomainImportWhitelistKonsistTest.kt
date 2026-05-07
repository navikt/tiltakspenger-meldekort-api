package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.jupiter.api.Test
import java.nio.file.Path

class DomainImportWhitelistKonsistTest {
    @Test
    fun `domene-filer importerer kun tillatte pakker`() {
        val allowedImportPackages =
            listOf(
                "arrow.core",
                "io.github.oshai.kotlinlogging",
                "java.time",
                "java.util",
                "kotlin",
                "no.nav.tiltakspenger.libs.common",
                "no.nav.tiltakspenger.libs.dato",
                "no.nav.tiltakspenger.libs.meldekort",
                "no.nav.tiltakspenger.libs.periode",
                "no.nav.tiltakspenger.libs.persistering.domene",
                "no.nav.tiltakspenger.meldekort",
            )

        val violations =
            Konsist
                .scopeFromProduction()
                .files
                .filter { file -> file.packagee?.name?.isDomainPackage() == true }
                .flatMap { file ->
                    val packageName = file.packagee?.name.orEmpty()
                    file.imports
                        .filterNot { import -> import.name.isAllowedDomainImport(allowedImportPackages) }
                        .map { import ->
                            DomainImportViolation(
                                sourceReference = "$packageName.${file.path.toFileNameWithoutExtension()}",
                                sourcePackage = packageName,
                                importName = import.name,
                                importLocation = import.location.toProjectRelativeLocation(),
                            )
                        }
                }

        withClue(violations.toFailureMessage(allowedImportPackages)) {
            violations.shouldBeEmpty()
        }
    }

    private data class DomainImportViolation(
        val sourceReference: String,
        val sourcePackage: String,
        val importName: String,
        val importLocation: String,
    )

    private fun String.isDomainPackage(): Boolean =
        startsWith("no.nav.tiltakspenger.meldekort") && !hasInfraPackageSegment()

    private fun String.isAllowedDomainImport(allowedImportPackages: List<String>): Boolean =
        !hasInfraPackageSegment() && allowedImportPackages.any { allowedPackage -> isSamePackageOrSubPackageOf(allowedPackage) }

    private fun String.hasInfraPackageSegment(): Boolean =
        split('.').contains("infra")

    private fun String.isSamePackageOrSubPackageOf(packageName: String): Boolean =
        this == packageName || startsWith("$packageName.")

    private fun List<DomainImportViolation>.toFailureMessage(allowedImportPackages: List<String>): String =
        if (isEmpty()) {
            "No domain import whitelist violations found."
        } else {
            "Domain files may only import packages from the explicit allowlist.\n" +
                "Found $size problematic import(s):\n" +
                groupBy { violation -> violation.sourceReference to violation.sourcePackage }
                    .entries
                    .joinToString("\n") { (source, violations) ->
                        val (sourceReference, sourcePackage) = source
                        "- $sourceReference\n" +
                            "  package: $sourcePackage\n" +
                            violations.joinToString("\n") { violation ->
                                "  imports: ${violation.importName}\n" +
                                    "  location: ${violation.importLocation}"
                            }
                    } +
                "\nAllowed import packages:\n" +
                allowedImportPackages.joinToString("\n") { allowedPackage -> "- $allowedPackage.*" }
        }

    private fun String.toFileNameWithoutExtension(): String =
        Path.of(this).fileName.toString().removeSuffix(".kt")

    private fun String.toProjectRelativeLocation(): String {
        val projectPath = Path.of(System.getProperty("user.dir")).toString()
        return removePrefix("$projectPath/")
    }
}
