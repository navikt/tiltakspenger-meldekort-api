package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readLines
import kotlin.streams.asSequence

/**
 * Håndhever konvensjonen om én setning per linje i KDoc, kommentarer og markdown-filer.
 * Se «KDoc og kommentarer: én setning per linje» i AGENTS-backend.md i monorepo-rota.
 *
 * Konvensjonen har to halvdeler, og begge håndheves:
 * en linje skal ikke inneholde mer enn én setning, og en setning skal ikke brekkes over flere linjer.
 *
 * Deteksjonen er heuristikker.
 * Flere setninger på én linje flagges når en setningsavslutter (`.`, `!`, `?`) etterfølges av mellomrom og stor forbokstav.
 * Brukket setning flagges når en fortsettelseslinje starter med liten bokstav og forrige linje i samme avsnitt ikke er avsluttet.
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

        withClue(violations.toFailureMessage(intro = "Skriv én setning per linje i KDoc/kommentarer")) {
            violations.shouldBeEmpty()
        }
    }

    @Test
    fun `kdoc og kommentarer brekker ikke en setning over flere linjer`() {
        val violations =
            Konsist
                .scopeFromProject()
                .files
                .flatMap { file ->
                    file.text
                        .lines()
                        .toKommentarlinjer()
                        .finnBrukneSetninger()
                        .map { kommentarlinje -> "${file.path.toProjectRelativeLocation()}:${kommentarlinje.linjenummer}: ${kommentarlinje.tekst}" }
                }

        withClue(violations.toFailureMessage(intro = "Ikke brekk en setning over flere linjer i KDoc/kommentarer — slå sammen med forrige linje")) {
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

        withClue(violations.toFailureMessage(intro = "Skriv én setning per linje i markdown")) {
            violations.shouldBeEmpty()
        }
    }

    @Test
    fun `markdown-filer brekker ikke en setning over flere linjer`() {
        val moduleRoot = Path.of(System.getProperty("user.dir"))
        val violations =
            moduleRoot
                .markdownFiler()
                .flatMap { markdownFil ->
                    markdownFil
                        .readLines()
                        .toMarkdownProsalinjer()
                        .finnBrukneSetninger()
                        .map { markdownlinje -> "${moduleRoot.relativize(markdownFil)}:${markdownlinje.linjenummer}: ${markdownlinje.tekst}" }
                }.toList()

        withClue(violations.toFailureMessage(intro = "Ikke brekk en setning over flere linjer i markdown — slå sammen med forrige linje")) {
            violations.shouldBeEmpty()
        }
    }

    private data class Tekstlinje(
        val linjenummer: Int,
        val tekst: String,
        val gruppe: Int,
    )

    /**
     * Trekker ut kommentarinnholdet (KDoc, blokkommentarer og `//`-kommentarer) fra kildekodelinjer.
     * Linjer inne i kodeblokker (```) i kommentarer hoppes over.
     * [Tekstlinje.gruppe] identifiserer sammenhengende avsnitt: blanke linjer, kodeblokker og bytte av kommentarblokk starter en ny gruppe.
     */
    private fun List<String>.toKommentarlinjer(): List<Tekstlinje> {
        val kommentarlinjer = mutableListOf<Tekstlinje>()
        var iBlokkommentar = false
        var iKodeblokk = false
        var gruppe = 0

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

            when {
                kommentartekst == null -> gruppe++

                kommentartekst.startsWith("```") -> {
                    iKodeblokk = !iKodeblokk
                    gruppe++
                }

                iKodeblokk -> {}

                kommentartekst.isEmpty() -> gruppe++

                else -> kommentarlinjer.add(Tekstlinje(linjenummer, kommentartekst, gruppe))
            }
            if (trimmet.contains("*/")) {
                gruppe++
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
     * [Tekstlinje.gruppe] identifiserer sammenhengende avsnitt: blanke linjer, kodeblokker og tabeller starter en ny gruppe.
     */
    private fun List<String>.toMarkdownProsalinjer(): List<Tekstlinje> {
        val prosalinjer = mutableListOf<Tekstlinje>()
        var iKodeblokk = false
        var gruppe = 0

        forEachIndexed { index, linje ->
            val trimmet = linje.trim()
            if (trimmet.startsWith("```")) {
                iKodeblokk = !iKodeblokk
                gruppe++
                return@forEachIndexed
            }
            if (iKodeblokk || trimmet.startsWith("|") || trimmet.isEmpty()) {
                gruppe++
                return@forEachIndexed
            }
            prosalinjer.add(Tekstlinje(index + 1, trimmet, gruppe))
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

    /**
     * Heuristikk for setninger som er brukket over flere linjer.
     * Flagger en fortsettelseslinje som starter med liten bokstav når forrige linje i samme avsnitt ikke er avsluttet.
     * Markørlinjer (listepunkter, KDoc-tags, overskrifter) starter aldri med liten bokstav og er dermed automatisk unntatt.
     */
    private fun List<Tekstlinje>.finnBrukneSetninger(): List<Tekstlinje> =
        zipWithNext().mapNotNull { (forrige, denne) ->
            denne.takeIf {
                denne.gruppe == forrige.gruppe &&
                    denne.tekst.first().isLowerCase() &&
                    forrige.tekst.erUavsluttet()
            }
        }

    /** En linje regnes som avsluttet når siste tegn (etter stripping av avsluttende tegn) er setningsavslutter eller kolon. */
    private fun String.erUavsluttet(): Boolean {
        val sisteTegn = trimEnd { char -> char in avsluttendeTegn || char.isWhitespace() }.lastOrNull() ?: return false
        return sisteTegn !in ".!?:"
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

    private fun List<String>.toFailureMessage(intro: String): String =
        if (isEmpty()) {
            "Ingen brudd på én-setning-per-linje."
        } else {
            "$intro (se AGENTS-backend.md i monorepo-rota).\n" +
                "Fant $size linje(r) som bryter regelen:\n" +
                joinToString("\n") { violation -> "- $violation" }
        }

    private fun String.toProjectRelativeLocation(): String {
        val projectPath = Path.of(System.getProperty("user.dir")).toString()
        return removePrefix("$projectPath/")
    }

    private companion object {
        /** Setningsavslutter, eventuelle avsluttende tegn (sitat/parentes/utheving), mellomrom, eventuelle innledende tegn og stor forbokstav. */
        private val setningsskilleRegex = Regex("""[.!?]["'`»)\]*_]*\s+[\[("«]*\p{Lu}""")

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
