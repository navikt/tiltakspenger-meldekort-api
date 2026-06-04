package no.nav.tiltakspenger.meldekort.meldekortvedtak.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.lagreMeldekortvedtak
import no.nav.tiltakspenger.lagreSak
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldeperiodebehandling
import no.nav.tiltakspenger.meldekort.meldekortvedtak.MeldeperiodebehandlingDag
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Reduksjon
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
                    Meldeperiodebehandling(
                        meldeperiodeId = MeldeperiodeId.random(),
                        meldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode1),
                        brukersMeldekortId = null,
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
                    Meldeperiodebehandling(
                        meldeperiodeId = MeldeperiodeId.random(),
                        meldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode2),
                        brukersMeldekortId = MeldekortId.random(),
                        periode = periode2,
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

            val tidligereVedtak = Meldekortvedtak(
                id = VedtakId.random(),
                sakId = sak.id,
                opprettet = nå(fixedClock),
                erKorrigering = false,
                erAutomatiskBehandlet = true,
                meldeperiodebehandlinger = listOf(
                    Meldeperiodebehandling(
                        meldeperiodeId = MeldeperiodeId.random(),
                        meldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
                        brukersMeldekortId = null,
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
                    ),
                ),
            )

            val seinereVedtak = Meldekortvedtak(
                id = VedtakId.random(),
                sakId = sak.id,
                opprettet = nå(fixedClock).plusSeconds(60),
                erKorrigering = true,
                erAutomatiskBehandlet = false,
                meldeperiodebehandlinger = listOf(
                    Meldeperiodebehandling(
                        meldeperiodeId = MeldeperiodeId.random(),
                        meldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
                        brukersMeldekortId = null,
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
                    Meldeperiodebehandling(
                        meldeperiodeId = MeldeperiodeId.random(),
                        meldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
                        brukersMeldekortId = null,
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
}
