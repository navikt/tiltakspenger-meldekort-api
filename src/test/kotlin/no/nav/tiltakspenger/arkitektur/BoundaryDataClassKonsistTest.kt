package no.nav.tiltakspenger.arkitektur

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

class BoundaryDataClassKonsistTest {
    @Test
    fun `boundary-klasser ligger i infra-pakker`() {
        val boundaryNameRegex = Regex("\\b(?:data\\s+)?(?:class|interface|enum\\s+class)\\s+\\w*(?:DTO|Dto|Request|Response|DbJson)\\b")
        val productionRoot = Path.of(System.getProperty("user.dir"), "src/main/kotlin")

        val violations = Files.walk(productionRoot).use { paths ->
            paths
                .filter { path: Path -> path.isRegularFile() && path.toString().endsWith(".kt") }
                .toList()
        }.mapNotNull { path ->
            val content = path.readText()
            val declarations = boundaryNameRegex.findAll(content).map { match -> match.value }.toList()
            val packageName = content.lineSequence().firstOrNull { line -> line.startsWith("package ") }.orEmpty()

            if (declarations.isNotEmpty() && !packageName.contains(".infra")) {
                BoundaryClassViolation(
                    file = path.toProjectRelativeLocation(),
                    declarations = declarations,
                )
            } else {
                null
            }
        }

        withClue(violations.toFailureMessage()) {
            violations.shouldBeEmpty()
        }
    }

    private data class BoundaryClassViolation(
        val file: String,
        val declarations: List<String>,
    )

    private fun List<BoundaryClassViolation>.toFailureMessage(): String =
        if (isEmpty()) {
            "No boundary data class placement violations found."
        } else {
            "Boundary classes ending with DTO, Dto, Request, Response or DbJson must be placed in infra packages.\n" +
                "Found $size violation(s):\n" +
                joinToString("\n") { violation ->
                    "- ${violation.file}\n" +
                        violation.declarations.joinToString("\n") { declaration -> "  declaration: $declaration" }
                }
        }

    private fun Path.toProjectRelativeLocation(): String {
        val projectPath = Path.of(System.getProperty("user.dir"))
        return projectPath.relativize(this).toString()
    }
}
