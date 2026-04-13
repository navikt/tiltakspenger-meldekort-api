package no.nav.tiltakspenger.meldekort.domene.varsler

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.meldekort.domene.Sak
import java.time.Clock
import java.time.LocalDateTime

internal fun vurderVarselForSak(
    sak: Sak,
    sistFlaggetTidspunktVedLesing: LocalDateTime? = null,
    clock: Clock,
    sessionFactory: SessionFactory,
    hentVarsler: () -> Varsler,
    hentKjederSomManglerInnsending: () -> List<KjedeSomManglerInnsending>,
    lagreVarsel: (Varsel, SessionContext) -> Unit,
    markerVarselVurdert: (LocalDateTime, LocalDateTime?, SessionContext) -> Unit,
    logInfo: (String) -> Unit,
    logWarn: (String) -> Unit,
) {
    val vurderingstidspunkt = nå(clock)
    val varsler = hentVarsler()
    val kjederSomManglerInnsending = hentKjederSomManglerInnsending()

    sessionFactory.withTransactionContext { tx ->
        if (kjederSomManglerInnsending.isNotEmpty()) {
            opprettEllerOppdaterVarselHvisNødvendig(
                sak = sak,
                varsler = varsler,
                kjederSomManglerInnsending = kjederSomManglerInnsending,
                clock = clock,
                sessionContext = tx,
                lagreVarsel = lagreVarsel,
                logInfo = logInfo,
                logWarn = logWarn,
            )
        } else {
            avbrytEllerForberedInaktivering(
                sak = sak,
                varsler = varsler,
                nå = vurderingstidspunkt,
                sessionContext = tx,
                lagreVarsel = lagreVarsel,
                logInfo = logInfo,
                logWarn = logWarn,
            )
        }
        // Hvis sist_flagget_tidspunkt er endret siden vi leste saken kaster markerVarselVurdert
        // OptimistiskLåsFeil og hele transaksjonen rulles tilbake. Saken forblir flagget og
        // plukkes opp på nytt i neste kjøring, slik at hendelsen ikke går tapt.
        markerVarselVurdert(vurderingstidspunkt, sistFlaggetTidspunktVedLesing, tx)
    }
}

private fun opprettEllerOppdaterVarselHvisNødvendig(
    sak: Sak,
    varsler: Varsler,
    kjederSomManglerInnsending: List<KjedeSomManglerInnsending>,
    clock: Clock,
    sessionContext: SessionContext,
    lagreVarsel: (Varsel, SessionContext) -> Unit,
    logInfo: (String) -> Unit,
    logWarn: (String) -> Unit,
) {
    val planlagtAktivering = beregnPlanlagtAktivering(
        kjederSomManglerInnsending = kjederSomManglerInnsending,
        varsler = varsler,
        clock = clock,
    )

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
            lagreVarsel(nyttVarsel, sessionContext)
            logInfo("Opprettet varsel ${nyttVarsel.varselId} for sak ${sak.id}")
        }.onLeft { feil ->
            // Vi har allerede sjekket at alle eksisterende varsler er inaktivert/avbrutt, og
            // finnPlanlagtAktiveringstidspunkt håndterer cooldown. Hvis vi likevel ender her
            // er det en invariant-brudd. Vi kaster en exception slik at transaksjonen ruller
            // tilbake og hendelsen blir forsøkt på nytt (i stedet for å bli markert som vurdert
            // og dermed tapt).
            logWarn("Kunne ikke opprette varsel for sak ${sak.id}: ${feil.melding}")
            error("Kunne ikke opprette varsel for sak ${sak.id}: ${feil.melding}")
        }
        return
    }

    val eksisterendeVarsel = varsler.single { !it.erInaktivertEllerAvbrutt }
    when (eksisterendeVarsel) {
        is Varsel.SkalAktiveres -> {
            val skalOppdateres = eksisterendeVarsel.skalAktiveresTidspunkt != planlagtAktivering.tidspunkt

            if (skalOppdateres) {
                val oppdatertVarsel = eksisterendeVarsel.planleggPåNytt(
                    skalAktiveresTidspunkt = planlagtAktivering.tidspunkt,
                    skalAktiveresBegrunnelse = planlagtAktivering.begrunnelse,
                    sistEndret = nå(clock),
                )
                lagreVarsel(oppdatertVarsel, sessionContext)
                logInfo(
                    "Oppdaterte varsel ${oppdatertVarsel.varselId} for sak ${sak.id} fra ${eksisterendeVarsel.skalAktiveresTidspunkt} til ${oppdatertVarsel.skalAktiveresTidspunkt}",
                )
            }
        }

        is Varsel.Aktiv -> {
            // Scenario: bruker har sendt inn meldekort for meldeperioden varselet gjaldt,
            // men en senere meldeperiode mangler fortsatt innsending.
            // Da skal vi forberede inaktivering av det aktive varselet. Det nye varselet
            // for den senere meldeperioden opprettes i en påfølgende kjøring (når varselet
            // har blitt Inaktivert), for å ikke bryte invarianten om kun ett aktivt varsel.
            //
            // Vi sammenligner mot tidligste kjede sin kanFyllesUtFraOgMed (ikke bumped
            // planlagtAktivering.tidspunkt), siden cooldown-bump kan gi en senere dato
            // selv når kjeden gjelder samme meldeperiode som eksisterende varsel.
            val tidligsteKjedeDato = kjederSomManglerInnsending.minOf { it.kanFyllesUtFraOgMed }.toLocalDate()
            val nyMeldeperiode = tidligsteKjedeDato > eksisterendeVarsel.skalAktiveresTidspunkt.toLocalDate()
            if (nyMeldeperiode) {
                eksisterendeVarsel.forberedInaktivering(
                    skalInaktiveresTidspunkt = nå(clock),
                    skalInaktiveresBegrunnelse = "Meldekort mottatt for meldeperioden varselet gjaldt, men ny meldeperiode mangler innsending. ${planlagtAktivering.begrunnelse}",
                ).onRight { skalInaktiveres ->
                    lagreVarsel(skalInaktiveres, sessionContext)
                    logInfo("Forberedte inaktivering av aktivt varsel ${eksisterendeVarsel.varselId} for sak ${sak.id} (ny meldeperiode mangler innsending)")
                }.onLeft { feil ->
                    // Et Aktiv-varsel har aktiveringstidspunkt i fortiden, så forberedInaktivering
                    // med nå bør aldri feile. Hvis det likevel gjør det, rull tilbake og retry.
                    logWarn("Kunne ikke forberede inaktivering av aktivt varsel ${eksisterendeVarsel.varselId} for sak ${sak.id}: ${feil.melding}")
                    error("Kunne ikke forberede inaktivering av aktivt varsel ${eksisterendeVarsel.varselId} for sak ${sak.id}: ${feil.melding}")
                }
            }
        }

        is Varsel.SkalInaktiveres -> {
            // Varselet er allerede planlagt inaktivert. Nytt varsel opprettes i en senere
            // kjøring når varselet har blitt Inaktivert.
            Unit
        }

        is Varsel.Inaktivert,
        is Varsel.Avbrutt,
        -> error("Eksisterende varsel skal ikke være inaktivert eller avbrutt her (filtrert ut ovenfor)")
    }
}

private fun beregnPlanlagtAktivering(
    kjederSomManglerInnsending: List<KjedeSomManglerInnsending>,
    varsler: Varsler,
    clock: Clock,
): PlanlagtAktivering {
    val tidligsteKjede = kjederSomManglerInnsending.minBy { it.kanFyllesUtFraOgMed }
    val vurderingstidspunkt = nå(clock)
    val planlagtTidspunkt = varsler.finnPlanlagtAktiveringstidspunkt(
        ønsketTidspunkt = tidligsteKjede.kanFyllesUtFraOgMed,
        nå = vurderingstidspunkt,
    )
    val kjederInfo = kjederSomManglerInnsending.joinToString(
        prefix = "[",
        postfix = "]",
    ) {
        "(meldeperiodeId=${it.meldeperiodeId}, kjedeId=${it.kjedeId}, versjon=${it.nyesteVersjon}, kanFyllesUtFraOgMed=${it.kanFyllesUtFraOgMed})"
    }

    return PlanlagtAktivering(
        tidspunkt = planlagtTidspunkt,
        begrunnelse = "Vurdert av VurderVarselService - ønsketAktivering=${tidligsteKjede.kanFyllesUtFraOgMed}, planlagtAktivering=$planlagtTidspunkt, vurderingstidspunkt=$vurderingstidspunkt, valgtKjedeId=${tidligsteKjede.kjedeId}, antallKjeder=${kjederSomManglerInnsending.size}, manglendeKjeder=$kjederInfo",
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
    lagreVarsel: (Varsel, SessionContext) -> Unit,
    logInfo: (String) -> Unit,
    logWarn: (String) -> Unit,
) {
    varsler.forEach { varsel ->
        when (varsel) {
            is Varsel.SkalAktiveres -> {
                val avbrutt = varsel.avbryt(
                    avbruttTidspunkt = nå,
                    avbruttBegrunnelse = "Ingen kjeder mangler innsending ved vurdering",
                )
                lagreVarsel(avbrutt, sessionContext)
                logInfo("Avbrøt varsel ${varsel.varselId} for sak ${sak.id}")
            }

            is Varsel.Aktiv -> {
                varsel.forberedInaktivering(
                    skalInaktiveresTidspunkt = nå,
                    skalInaktiveresBegrunnelse = "Ingen kjeder mangler innsending ved vurdering",
                ).onRight { skalInaktiveres ->
                    lagreVarsel(skalInaktiveres, sessionContext)
                    logInfo("Forberedte inaktivering av varsel ${varsel.varselId} for sak ${sak.id}")
                }.onLeft { feil ->
                    // aktiveringstidspunkt er i fortiden, så forberedInaktivering med nå skal aldri feile.
                    // Kast for å rulle tilbake transaksjonen og forsøke på nytt.
                    logWarn("Kunne ikke forberede inaktivering av varsel ${varsel.varselId}: ${feil.melding}")
                    error("Kunne ikke forberede inaktivering av varsel ${varsel.varselId} for sak ${sak.id}: ${feil.melding}")
                }
            }

            is Varsel.SkalInaktiveres,
            is Varsel.Inaktivert,
            is Varsel.Avbrutt,
            -> Unit
        }
    }
}
