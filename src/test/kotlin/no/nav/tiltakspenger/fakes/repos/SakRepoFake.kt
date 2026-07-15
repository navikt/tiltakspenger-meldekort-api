package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.mottak.MottattSak
import no.nav.tiltakspenger.meldekort.sak.Sak
import no.nav.tiltakspenger.meldekort.sak.SakRepo

/**
 * In-memory speiling av [no.nav.tiltakspenger.meldekort.sak.infra.SakPostgresRepo].
 *
 * Lagrer kun sak-row-feltene internt, slik `sak`-tabellen gjør. `meldeperioder` og `meldekortvedtak` lagres i egne fakes og joines inn i [hent].
 *
 * For at join skal fungere må samme [meldeperiodeRepo]/[meldekortvedtakRepo]-instans deles med koden under test (typisk via en felles `TestApplicationContext`).
 */
class SakRepoFake(
    private val meldeperiodeRepo: MeldeperiodeRepoFake = MeldeperiodeRepoFake(),
    private val meldekortvedtakRepo: MeldekortvedtakRepoFake = MeldekortvedtakRepoFake(),
) : SakRepo {
    private val data = Atomic(mutableMapOf<SakId, Sak>())

    /** Eksponert for [BrukerSakRepoFake] slik at fakes deler underliggende state. */
    internal fun alleSaker(): Collection<Sak> = data.get().values

    /**
     * Skrivesiden eies av mottak ([no.nav.tiltakspenger.meldekort.mottak.MottakRepo]), kalles via [no.nav.tiltakspenger.fakes.repos.MottakRepoFake].
     */
    fun lagre(sak: MottattSak) {
        // Inserter kun sak-row; meldeperioder/meldekortvedtak persistes via egne repo-er.
        data.get()[sak.id] = Sak(
            id = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            meldeperioder = emptyList(),
            // Nylig mottatt sak har ennå ukjent arena-status; settes av egen jobb.
            arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
            harSoknadUnderBehandling = sak.harSoknadUnderBehandling,
            kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort,
            meldekortvedtak = emptyList(),
        )
    }

    /** Speiler [no.nav.tiltakspenger.meldekort.mottak.infra.MottakPostgresRepo.oppdaterSak]: oppdaterer kun fnr, harSoknadUnderBehandling og kanSendeInnHelgForMeldekort. */
    fun oppdater(sak: MottattSak) {
        data.get()[sak.id] = data.get()[sak.id]!!.copy(
            fnr = sak.fnr,
            harSoknadUnderBehandling = sak.harSoknadUnderBehandling,
            kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort,
        )
    }

    override fun oppdaterFnr(gammeltFnr: Fnr, nyttFnr: Fnr, sessionContext: SessionContext?) {
        data.get()
            .filterValues { it.fnr == gammeltFnr }
            .forEach { (sakId, sak) ->
                data.get()[sakId] = sak.copy(fnr = nyttFnr)
            }
    }

    override fun oppdaterArenaStatus(
        id: SakId,
        arenaStatus: ArenaMeldekortStatus,
        sessionContext: SessionContext?,
    ) {
        data.get()[id] = data.get()[id]!!.copy(arenaMeldekortStatus = arenaStatus)
    }

    /** Speiler [no.nav.tiltakspenger.meldekort.sak.infra.SakPostgresRepo.hent]: returnerer sak med joinede meldeperioder og meldekortvedtak. */
    override fun hent(
        id: SakId,
        sessionContext: SessionContext?,
    ): Sak? {
        return data.get()[id]?.copy(
            meldeperioder = meldeperiodeRepo.hentForSakId(id),
            meldekortvedtak = meldekortvedtakRepo.hentForSakId(id),
        )
    }

    /** Speiler [no.nav.tiltakspenger.meldekort.sak.infra.SakPostgresRepo.hentSakerUtenArenaStatus]: uten meldeperioder/meldekortvedtak. */
    override fun hentSakerUtenArenaStatus(sessionContext: SessionContext?): List<Sak> {
        return data.get().values.filter { it.arenaMeldekortStatus == ArenaMeldekortStatus.UKJENT }
    }
}
