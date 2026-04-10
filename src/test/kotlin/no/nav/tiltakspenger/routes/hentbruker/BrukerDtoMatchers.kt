package no.nav.tiltakspenger.routes.hentbruker

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.clients.utils.toNorskUkenummer
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.domene.BrukerDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatusDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagTilBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode.Companion.kanFyllesUtFraOgMed
import java.time.DayOfWeek
import java.time.LocalDateTime

fun BrukerDTO.UtenSak.shouldBe(
    arenaMeldekortStatus: ArenaMeldekortStatusDTO = ArenaMeldekortStatusDTO.UKJENT,
) {
    this shouldBe BrukerDTO.UtenSak(
        arenaMeldekortStatus = arenaMeldekortStatus,
    )
    this.harSak.shouldBeFalse()
}

/**
 * Sammenligner alle felter på [BrukerDTO.MedSak].
 *
 * For [nesteMeldekort] og [forrigeMeldekort] kopieres genererte felter fra actual:
 * - id (meldekort-ID): generert server-side, kopieres fra actual
 * - innsendt: sjekkes for null/ikke-null, men eksakt tidspunkt kopieres fra actual
 *
 * Null i expected skal alltid matche null i actual (og vice versa).
 */
fun BrukerDTO.MedSak.shouldBe(
    nesteMeldekort: MeldekortTilBrukerDTO? = null,
    forrigeMeldekort: MeldekortTilBrukerDTO? = null,
    arenaMeldekortStatus: ArenaMeldekortStatusDTO = ArenaMeldekortStatusDTO.UKJENT,
    harSoknadUnderBehandling: Boolean = false,
    kanSendeInnHelgForMeldekort: Boolean = false,
) {
    this shouldBe BrukerDTO.MedSak(
        nesteMeldekort = nesteMeldekort?.kopierGenererteFelterFra(this.nesteMeldekort),
        forrigeMeldekort = forrigeMeldekort?.kopierGenererteFelterFra(this.forrigeMeldekort),
        arenaMeldekortStatus = arenaMeldekortStatus,
        harSoknadUnderBehandling = harSoknadUnderBehandling,
        kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
    )
    this.harSak.shouldBeTrue()
}

/**
 * Genererer en [MeldekortTilBrukerDTO] med fornuftige standardverdier basert på [periode] og [meldeperiodeId].
 *
 * @param id: default tom streng (ignoreres i sammenligning via [kopierGenererteFelterFra])
 * @param kjedeId: utledet fra [periode]
 * @param uke1: utledes fra [periode]
 * @param uke2: utledes fra [periode]
 * @param status: [MeldekortStatusDTO.KAN_UTFYLLES]
 * @param maksAntallDager: 10
 * @param innsendt: null
 * @param dager: generert fra [periode] med hverdager=[MeldekortDagStatusDTO.IKKE_BESVART], helg=[MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER]
 * @param kanSendes: utledet fra [Periode.kanFyllesUtFraOgMed]
 */
fun forventetMeldekort(
    id: String = "",
    meldeperiodeId: MeldeperiodeId,
    periode: Periode,
    uke1: Int = periode.fraOgMed.toNorskUkenummer(),
    uke2: Int = periode.tilOgMed.toNorskUkenummer(),
    kjedeId: String = "${periode.fraOgMed}/${periode.tilOgMed}",
    versjon: Int = 1,
    status: MeldekortStatusDTO = MeldekortStatusDTO.KAN_UTFYLLES,
    maksAntallDager: Int = 10,
    innsendt: LocalDateTime? = null,
    dager: List<MeldekortDagTilBrukerDTO> = forventedeDager(periode),
    kanSendes: LocalDateTime? = periode.kanFyllesUtFraOgMed(),
): MeldekortTilBrukerDTO {
    return MeldekortTilBrukerDTO(
        id = id,
        meldeperiodeId = meldeperiodeId.toString(),
        kjedeId = kjedeId,
        versjon = versjon,
        periode = periode,
        uke1 = uke1,
        uke2 = uke2,
        status = status,
        maksAntallDager = maksAntallDager,
        innsendt = innsendt,
        dager = dager,
        kanSendes = kanSendes,
    )
}

/**
 * Genererer forventede [MeldekortDagTilBrukerDTO] for alle dager i [periode].
 *
 * @param hverdagStatus status for mandag–fredag (default: [MeldekortDagStatusDTO.IKKE_BESVART])
 * @param helgStatus status for lørdag–søndag (default: [MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER])
 */
fun forventedeDager(
    periode: Periode,
    hverdagStatus: MeldekortDagStatusDTO = MeldekortDagStatusDTO.IKKE_BESVART,
    helgStatus: MeldekortDagStatusDTO = MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER,
): List<MeldekortDagTilBrukerDTO> {
    require(periode.antallDager == 14L)
    require(periode.fraOgMed.dayOfWeek == DayOfWeek.MONDAY)
    require(periode.tilOgMed.dayOfWeek == DayOfWeek.SUNDAY)
    return periode.tilDager().map { dag ->
        MeldekortDagTilBrukerDTO(
            dag = dag,
            status = if (dag.dayOfWeek.value <= 5) hverdagStatus else helgStatus,
        )
    }
}

/**
 * Kopierer genererte felter fra [actual] til this (expected) for sammenligning.
 * - id (meldekort-ID): generert server-side, kopieres fra actual
 * - innsendt: sjekkes for null/ikke-null, men eksakt tidspunkt kopieres fra actual
 */
private fun MeldekortTilBrukerDTO.kopierGenererteFelterFra(actual: MeldekortTilBrukerDTO?): MeldekortTilBrukerDTO {
    actual shouldNotBe null
    if (this.innsendt == null) {
        actual!!.innsendt shouldBe null
    } else {
        actual!!.innsendt shouldNotBe null
    }
    return this.copy(
        id = actual.id,
        innsendt = actual.innsendt,
    )
}
