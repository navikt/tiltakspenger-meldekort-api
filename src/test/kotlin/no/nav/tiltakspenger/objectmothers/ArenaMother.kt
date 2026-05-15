package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.arena.ArenaFravaerType
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekort
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortOversikt
import java.time.LocalDate
import kotlin.random.Random

interface ArenaMother {

    fun arenaMeldekort(
        periode: Periode,
        erTiltakspengerMeldekort: Boolean = true,
        meldekortId: Long = Random.nextLong(),
        kortType: String = "",
        meldeperiode: String = "${periode.fraOgMed}-${periode.tilOgMed}",
        fraDato: LocalDate = periode.fraOgMed,
        tilDato: LocalDate = periode.tilOgMed,
        hoyesteMeldegruppe: String = if (erTiltakspengerMeldekort) "INDIV" else "ARBS",
        beregningstatus: String = "",
        forskudd: Boolean = false,
        mottattDato: LocalDate? = null,
        bruttoBelop: Float = 0F,
    ): ArenaMeldekort = ArenaMeldekort(
        meldekortId = meldekortId,
        kortType = kortType,
        meldeperiode = meldeperiode,
        fraDato = fraDato,
        tilDato = tilDato,
        hoyesteMeldegruppe = hoyesteMeldegruppe,
        beregningstatus = beregningstatus,
        forskudd = forskudd,
        mottattDato = mottattDato,
        bruttoBelop = bruttoBelop,
    )

    fun arenaMeldekortOversikt(
        meldekortListe: List<ArenaMeldekort>? = null,
        personId: Long = 1L,
        etternavn: String = "Testesen",
        fornavn: String = "Test",
        maalformkode: String = "",
        meldeform: String = "",
        antallGjenstaaendeFeriedager: Int? = 0,
        fravaerListe: List<ArenaFravaerType>? = null,
    ): ArenaMeldekortOversikt = ArenaMeldekortOversikt(
        personId = personId,
        etternavn = etternavn,
        fornavn = fornavn,
        maalformkode = maalformkode,
        meldeform = meldeform,
        antallGjenstaaendeFeriedager = antallGjenstaaendeFeriedager,
        meldekortListe = meldekortListe,
        fravaerListe = fravaerListe,
    )
}
