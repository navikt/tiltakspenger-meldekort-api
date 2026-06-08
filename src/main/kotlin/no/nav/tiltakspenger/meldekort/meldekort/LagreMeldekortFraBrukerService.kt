package no.nav.tiltakspenger.meldekort.meldekort

import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.meldeperiode.MeldeperiodeRepo
import no.nav.tiltakspenger.meldekort.varsler.SakVarselRepo
import java.time.Clock

class LagreMeldekortFraBrukerService(
    private val meldekortRepo: MeldekortRepo,
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val sakVarselRepo: SakVarselRepo,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    fun lagreMeldekortFraBruker(kommando: LagreMeldekortFraBrukerKommando) {
        val meldekortId = kommando.id
        val innsenderFnr = kommando.fnr
        val meldekort = meldekortRepo.hentForMeldekortId(meldekortId, innsenderFnr)!!

        require(meldekort.mottatt == null) {
            "Meldekort med id $meldekortId er allerede mottatt (${meldekort.mottatt})"
        }
        val sisteMeldeperiode = meldeperiodeRepo.hentSisteMeldeperiodeForMeldeperiodeKjedeId(
            id = meldekort.meldeperiode.kjedeId,
            fnr = innsenderFnr,
        )!!
        require(meldekort.meldeperiode.id == sisteMeldeperiode.id) {
            "Meldekortets meldeperiode-id (${meldekort.meldeperiode.id}) må være den siste meldeperioden (${sisteMeldeperiode.id})"
        }

        val utfyltMeldekort = meldekort.fyllUtMeldekortFraBruker(
            brukerutfylteDager = kommando.dager,
            locale = kommando.locale,
            clock = clock,
        )
        sessionFactory.withTransactionContext { tx ->
            meldekortRepo.lagre(utfyltMeldekort, tx)

            // Flagg saken for varselvurdering - den asynkrone jobben håndterer selve varselet
            sakVarselRepo.flaggForVarselvurdering(utfyltMeldekort.sakId, tx)
        }
    }
}
