package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.fakes.TEXAS_FAKE_FNR
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.VarselId
import java.time.LocalDate
import java.time.LocalDateTime

interface MeldekortMother {
    fun meldekort(
        periode: Periode = ObjectMother.periode(),
        mottatt: LocalDateTime? = nå(fixedClock),
        saksnummer: String = Math.random().toString(),
        statusMap: Map<LocalDate, MeldekortDagStatus> = emptyMap(),
        varselId: VarselId? = null,
        fnr: Fnr = Fnr.fromString(TEXAS_FAKE_FNR),
        erVarselInaktivert: Boolean = false,
    ): Meldekort {
        val meldeperiode = ObjectMother.meldeperiode(periode = periode, saksnummer = saksnummer, fnr = fnr)

        return Meldekort(
            id = MeldekortId.random(),
            mottatt = mottatt,
            meldeperiode = meldeperiode,
            sakId = SakId.random(),
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
        )
    }
}
