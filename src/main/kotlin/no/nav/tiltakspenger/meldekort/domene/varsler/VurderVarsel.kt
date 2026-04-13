package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import java.time.Clock
import java.time.LocalDateTime

internal sealed interface VurderVarselUtfall {
    data object HarPågåendeInaktivering : VurderVarselUtfall

    data object KanIkkeErstattePåGrunnAvCooldown : VurderVarselUtfall
}

internal fun vurderVarselForSak(
    sakId: SakId,
    saksnummer: String,
    fnr: Fnr,
    sistFlaggetTidspunktVedLesing: LocalDateTime? = null,
    clock: Clock,
    sessionFactory: SessionFactory,
    hentVarsler: () -> Varsler,
    hentKjederSomManglerInnsending: () -> List<KjedeSomManglerInnsending>,
    lagreVarsel: (Varsel, SessionContext) -> Unit,
    markerVarselVurdert: (LocalDateTime, LocalDateTime?, SessionContext) -> Unit,
    logger: KLogger = KotlinLogging.logger {},
): Either<VurderVarselUtfall, Unit> {
    val vurderingstidspunkt = nå(clock)
    val varsler = hentVarsler()
    val kjederSomManglerInnsending = hentKjederSomManglerInnsending()
    var resultat: Either<VurderVarselUtfall, Unit> = Unit.right()

    sessionFactory.withTransactionContext { tx ->
        resultat = if (kjederSomManglerInnsending.isNotEmpty()) {
            val førsteKjedeSomManglerInnsending = kjederSomManglerInnsending.minBy { it.kanFyllesUtFraOgMed }
            opprettEllerOppdaterVarselHvisNødvendig(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                varsler = varsler,
                førsteKjedeSomManglerInnsending = førsteKjedeSomManglerInnsending,
                antallKjederSomManglerInnsending = kjederSomManglerInnsending.size,
                clock = clock,
                sessionContext = tx,
                lagreVarsel = lagreVarsel,
                logger = logger,
            )
        } else {
            forberedInaktiveringHvisNødvendig(
                sakId = sakId,
                varsler = varsler,
                nå = vurderingstidspunkt,
                sessionContext = tx,
                lagreVarsel = lagreVarsel,
                logger = logger,
            )
            Unit.right()
        }
        // Hvis sist_flagget_tidspunkt er endret siden vi leste saken kaster markerVarselVurdert
        // OptimistiskLåsFeil og hele transaksjonen rulles tilbake. Saken forblir flagget og
        // plukkes opp på nytt i neste kjøring, slik at hendelsen ikke går tapt.
        markerVarselVurdert(vurderingstidspunkt, sistFlaggetTidspunktVedLesing, tx)
    }

    return resultat
}

private fun opprettEllerOppdaterVarselHvisNødvendig(
    sakId: SakId,
    saksnummer: String,
    fnr: Fnr,
    varsler: Varsler,
    førsteKjedeSomManglerInnsending: KjedeSomManglerInnsending,
    antallKjederSomManglerInnsending: Int,
    clock: Clock,
    sessionContext: SessionContext,
    lagreVarsel: (Varsel, SessionContext) -> Unit,
    logger: KLogger,
): Either<VurderVarselUtfall, Unit> {
    val planlagtAktivering = beregnPlanlagtAktivering(
        førsteKjedeSomManglerInnsending = førsteKjedeSomManglerInnsending,
        antallKjederSomManglerInnsending = antallKjederSomManglerInnsending,
        varsler = varsler,
        clock = clock,
    )

    return when (val pågående = varsler.pågåendeOppretting) {
        null -> opprettNyttVarsel(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varsler = varsler,
            planlagtAktivering = planlagtAktivering,
            clock = clock,
            sessionContext = sessionContext,
            lagreVarsel = lagreVarsel,
            logger = logger,
        ).let { Unit.right() }

        is Varsel.Aktiv, is Varsel.SkalAktiveres -> håndterPågåendeMedKjeder(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varsler = varsler,
            pågående = pågående,
            planlagtAktivering = planlagtAktivering,
            clock = clock,
            sessionContext = sessionContext,
            lagreVarsel = lagreVarsel,
            logger = logger,
        )

        is Varsel.SkalInaktiveres, is Varsel.Inaktivert -> error("Uventet pågående varsel-type ${pågående::class.simpleName}")
    }
}

private fun opprettNyttVarsel(
    sakId: SakId,
    saksnummer: String,
    fnr: Fnr,
    varsler: Varsler,
    planlagtAktivering: PlanlagtAktivering,
    clock: Clock,
    sessionContext: SessionContext,
    lagreVarsel: (Varsel, SessionContext) -> Unit,
    logger: KLogger,
) {
    varsler.leggTil(
        sakId = sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        skalAktiveresTidspunkt = planlagtAktivering.skalAktiveresTidspunkt,
        skalAktiveresEksterntTidspunkt = planlagtAktivering.skalAktiveresEksterntTidspunkt,
        skalAktiveresBegrunnelse = planlagtAktivering.begrunnelse,
        clock = clock,
    ).onRight { oppdaterteVarsler ->
        val nyttVarsel = oppdaterteVarsler.last()
        lagreVarsel(nyttVarsel, sessionContext)
        logger.info { "Opprettet varsel ${nyttVarsel.varselId} for sak $sakId" }
    }.onLeft { feil ->
        // Vi har allerede sjekket at det ikke finnes noe pågående varsel, og
        // finnPlanlagtAktiveringstidspunkt håndterer cooldown. Hvis vi likevel ender her
        // er det et invariantbrudd – kast og rull tilbake transaksjonen for retry.
        logger.warn { "Kunne ikke opprette varsel for sak $sakId: ${feil.melding}" }
        error("Kunne ikke opprette varsel for sak $sakId: ${feil.melding}")
    }
}

/**
 * I dette tilfellet har vi ett pågående varsel (SkalAktiveres eller Aktiv) og minst én kjede som mangler innsending.
 * Vi gjør en sammenligning av det pågående varselet og den planlagte aktiveringen basert på den tidligste kjeden som mangler innsending
 * for å avgjøre om vi er fornøyd med nåværende varsel eller om vi vil inaktivere det og opprette et nytt som samsvarer med
 * oppdatert kanFyllesUtFraOgMed.
 */
private fun håndterPågåendeMedKjeder(
    sakId: SakId,
    saksnummer: String,
    fnr: Fnr,
    varsler: Varsler,
    pågående: Varsel,
    planlagtAktivering: PlanlagtAktivering,
    clock: Clock,
    sessionContext: SessionContext,
    lagreVarsel: (Varsel, SessionContext) -> Unit,
    logger: KLogger,
): Either<VurderVarselUtfall, Unit> {
    planlagtAktivering.vurderPågåendeVarsel(clock, pågående).onLeft {
        logger.info { "Beholder pågående varsel ${pågående.varselId} for sak $sakId siden planlagt aktivering (${planlagtAktivering.skalAktiveresTidspunkt}) ga utfallet ${it::class.simpleName}" }
        return it.left()
    }
    if (varsler.pågåendeInaktivering != null) {
        logger.info { "Sak $sakId har allerede pågående inaktivering og venter med å opprette nytt varsel" }
        return VurderVarselUtfall.HarPågåendeInaktivering.left()
    }
    val vurderingstidspunkt = nå(clock)
    val skalInaktiveresBegrunnelse = if (pågående is Varsel.Aktiv) {
        "Pågående varsel samsvarer ikke lenger med kanFyllesUtFraOgMed for manglende innsending. ${planlagtAktivering.begrunnelse}"
    } else {
        "Pågående varsel før aktivering samsvarer ikke lenger med kanFyllesUtFraOgMed for manglende innsending. ${planlagtAktivering.begrunnelse}"
    }
    val (oppdaterteVarsler, skalInaktiveres) = varsler.forberedInaktivering(
        varselId = pågående.varselId,
        skalInaktiveresTidspunkt = vurderingstidspunkt,
        skalInaktiveresBegrunnelse = skalInaktiveresBegrunnelse,
    )
    oppdaterteVarsler.leggTil(
        sakId = sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        skalAktiveresTidspunkt = planlagtAktivering.skalAktiveresTidspunkt,
        skalAktiveresEksterntTidspunkt = planlagtAktivering.skalAktiveresEksterntTidspunkt,
        skalAktiveresBegrunnelse = planlagtAktivering.begrunnelse,
        clock = clock,
    ).onRight { varslerMedNyttVarsel ->
        val nyttVarsel = varslerMedNyttVarsel.last()
        lagreVarsel(skalInaktiveres, sessionContext)
        logger.info {
            "Forberedte inaktivering av pågående varsel ${pågående.varselId} (${pågående::class.simpleName}) for sak $sakId siden varselet ikke lenger samsvarer med kanFyllesUtFraOgMed"
        }
        lagreVarsel(nyttVarsel, sessionContext)
        logger.info { "Opprettet nytt varsel ${nyttVarsel.varselId} for sak $sakId etter varsel ${pågående.varselId}" }
    }.onLeft { feil ->
        if (feil is Varsler.KanIkkeLeggeTilVarsel.CooldownIkkeUtløpt) {
            logger.info { "Beholder pågående varsel ${pågående.varselId} for sak $sakId siden nytt varsel ville brutt cooldown samme dag" }
            return VurderVarselUtfall.KanIkkeErstattePåGrunnAvCooldown.left()
        }
        error("Kunne ikke opprette nytt varsel for sak $sakId: ${feil.melding}")
    }
    return Unit.right()
}

/**
 * Merk at denne kan finne tidspunkter tilbake i tid for skalAktiveresTidspunkt, mens skalAktiveresEksterntTidspunkt alltid vil være nå eller frem i tid.
 */
private fun beregnPlanlagtAktivering(
    førsteKjedeSomManglerInnsending: KjedeSomManglerInnsending,
    antallKjederSomManglerInnsending: Int,
    varsler: Varsler,
    clock: Clock,
): PlanlagtAktivering {
    val vurderingstidspunkt = nå(clock)
    // Denne kan være tilbake i tid, men ikke frem i tid.
    val skalAktiveresTidspunkt = førsteKjedeSomManglerInnsending.kanFyllesUtFraOgMed

    // Denne vil enten være nå eller frem i tid, aldri tilbake i tid.
    val skalAktiveresEksterntTidspunkt = varsler.finnSkalAktiveresEksterntTidspunkt(
        ønsketTidspunkt = skalAktiveresTidspunkt,
        nå = vurderingstidspunkt,
    )
    val kjederInfo =
        "(meldeperiodeId=${førsteKjedeSomManglerInnsending.meldeperiodeId}, kjedeId=${førsteKjedeSomManglerInnsending.kjedeId}, versjon=${førsteKjedeSomManglerInnsending.nyesteVersjon}, kanFyllesUtFraOgMed=${førsteKjedeSomManglerInnsending.kanFyllesUtFraOgMed})"
    return PlanlagtAktivering(
        skalAktiveresTidspunkt = skalAktiveresTidspunkt,
        skalAktiveresEksterntTidspunkt = skalAktiveresEksterntTidspunkt,
        begrunnelse = "Automatisk vurdert - skalAktiveresTidspunkt=$skalAktiveresTidspunkt, skalAktiveresEksterntTidspunkt=$skalAktiveresEksterntTidspunkt, vurderingstidspunkt=$vurderingstidspunkt, valgtKjedeId=${førsteKjedeSomManglerInnsending.kjedeId}, antallKjeder=$antallKjederSomManglerInnsending, manglendeKjeder=$kjederInfo",
    )
}

private fun forberedInaktiveringHvisNødvendig(
    sakId: SakId,
    varsler: Varsler,
    nå: LocalDateTime,
    sessionContext: SessionContext,
    lagreVarsel: (Varsel, SessionContext) -> Unit,
    logger: KLogger,
) {
    // Ingen kjeder mangler innsending lenger. Forbered inaktivering av det pågående varselet.
    //
    // Også for SkalAktiveres går vi rett til SkalInaktiveres (uten å gå via Aktiv). Vi kan ha
    // publisert aktiveringen på Kafka uten at lagringen lyktes, så for sikkerhets skyld må vi
    // alltid publisere en inaktivering mot Min side. InaktiverVarslerService håndterer dette
    // basert på SkalInaktiveres-tilstanden.
    when (val pågående = varsler.pågåendeOppretting) {
        is Varsel.SkalAktiveres, is Varsel.Aktiv -> {
            val typeNavn = pågående::class.simpleName
            val begrunnelse = "Ingen kjeder mangler innsending ved vurdering (var $typeNavn)"
            val (_, skalInaktiveres) = varsler.forberedInaktivering(
                varselId = pågående.varselId,
                skalInaktiveresTidspunkt = nå,
                skalInaktiveresBegrunnelse = begrunnelse,
            )
            lagreVarsel(skalInaktiveres, sessionContext)
            logger.info { "Forberedte inaktivering av varsel ${pågående.varselId} ($typeNavn) for sak $sakId" }
        }

        is Varsel.SkalInaktiveres, is Varsel.Inaktivert, null -> Unit
    }
}
