package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.streams.asSequence

/**
 * Håndhever konvensjonen om én setning per linje i KDoc, kommentarer og markdown-filer.
 * Se «KDoc og kommentarer: én setning per linje» i AGENTS-backend.md i monorepo-rota.
 *
 * Regelen her dekker den ene halvdelen av konvensjonen: en linje skal ikke inneholde mer enn én setning.
 * Deteksjonen er en heuristikk: en setningsavslutter (`.`, `!`, `?`) etterfulgt av mellomrom og stor forbokstav tolkes som starten på en ny setning.
 * Norske/engelske forkortelser, tall (listepunkter, datoer) og innhold i backticks/kodeblokker er unntatt.
 */
class EnSetningPerLinjeKonsistTest {
    @Test
    fun `kdoc og kommentarer har maks en setning per linje`() {
        val violations =
            Konsist
                .scopeFromProject()
                .files
                .flatMap { file ->
                    file.text
                        .lines()
                        .toKommentarlinjer()
                        .filter { kommentarlinje -> kommentarlinje.tekst.harFlereSetninger() }
                        .map { kommentarlinje -> "${file.path.toProjectRelativeLocation()}:${kommentarlinje.linjenummer}: ${kommentarlinje.tekst}" }
                }

        withClue(violations.toFailureMessage(kilde = "KDoc/kommentarer")) {
            violations.shouldBeEmpty()
        }
    }

    @Test
    fun `markdown-filer har maks en setning per linje`() {
        val moduleRoot = Path.of(System.getProperty("user.dir"))
        val violations =
            moduleRoot
                .markdownFiler()
                .flatMap { markdownFil ->
                    markdownFil
                        .readLines()
                        .toMarkdownProsalinjer()
                        .filter { markdownlinje -> markdownlinje.tekst.harFlereSetninger() }
                        .map { markdownlinje -> "${moduleRoot.relativize(markdownFil)}:${markdownlinje.linjenummer}: ${markdownlinje.tekst}" }
                }.toList()

        withClue(violations.toFailureMessage(kilde = "markdown")) {
            violations.shouldBeEmpty()
        }
    }

    private data class Tekstlinje(
        val linjenummer: Int,
        val tekst: String,
    )

    /**
     * Trekker ut kommentarinnholdet (KDoc, blokkommentarer og `//`-kommentarer) fra kildekodelinjer.
     * Linjer inne i kodeblokker (```) i kommentarer hoppes over.
     */
    private fun List<String>.toKommentarlinjer(): List<Tekstlinje> {
        val kommentarlinjer = mutableListOf<Tekstlinje>()
        var iBlokkommentar = false
        var iKodeblokk = false

        forEachIndexed { index, linje ->
            val linjenummer = index + 1
            val trimmet = linje.trim()

            val kommentartekst =
                when {
                    iBlokkommentar -> {
                        if (trimmet.contains("*/")) iBlokkommentar = false
                        trimmet.substringBefore("*/").removePrefix("*").trim()
                    }

                    trimmet.startsWith("/*") -> {
                        if (!trimmet.contains("*/")) iBlokkommentar = true
                        trimmet
                            .removePrefix("/**")
                            .removePrefix("/*")
                            .substringBefore("*/")
                            .trim()
                    }

                    else -> linje.tekstEtterLinjekommentar()
                }

            if (kommentartekst == null) {
                return@forEachIndexed
            }
            if (kommentartekst.startsWith("```")) {
                iKodeblokk = !iKodeblokk
                return@forEachIndexed
            }
            if (!iKodeblokk && kommentartekst.isNotEmpty()) {
                kommentarlinjer.add(Tekstlinje(linjenummer, kommentartekst))
            }
        }
        return kommentarlinjer
    }

    /**
     * Finner innholdet etter `//` på en kodelinje, eller null hvis linjen ikke har noen linjekommentar.
     * `//` inne i strengliteraler (typisk URL-er) filtreres bort med en enkel heuristikk: et oddetall anførselstegn foran, eller `:` rett foran.
     */
    private fun String.tekstEtterLinjekommentar(): String? {
        var searchFrom = 0
        while (true) {
            val index = indexOf("//", searchFrom)
            if (index == -1) return null
            val insideString = take(index).count { char -> char == '"' } % 2 == 1
            val partOfUrl = index > 0 && this[index - 1] == ':'
            if (!insideString && !partOfUrl) {
                return substring(index + 2).trim()
            }
            searchFrom = index + 2
        }
    }

    /**
     * Trekker ut prosalinjene fra en markdown-fil.
     * Kodeblokker (```) og tabellinjer hoppes over.
     */
    private fun List<String>.toMarkdownProsalinjer(): List<Tekstlinje> {
        val prosalinjer = mutableListOf<Tekstlinje>()
        var iKodeblokk = false

        forEachIndexed { index, linje ->
            val trimmet = linje.trim()
            if (trimmet.startsWith("```")) {
                iKodeblokk = !iKodeblokk
                return@forEachIndexed
            }
            if (iKodeblokk || trimmet.startsWith("|") || trimmet.isEmpty()) {
                return@forEachIndexed
            }
            prosalinjer.add(Tekstlinje(index + 1, trimmet))
        }
        return prosalinjer
    }

    /**
     * Heuristikk for om en linje inneholder mer enn én setning.
     * Flagger setningsavslutter etterfulgt av mellomrom og stor forbokstav, med unntak for forkortelser, tall og innhold i backticks.
     */
    private fun String.harFlereSetninger(): Boolean {
        val tekstUtenKodespenn = replace(inlineKodespennRegex, "kode")
        return setningsskilleRegex.findAll(tekstUtenKodespenn).any { match ->
            val avslutter = tekstUtenKodespenn[match.range.first]
            val ordFørAvslutter =
                tekstUtenKodespenn
                    .take(match.range.first)
                    .trimEnd { char -> char in avsluttendeTegn }
                    .takeLastWhile { char -> char.isLetterOrDigit() || char == '.' }
                    .trimStart('.')
                    .lowercase()

            when {
                ordFørAvslutter.isEmpty() -> false
                avslutter == '.' && ordFørAvslutter in forkortelser -> false
                avslutter == '.' && ordFørAvslutter.all { char -> char.isDigit() } -> false
                avslutter == '!' && ordFørAvslutter in interjeksjoner -> false
                else -> true
            }
        }
    }

    private fun Path.markdownFiler(): Sequence<Path> =
        Files
            .walk(this)
            .asSequence()
            .filter { path -> path.extension == "md" }
            .filterNot { path ->
                val relativePath = relativize(path).toString()
                ekskluderteKataloger.any { katalog -> relativePath == katalog || relativePath.startsWith("$katalog/") }
            }

    private fun List<String>.toFailureMessage(kilde: String): String =
        if (isEmpty()) {
            "Ingen brudd på én-setning-per-linje i $kilde."
        } else {
            "Skriv én setning per linje i $kilde (se AGENTS-backend.md i monorepo-rota).\n" +
                "Fant $size linje(r) med flere setninger:\n" +
                joinToString("\n") { violation -> "- $violation" }
        }

    private fun String.toProjectRelativeLocation(): String {
        val projectPath = Path.of(System.getProperty("user.dir")).toString()
        return removePrefix("$projectPath/")
    }

    private companion object {
        /** Setningsavslutter, eventuelle avsluttende tegn (sitat/parentes/utheving), mellomrom og stor forbokstav. */
        private val setningsskilleRegex = Regex("""[.!?]["'`»)\]*_]*\s+\p{Lu}""")

        /** Tegn som kan stå inntil setningsavslutteren uten å være del av ordet (sitat/parentes/utheving). */
        private val avsluttendeTegn = "\"'`»)]*_"

        private val inlineKodespennRegex = Regex("""`[^`]+`""")

        private val forkortelser =
            setOf(
                "f.eks", "bl.a", "dvs", "osv", "ca", "jf", "nr", "evt", "e.l", "o.l",
                "mv", "m.m", "m.fl", "pga", "ift", "iht", "mht", "inkl", "hhv",
                "e.g", "i.e", "etc", "vs",
            )

        private val interjeksjoner = setOf("nb", "obs")

        private val ekskluderteKataloger = setOf("build", ".gradle", ".git", ".idea", "node_modules")
    }
}
