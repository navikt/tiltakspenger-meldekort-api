package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagFraBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatusDTO
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR
import java.time.LocalDate
import java.time.LocalDateTime

interface MeldekortMother {

    fun meldekortAlleDagerGirRett(
        periode: Periode = ObjectMother.periode(),
        mottatt: LocalDateTime? = nå(fixedClock),
        deaktivert: LocalDateTime? = null,
        saksnummer: String = Math.random().toString(),
        sakId: SakId = SakId.random(),
        statusMap: Map<LocalDate, MeldekortDagStatus> = emptyMap(),
        varselId: VarselId? = null,
        fnr: Fnr = Fnr.fromString(FAKE_FNR),
        erVarselInaktivert: Boolean = false,
        maksAntallDagerForPeriode: Int = 10,
    ): Meldekort {
        return meldekort(
            periode = periode,
            mottatt = mottatt,
            deaktivert = deaktivert,
            saksnummer = saksnummer,
            sakId = sakId,
            statusMap = statusMap,
            varselId = varselId,
            fnr = fnr,
            erVarselInaktivert = erVarselInaktivert,
            meldeperiode = ObjectMother.meldeperiode(
                periode = periode,
                saksnummer = saksnummer,
                sakId = sakId,
                fnr = fnr,
                girRett = periode.tilDager().associateWith { true },
                antallDagerForPeriode = maksAntallDagerForPeriode,
            ),
        )
    }

    fun meldekort(
        periode: Periode = ObjectMother.periode(),
        mottatt: LocalDateTime? = nå(fixedClock),
        deaktivert: LocalDateTime? = null,
        saksnummer: String = Math.random().toString(),
        sakId: SakId = SakId.random(),
        statusMap: Map<LocalDate, MeldekortDagStatus> = emptyMap(),
        varselId: VarselId? = null,
        fnr: Fnr = Fnr.fromString(FAKE_FNR),
        erVarselInaktivert: Boolean = false,
        meldeperiode: Meldeperiode = ObjectMother.meldeperiode(
            periode = periode,
            saksnummer = saksnummer,
            sakId = sakId,
            fnr = fnr,
        ),
    ): Meldekort {
        val dager = meldeperiode.girRett.map { (dag, girRett) ->
            MeldekortDag(
                dag = dag,
                status = statusMap[dag] ?: if (girRett) {
                    MeldekortDagStatus.IKKE_BESVART
                } else {
                    MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
                },
            )
        }

        return Meldekort(
            id = MeldekortId.random(),
            mottatt = mottatt,
            meldeperiode = meldeperiode,
            dager = dager,
            journalpostId = null,
            journalføringstidspunkt = null,
            varselId = varselId,
            erVarselInaktivert = erVarselInaktivert,
            deaktivert = deaktivert,
        )
    }

    fun lagreMeldekortFraBrukerKommando(
        meldeperiode: Meldeperiode,
        meldekortId: MeldekortId = MeldekortId.random(),
        mottatt: LocalDateTime = nå(fixedClock),
        dager: List<MeldekortDagFraBrukerDTO> = meldeperiode.girRett.map { (dag, _) ->
            MeldekortDagFraBrukerDTO(
                dag = dag,
                status = if (meldeperiode.girRett[dag] == true) MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET else MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER,
            )
        },
    ): LagreMeldekortFraBrukerKommando {
        return LagreMeldekortFraBrukerKommando(
            id = meldekortId,
            mottatt = nå(fixedClock),
            fnr = meldeperiode.fnr,
            dager = dager,
        )
    }
}

fun TestApplicationContext.lagMeldekort(meldeperiode: Meldeperiode, mottatt: LocalDateTime? = null): Meldekort {
    val meldekort = ObjectMother.meldekort(
        meldeperiode = meldeperiode,
        mottatt = mottatt,
        fnr = meldeperiode.fnr,
        periode = meldeperiode.periode,
        sakId = meldeperiode.sakId,
        saksnummer = meldeperiode.saksnummer,
    )

    meldeperiodeRepo.lagre(meldekort.meldeperiode)
    meldekortRepo.lagre(meldekort)

    return meldekort
}

fun lagMeldekortFraBrukerKommando(
    meldekort: Meldekort,
    fnr: Fnr = meldekort.fnr,
): LagreMeldekortFraBrukerKommando {
    return LagreMeldekortFraBrukerKommando(
        id = meldekort.id,
        mottatt = nå(fixedClock),
        fnr = fnr,
        dager = meldekort.dager.map {
            MeldekortDagFraBrukerDTO(
                dag = it.dag,
                status = if (meldekort.meldeperiode.girRett[it.dag] == true) MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET else MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER,
            )
        },
    )
}
