package no.nav.tiltakspenger.arkitektur

import com.lemonappdev.konsist.api.Konsist
import no.nav.tiltakspenger.libs.konsist.BoundaryKlasser
import no.nav.tiltakspenger.libs.konsist.DomeneImportWhitelist
import no.nav.tiltakspenger.libs.konsist.EnSetningPerLinje
import no.nav.tiltakspenger.libs.konsist.InfraImport
import no.nav.tiltakspenger.libs.konsist.IngenClockDefault
import no.nav.tiltakspenger.libs.konsist.IngenJUnit4
import no.nav.tiltakspenger.libs.konsist.IngenJackson2
import no.nav.tiltakspenger.libs.konsist.IngenJupiterAsserts
import no.nav.tiltakspenger.libs.konsist.IngenLocalDateTimeNow
import no.nav.tiltakspenger.libs.konsist.IngenLokaleJacksonMappere
import no.nav.tiltakspenger.libs.konsist.IngenNowUtenClock
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * Kjører de delte arkitekturreglene fra tiltakspenger-libs (`konsist-regler`) på dette repoet.
 */
class FellesArkitekturKonsistTest {
    @Test
    fun `produksjonskode bruker Jackson 3, ikke Jackson 2`() {
        IngenJackson2.assert(Konsist.scopeFromProduction())
    }

    @Test
    fun `testkode bruker JUnit 5, ikke JUnit 4`() {
        IngenJUnit4.assert(Konsist.scopeFromTest())
    }

    @Test
    fun `testkode bruker Kotest assertions, ikke Jupiter Assertions`() {
        IngenJupiterAsserts.assert(Konsist.scopeFromTest())
    }

    @Test
    fun `ingen lokale Jackson-mappere — bruk objectMapper fra libs-json`() {
        IngenLokaleJacksonMappere.assert(Konsist.scopeFromProject())
    }

    @Test
    fun `henter aldri nåtid uten Clock`() {
        IngenNowUtenClock.assert(Konsist.scopeFromProject())
    }

    @Test
    fun `bruk nå fra libs-common, ikke LocalDateTime-now`() {
        IngenLocalDateTimeNow.assert(Konsist.scopeFromProduction())
    }

    @Test
    fun `Clock-parametre har ikke default-verdi i produksjonskode`() {
        IngenClockDefault.assert(Konsist.scopeFromProduction())
    }

    @Test
    fun `kdoc og kommentarer har maks en setning per linje`() {
        EnSetningPerLinje.assertFlereSetningerIKommentarer(Konsist.scopeFromProject())
    }

    @Test
    fun `kdoc og kommentarer brekker ikke en setning over flere linjer`() {
        EnSetningPerLinje.assertBrukneSetningerIKommentarer(Konsist.scopeFromProject())
    }

    @Test
    fun `markdown-filer har maks en setning per linje`() {
        EnSetningPerLinje.assertFlereSetningerIMarkdown(repoRot())
    }

    @Test
    fun `markdown-filer brekker ikke en setning over flere linjer`() {
        EnSetningPerLinje.assertBrukneSetningerIMarkdown(repoRot())
    }

    @Test
    fun `kun infra-pakker kan importere infra-pakker`() {
        InfraImport.assert(Konsist.scopeFromProduction())
    }

    @Test
    fun `domene-filer importerer kun tillatte pakker`() {
        DomeneImportWhitelist.assert(
            scope = Konsist.scopeFromProduction(),
            erDomenepakke = { pakke -> pakke.startsWith("no.nav.tiltakspenger.meldekort") },
            tillattePakker = listOf(
                "arrow.core",
                "io.github.oshai.kotlinlogging",
                "java.time",
                "java.util",
                "kotlin",
                "no.nav.tiltakspenger.libs.common",
                "no.nav.tiltakspenger.libs.dato",
                "no.nav.tiltakspenger.libs.httpklient",
                "no.nav.tiltakspenger.libs.logging",
                "no.nav.tiltakspenger.libs.meldekort",
                "no.nav.tiltakspenger.libs.periode",
                "no.nav.tiltakspenger.libs.persistering.domene",
                "no.nav.tiltakspenger.meldekort",
            ),
        )
    }

    @Test
    fun `boundary-klasser ligger i infra-pakker`() {
        BoundaryKlasser.assert(Konsist.scopeFromProduction())
    }

    /** Enmodul-repo: testens arbeidskatalog er repo-rota. */
    private fun repoRot(): Path = Path.of(System.getProperty("user.dir"))
}
