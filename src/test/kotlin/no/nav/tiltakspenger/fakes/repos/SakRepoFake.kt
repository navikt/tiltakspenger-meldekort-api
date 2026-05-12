package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendStatus
import no.nav.tiltakspenger.meldekort.sak.ArenaMeldekortStatus
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
 * `MeldekortvedtakPostgresRepo.hentForSakId`-joinene i [SakPostgresRepo.hent]).
 *
 * For at joinet skal fungere må den samme [meldeperiodeRepo]/[meldekortvedtakRepo]-instansen
 * deles med koden under test (typisk via en felles `TestApplicationContext`).
 */
class SakRepoFake(
    private val clock: Clock,
    private val meldeperiodeRepo: MeldeperiodeRepoFake = MeldeperiodeRepoFake(),
    private val meldekortvedtakRepo: MeldekortvedtakRepoFake = MeldekortvedtakRepoFake(),
) : SakRepo {
    private val data = Atomic(mutableMapOf<SakId, Sak>())
    private val microfrontendStatus = Atomic(mutableMapOf<SakId, MicrofrontendStatus>())

    override fun lagre(
        sak: Sak,
        sessionContext: SessionContext?,
    ) {
        // SakPostgresRepo.lagre() inserter kun sak-row, ikke meldeperioder/meldekortvedtak
        // (de persistes via egne repo-er). Vi speiler det her.
        data.get()[sak.id] = sak.copy(meldeperioder = emptyList(), meldekortvedtak = emptyList())
        microfrontendStatus.get()[sak.id] = MicrofrontendStatus.UBEHANDLET
    }

    /** Speiler [SakPostgresRepo.oppdater]: oppdaterer kun fnr, harSoknadUnderBehandling og kanSendeInnHelgForMeldekort. */
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

    /** Speiler [SakPostgresRepo.hent]: returnerer sak med joinede meldeperioder og meldekortvedtak. */
    override fun hent(
        id: SakId,
        sessionContext: SessionContext?,
    ): Sak? {
        return data.get()[id]?.copy(
            meldeperioder = meldeperiodeRepo.hentForSakId(id),
            meldekortvedtak = meldekortvedtakRepo.hentForSakId(id),
        )
    }

    /** Speiler [SakPostgresRepo.hentForBruker]: returnerer sak UTEN meldeperioder/meldekortvedtak. */
    override fun hentForBruker(
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): Sak? {
        return data.get().values.find { it.fnr == fnr }
    }

    /** Speiler [SakPostgresRepo.hentSakerUtenArenaStatus]: uten meldeperioder/meldekortvedtak. */
    override fun hentSakerUtenArenaStatus(sessionContext: SessionContext?): List<Sak> {
        return data.get().values.filter { it.arenaMeldekortStatus == ArenaMeldekortStatus.UKJENT }
    }

    /**
     * Speiler [SakPostgresRepo.hentSakerHvorMicrofrontendSkalAktiveres]:
     * status != AKTIV OG eksisterer en meldeperiode for saken hvor minst én dag i girRett er true,
     * OG (opprettet > offset eller tilOgMed > offset). Offset = nå - 1 måned.
     */
    override fun hentSakerHvorMicrofrontendSkalAktiveres(sessionContext: SessionContext?): List<Sak> {
        val offset = nå(clock).minusMonths(MICROFRONTEND_OFFSET_MONTHS)
        return data.get().values
            .filter { (microfrontendStatus.get()[it.id] ?: MicrofrontendStatus.UBEHANDLET) != MicrofrontendStatus.AKTIV }
            .filter { sak -> harMeldeperiodeMedRettInnenforOffset(sak.id, offset) }
    }

    /**
     * Speiler [SakPostgresRepo.hentSakerHvorMicrofrontendSkalInaktiveres]:
     * status != INAKTIV OG IKKE eksisterer en meldeperiode for saken som matcher samme exists-vilkår.
     */
    override fun hentSakerHvorMicrofrontendSkalInaktiveres(sessionContext: SessionContext?): List<Sak> {
        val offset = nå(clock).minusMonths(MICROFRONTEND_OFFSET_MONTHS)
        return data.get().values
            .filter { (microfrontendStatus.get()[it.id] ?: MicrofrontendStatus.UBEHANDLET) != MicrofrontendStatus.INAKTIV }
            .filterNot { sak -> harMeldeperiodeMedRettInnenforOffset(sak.id, offset) }
    }

    /** Test-hjelpemetode for å verifisere microfrontend-status (Postgres-lagrer den i sak-raden). */
    fun hentMicrofrontendStatus(sakId: SakId): MicrofrontendStatus =
        microfrontendStatus.get()[sakId] ?: MicrofrontendStatus.UBEHANDLET

    private fun harMeldeperiodeMedRettInnenforOffset(
        sakId: SakId,
        offset: java.time.LocalDateTime,
    ): Boolean {
        return meldeperiodeRepo.hentForSakId(sakId).any { mp ->
            mp.minstEnDagGirRettIPerioden &&
                (mp.opprettet.isAfter(offset) || mp.periode.tilOgMed.isAfter(offset.toLocalDate()))
        }
    }

    private companion object {
        const val MICROFRONTEND_OFFSET_MONTHS = 1L
    }
}
