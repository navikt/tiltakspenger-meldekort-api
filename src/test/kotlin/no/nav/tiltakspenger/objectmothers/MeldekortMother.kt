package no.nav.tiltakspenger.objectmothers

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
    ): Meldekort {
        val meldeperiode =
            ObjectMother.meldeperiode(periode = periode, saksnummer = saksnummer, sakId = sakId, fnr = fnr)

        return Meldekort(
            id = MeldekortId.random(),
            mottatt = mottatt,
            meldeperiode = meldeperiode,
            dager = meldeperiode.girRett.map { (dag, _) ->
                MeldekortDag(
                    dag = dag,
                    status = statusMap[dag] ?: MeldekortDagStatus.IKKE_REGISTRERT,
                )
            },
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
                status = if (meldeperiode.girRett[dag] == true) MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET else MeldekortDagStatusDTO.IKKE_REGISTRERT,
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
