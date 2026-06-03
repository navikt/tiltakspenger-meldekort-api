package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendStatus
import no.nav.tiltakspenger.meldekort.sak.Sak
import no.nav.tiltakspenger.meldekort.sak.SakRepo
import java.time.Clock

/**
 * In-memory speiling av [no.nav.tiltakspenger.meldekort.sak.infra.SakPostgresRepo].
 *
 * Lagrer kun "sak-row"-feltene internt (id, saksnummer, fnr, arenaMeldekortStatus,
 * harSoknadUnderBehandling, kanSendeInnHelgForMeldekort, microfrontendStatus) — akkurat slik
 * `sak`-tabellen i Postgres gjør. `meldeperioder` og `meldekortvedtak` lagres i sine egne fakes
 * og joines inn i [hent] (som tilsvarer `MeldeperiodePostgresRepo.hentForSakId`/
 * `MeldekortvedtakPostgresRepo.hentForSakId`-joinene i [no.nav.tiltakspenger.meldekort.sak.infra.SakPostgresRepo.hent]).
 *
 * For at `join` skal fungere må den samme [meldeperiodeRepo]/[meldekortvedtakRepo]-instansen
 * deles med koden under test (typisk via en felles `TestApplicationContext`).
 */
class SakRepoFake(
    private val meldeperiodeRepo: MeldeperiodeRepoFake = MeldeperiodeRepoFake(),
    private val meldekortvedtakRepo: MeldekortvedtakRepoFake = MeldekortvedtakRepoFake(),
) : SakRepo {
    private val data = Atomic(mutableMapOf<SakId, Sak>())
    private val microfrontendStatus = Atomic(mutableMapOf<SakId, MicrofrontendStatus>())

    /** Eksponert for [BrukerSakRepoFake] slik at fakes deler underliggende state. */
    internal fun alleSaker(): Collection<Sak> = data.get().values

    override fun lagre(
        sak: Sak,
        sessionContext: SessionContext?,
    ) {
        // SakPostgresRepo.lagre() inserter kun sak-row, ikke meldeperioder/meldekortvedtak
        // (de persistes via egne repo-er). Vi speiler det her.
        data.get()[sak.id] = sak.copy(meldeperioder = emptyList(), meldekortvedtak = emptyList())
        microfrontendStatus.get()[sak.id] = MicrofrontendStatus.UBEHANDLET
    }

    /** Speiler [no.nav.tiltakspenger.meldekort.sak.infra.SakPostgresRepo.oppdater]: oppdaterer kun fnr, harSoknadUnderBehandling og kanSendeInnHelgForMeldekort. */
    override fun oppdater(
        sak: Sak,
        sessionContext: SessionContext?,
    ) {
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

    override fun oppdaterStatusForMicrofrontend(sakId: SakId, aktiv: Boolean, sessionContext: SessionContext?) {
        microfrontendStatus.get()[sakId] = if (aktiv) MicrofrontendStatus.AKTIV else MicrofrontendStatus.INAKTIV
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

    /**
     * Speiler [no.nav.tiltakspenger.meldekort.sak.infra.SakPostgresRepo.hentSakerHvorMicrofrontendSkalAktiveres]:
     * status != AKTIV OG eksisterer en meldeperiode for saken hvor minst én dag i girRett er true,
     * OG (opprettet > offset eller tilOgMed > offset). Offset = nå - 1 måned.
     */
    override fun hentSakerHvorMicrofrontendSkalAktiveres(sessionContext: SessionContext?): List<Sak> {
        throw NotImplementedError("Postgresversjonen er feil. Anbefaler og fikse den først")
    }

    /**
     * Speiler [no.nav.tiltakspenger.meldekort.sak.infra.SakPostgresRepo.hentSakerHvorMicrofrontendSkalInaktiveres]:
     * status != INAKTIV OG IKKE eksisterer en meldeperiode for saken som matcher samme exists-vilkår.
     */
    override fun hentSakerHvorMicrofrontendSkalInaktiveres(sessionContext: SessionContext?): List<Sak> {
        throw NotImplementedError("Postgresversjonen er feil. Anbefaler og fikse den først")
    }
}
