package no.nav.tiltakspenger.meldekort.meldekortvedtak.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.lagreMeldekortvedtak
import no.nav.tiltakspenger.lagreMeldeperiode
import no.nav.tiltakspenger.lagreSak
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.infrastruktur.sqlQuery
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldeperiodebehandling
import no.nav.tiltakspenger.meldekort.meldekortvedtak.MeldeperiodebehandlingDag
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Reduksjon
import no.nav.tiltakspenger.meldekort.sak.Sak
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MeldekortvedtakPostgresRepoTest {

    @Test
    fun `lagrer og henter meldekortvedtak`() {
        withMigratedDb(runIsolated = false) { helper ->
            val periode1 = ObjectMother.periode()
            val periode2 = periode1.plus14Dager()
            val sak = ObjectMother.sak(fnr = helper.nesteFnr())
            helper.lagreSak(sak)

            val vedtak = Meldekortvedtak(
                id = VedtakId.random(),
                sakId = sak.id,
                opprettet = nå(fixedClock),
                erKorrigering = false,
                erAutomatiskBehandlet = false,
                meldeperiodebehandlinger = listOf(
                    helper.lagreMeldeperiodeOgByggBehandling(
                        sak = sak,
                        periode = periode1,
                        dager = listOf(
                            MeldeperiodebehandlingDag(
                                dato = periode1.fraOgMed,
                                status = MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                                reduksjon = Reduksjon.INGEN_REDUKSJON,
                                beløp = 500,
                                beløpBarnetillegg = 0,
                            ),
                        ),
                    ),
                    helper.lagreMeldeperiodeOgByggBehandling(
                        sak = sak,
                        periode = periode2,
                        medBrukersMeldekort = true,
                        dager = listOf(
                            MeldeperiodebehandlingDag(
                                dato = periode2.fraOgMed,
                                status = MeldekortDagStatus.FRAVÆR_SYK,
                                reduksjon = Reduksjon.REDUKSJON,
                                beløp = 250,
                                beløpBarnetillegg = 50,
                            ),
                        ),
                    ),
                ),
            )

            helper.lagreMeldekortvedtak(vedtak, null)

            val hentet = helper.sessionFactory.withSession { session ->
                MeldekortvedtakPostgresRepo.hentForSakId(sak.id, session)
            }

            hentet shouldBe listOf(vedtak)
        }
    }

    @Test
    fun `hentForSakId returnerer tom liste for sak uten vedtak`() {
        withMigratedDb(runIsolated = false) { helper ->
            val sak = ObjectMother.sak(fnr = helper.nesteFnr())
            helper.lagreSak(sak)

            val hentet = helper.sessionFactory.withSession { session ->
                MeldekortvedtakPostgresRepo.hentForSakId(sak.id, session)
            }

            hentet shouldBe emptyList()
        }
    }

    @Test
    fun `to vedtak for samme sak sorteres etter opprettet`() {
        withMigratedDb(runIsolated = false) { helper ->
            val periode = ObjectMother.periode()
            val sak = ObjectMother.sak(fnr = helper.nesteFnr())
            helper.lagreSak(sak)

            val behandling = helper.lagreMeldeperiodeOgByggBehandling(
                sak = sak,
                periode = periode,
                dager = listOf(
                    MeldeperiodebehandlingDag(
                        dato = periode.fraOgMed,
                        status = MeldekortDagStatus.IKKE_TILTAKSDAG,
                        reduksjon = Reduksjon.YTELSEN_FALLER_BORT,
                        beløp = 0,
                        beløpBarnetillegg = 0,
                    ),
                ),
            )

            val tidligereVedtak = Meldekortvedtak(
                id = VedtakId.random(),
                sakId = sak.id,
                opprettet = nå(fixedClock),
                erKorrigering = false,
                erAutomatiskBehandlet = true,
                meldeperiodebehandlinger = listOf(behandling),
            )

            val seinereVedtak = Meldekortvedtak(
                id = VedtakId.random(),
                sakId = sak.id,
                opprettet = nå(fixedClock).plusSeconds(60),
                erKorrigering = true,
                erAutomatiskBehandlet = false,
                meldeperiodebehandlinger = listOf(
                    behandling.copy(
                        dager = listOf(
                            MeldeperiodebehandlingDag(
                                dato = periode.fraOgMed,
                                status = MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                                reduksjon = Reduksjon.INGEN_REDUKSJON,
                                beløp = 500,
                                beløpBarnetillegg = 0,
                            ),
                        ),
                    ),
                ),
            )

            helper.lagreMeldekortvedtak(tidligereVedtak, null)
            helper.lagreMeldekortvedtak(seinereVedtak, null)

            val hentet = helper.sessionFactory.withSession { session ->
                MeldekortvedtakPostgresRepo.hentForSakId(sak.id, session)
            }

            hentet shouldBe listOf(tidligereVedtak, seinereVedtak)
        }
    }

    @Test
    fun `lagring av samme vedtak to ganger er idempotent`() {
        withMigratedDb(runIsolated = false) { helper ->
            val periode = ObjectMother.periode()
            val sak = ObjectMother.sak(fnr = helper.nesteFnr())
            helper.lagreSak(sak)

            val vedtak = Meldekortvedtak(
                id = VedtakId.random(),
                sakId = sak.id,
                opprettet = nå(fixedClock),
                erKorrigering = false,
                erAutomatiskBehandlet = false,
                meldeperiodebehandlinger = listOf(
                    helper.lagreMeldeperiodeOgByggBehandling(
                        sak = sak,
                        periode = periode,
                        dager = listOf(
                            MeldeperiodebehandlingDag(
                                dato = periode.fraOgMed,
                                status = MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                                reduksjon = Reduksjon.INGEN_REDUKSJON,
                                beløp = 500,
                                beløpBarnetillegg = 0,
                            ),
                        ),
                    ),
                ),
            )

            helper.lagreMeldekortvedtak(vedtak, null)
            helper.lagreMeldekortvedtak(vedtak, null)

            val hentet = helper.sessionFactory.withSession { session ->
                MeldekortvedtakPostgresRepo.hentForSakId(sak.id, session)
            }

            hentet shouldBe listOf(vedtak)
        }
    }

    @Test
    fun `skriver behandlingene til meldeperiodebehandling-tabellen`() {
        withMigratedDb(runIsolated = false) { helper ->
            val periode1 = ObjectMother.periode()
            val periode2 = periode1.plus14Dager()
            val sak = ObjectMother.sak(fnr = helper.nesteFnr())
            helper.lagreSak(sak)

            val behandling1 = helper.lagreMeldeperiodeOgByggBehandling(
                sak = sak,
                periode = periode1,
                dager = listOf(
                    MeldeperiodebehandlingDag(
                        dato = periode1.fraOgMed,
                        status = MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                        reduksjon = Reduksjon.INGEN_REDUKSJON,
                        beløp = 500,
                        beløpBarnetillegg = 0,
                    ),
                ),
            )
            val behandling2 = helper.lagreMeldeperiodeOgByggBehandling(
                sak = sak,
                periode = periode2,
                medBrukersMeldekort = true,
                dager = emptyList(),
            )

            val vedtak = Meldekortvedtak(
                id = VedtakId.random(),
                sakId = sak.id,
                opprettet = nå(fixedClock),
                erKorrigering = false,
                erAutomatiskBehandlet = false,
                meldeperiodebehandlinger = listOf(behandling1, behandling2),
            )

            helper.lagreMeldekortvedtak(vedtak, null)

            val rader = helper.sessionFactory.withSession { session ->
                session.run(
                    sqlQuery(
                        """
                        SELECT meldeperiode_kjede_id, meldeperiode_id, sak_id, brukers_meldekort_id
                        FROM meldeperiodebehandling
                        WHERE meldekortvedtak_id = :id
                        ORDER BY fra_og_med
                        """,
                        "id" to vedtak.id.toString(),
                    ).map { row ->
                        listOf(
                            row.string("meldeperiode_kjede_id"),
                            row.string("meldeperiode_id"),
                            row.string("sak_id"),
                            row.stringOrNull("brukers_meldekort_id"),
                        )
                    }.asList,
                )
            }

            rader shouldBe listOf(
                listOf(
                    behandling1.meldeperiodeKjedeId.toString(),
                    behandling1.meldeperiodeId.toString(),
                    sak.id.toString(),
                    null,
                ),
                listOf(
                    behandling2.meldeperiodeKjedeId.toString(),
                    behandling2.meldeperiodeId.toString(),
                    sak.id.toString(),
                    behandling2.brukersMeldekortId.toString(),
                ),
            )
        }
    }

    /**
     * Lagrer en meldeperiode (kreves av fremmednøkkelen meldeperiodebehandling.meldeperiode_id) og bygger en [Meldeperiodebehandling] som peker på den.
     *
     * Når [medBrukersMeldekort] er true lagres også et brukers meldekort for meldeperioden, og behandlingens brukersMeldekortId settes til dette meldekortet (kreves av fremmednøkkelen meldeperiodebehandling.brukers_meldekort_id).
     */
    private fun TestDataHelper.lagreMeldeperiodeOgByggBehandling(
        sak: Sak,
        periode: Periode,
        medBrukersMeldekort: Boolean = false,
        dager: List<MeldeperiodebehandlingDag>,
    ): Meldeperiodebehandling {
        val meldeperiode = ObjectMother.meldeperiode(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            periode = periode,
            opprettet = nå(fixedClock),
        )
        lagreMeldeperiode(meldeperiode)

        val brukersMeldekortId = if (medBrukersMeldekort) {
            val meldekort = ObjectMother.meldekort(meldeperiode = meldeperiode)
            meldekortPostgresRepo.lagre(meldekort)
            meldekort.id
        } else {
            null
        }

        return Meldeperiodebehandling(
            meldeperiodeId = meldeperiode.id,
            meldeperiodeKjedeId = meldeperiode.kjedeId,
            brukersMeldekortId = brukersMeldekortId,
            periode = periode,
            dager = dager,
        )
    }
}
