package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.domene.Sak
import no.nav.tiltakspenger.meldekort.repository.SakVarselRepo
import no.nav.tiltakspenger.meldekort.repository.VarselMeldekortRepo
import no.nav.tiltakspenger.meldekort.repository.VarselRepo
import java.time.Clock
import java.time.LocalDateTime

class VurderVarselService(
    private val sakVarselRepo: SakVarselRepo,
    private val varselMeldekortRepo: VarselMeldekortRepo,
    private val varselRepo: VarselRepo,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger {}

    fun vurderVarsler() {
        Either.catch {
            val saker = sakVarselRepo.hentSakerSomSkalVurdereVarsel()
            logger.debug { "Fant ${saker.size} saker som skal vurdere varsel" }
            saker.forEach { sak -> vurderVarselForSak(sak) }
        }.onLeft {
            logger.error(it) { "Ukjent feil under vurdering av varsler" }
        }
    }

    private fun vurderVarselForSak(sak: Sak) {
        Either.catch {
            val nå = nå(clock)
            val varsler = varselRepo.hentForSakId(sak.id)
            val kjederSomManglerInnsending = varselMeldekortRepo.hentKjederSomManglerInnsending(sak.id)
            sessionFactory.withTransactionContext { tx ->
                if (kjederSomManglerInnsending.isNotEmpty()) {
                    opprettVarselHvisNødvendig(sak, varsler, kjederSomManglerInnsending, tx)
                } else {
                    avbrytEllerForberedInaktivering(sak, varsler, nå, tx)
                }
                sakVarselRepo.markerVarselVurdert(sak.id, nå, tx)
            }
        }.onLeft {
            logger.error(it) { "Feil under vurdering av varsel for sak ${sak.id}" }
        }
    }

    private fun opprettVarselHvisNødvendig(
        sak: Sak,
        varsler: Varsler,
        kjederSomManglerInnsending: List<KjedeSomManglerInnsending>,
        sessionContext: SessionContext,
    ) {
        val planlagtAktivering = beregnPlanlagtAktivering(kjederSomManglerInnsending)

        if (varsler.erAlleInaktivertEllerAvbrutt || varsler.isEmpty()) {
            varsler.leggTil(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                fnr = sak.fnr,
                skalAktiveresTidspunkt = planlagtAktivering.tidspunkt,
                skalAktiveresBegrunnelse = planlagtAktivering.begrunnelse,
                clock = clock,
            ).onRight { oppdaterteVarsler ->
                val nyttVarsel = oppdaterteVarsler.last()
                varselRepo.lagre(nyttVarsel, sessionContext = sessionContext)
                logger.info { "Opprettet varsel ${nyttVarsel.varselId} for sak ${sak.id}" }
            }.onLeft { feil ->
                logger.warn { "Kunne ikke opprette varsel for sak ${sak.id}: ${feil.melding}" }
            }
            return
        }

        val eksisterendeVarsel = varsler.single { !it.erInaktivertEllerAvbrutt }
        if (eksisterendeVarsel is Varsel.SkalAktiveres) {
            val skalOppdateres = eksisterendeVarsel.skalAktiveresTidspunkt != planlagtAktivering.tidspunkt ||
                eksisterendeVarsel.skalAktiveresBegrunnelse != planlagtAktivering.begrunnelse

            if (skalOppdateres) {
                val oppdatertVarsel = eksisterendeVarsel.planleggPåNytt(
                    skalAktiveresTidspunkt = planlagtAktivering.tidspunkt,
                    skalAktiveresBegrunnelse = planlagtAktivering.begrunnelse,
                    sistEndret = nå(clock),
                )
                varselRepo.lagre(oppdatertVarsel, sessionContext = sessionContext)
                logger.info {
                    "Oppdaterte varsel ${oppdatertVarsel.varselId} for sak ${sak.id} fra ${eksisterendeVarsel.skalAktiveresTidspunkt} til ${oppdatertVarsel.skalAktiveresTidspunkt}"
                }
            }
        }
    }

    private fun beregnPlanlagtAktivering(kjederSomManglerInnsending: List<KjedeSomManglerInnsending>): PlanlagtAktivering {
        val tidligsteKjede = kjederSomManglerInnsending.minBy { it.kanFyllesUtFraOgMed }
        val kjederInfo = kjederSomManglerInnsending.joinToString(
            prefix = "[",
            postfix = "]",
        ) {
            "(meldeperiodeId=${it.meldeperiodeId}, kjedeId=${it.kjedeId}, versjon=${it.nyesteVersjon}, kanFyllesUtFraOgMed=${it.kanFyllesUtFraOgMed})"
        }

        return PlanlagtAktivering(
            tidspunkt = tidligsteKjede.kanFyllesUtFraOgMed,
            begrunnelse = "Vurdert av VurderVarselService - valgtAktivering=${tidligsteKjede.kanFyllesUtFraOgMed}, valgtKjedeId=${tidligsteKjede.kjedeId}, antallKjeder=${kjederSomManglerInnsending.size}, manglendeKjeder=$kjederInfo",
        )
    }

    private data class PlanlagtAktivering(
        val tidspunkt: LocalDateTime,
        val begrunnelse: String,
    )

    private fun avbrytEllerForberedInaktivering(
        sak: Sak,
        varsler: Varsler,
        nå: LocalDateTime,
        sessionContext: SessionContext,
    ) {
        varsler.forEach { varsel ->
            when (varsel) {
                is Varsel.SkalAktiveres -> {
                    val avbrutt = varsel.avbryt(
                        avbruttTidspunkt = nå,
                        avbruttBegrunnelse = "Ingen kjeder mangler innsending ved vurdering",
                    )
                    varselRepo.lagre(avbrutt, sessionContext = sessionContext)
                    logger.info { "Avbrøt varsel ${varsel.varselId} for sak ${sak.id}" }
                }

                is Varsel.Aktiv -> {
                    varsel.forberedInaktivering(
                        skalInaktiveresTidspunkt = nå,
                        skalInaktiveresBegrunnelse = "Ingen kjeder mangler innsending ved vurdering",
                    ).onRight { skalInaktiveres ->
                        varselRepo.lagre(skalInaktiveres, sessionContext = sessionContext)
                        logger.info { "Forberedte inaktivering av varsel ${varsel.varselId} for sak ${sak.id}" }
                    }.onLeft { feil ->
                        logger.warn { "Kunne ikke forberede inaktivering av varsel ${varsel.varselId}: ${feil.melding}" }
                    }
                }

                is Varsel.SkalInaktiveres,
                is Varsel.Inaktivert,
                is Varsel.Avbrutt,
                -> Unit
            }
        }
    }
}
