package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.jupiter.api.Test
import java.nio.file.Path

class InfraImportKonsistTest {
    @Test
    fun `kun infra-pakker kan importere infra-pakker`() {
        fun hasInfraPackageSegment(packageName: String): Boolean =
            packageName.split('.').contains("infra")

        val violations =
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { file -> hasInfraPackageSegment(file.packagee?.name.orEmpty()) }
                .flatMap { file ->
                    val packageName = file.packagee?.name.orEmpty()
                    file.imports
                        .filter { import -> hasInfraPackageSegment(import.name) }
                        .map { import ->
                            InfraImportViolation(
                                sourceReference = "$packageName.${file.path.toFileNameWithoutExtension()}",
                                sourcePackage = packageName,
                                importName = import.name,
                                importLocation = import.location.toProjectRelativeLocation(),
                            )
                        }
                }

        withClue(violations.toFailureMessage()) {
            violations.shouldBeEmpty()
        }
    }

    private data class InfraImportViolation(
        val sourceReference: String,
        val sourcePackage: String,
        val importName: String,
        val importLocation: String,
    )

    private fun List<InfraImportViolation>.toFailureMessage(): String =
        if (isEmpty()) {
            "No infra import violations found."
        } else {
            "Only packages containing an 'infra' segment may import packages containing an 'infra' segment.\n" +
                "Found $size problematic infra import(s):\n" +
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
                    }
        }

    private fun String.toFileNameWithoutExtension(): String =
        Path.of(this).fileName.toString().removeSuffix(".kt")

    private fun String.toProjectRelativeLocation(): String {
        val projectPath = Path.of(System.getProperty("user.dir")).toString()
        return removePrefix("$projectPath/")
    }
}
