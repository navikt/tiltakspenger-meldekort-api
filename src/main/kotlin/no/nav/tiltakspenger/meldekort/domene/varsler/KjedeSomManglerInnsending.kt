package no.nav.tiltakspenger.meldekort.domene.varsler

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import java.time.LocalDateTime

/**
 * Representerer en meldeperiodekjede som mangler innsending fra bruker.
 * En kjede mangler innsending dersom vi aldri har mottatt et meldekort for
 * noen meldeperiode i kjeden.
 *
 * Brukes av [VurderVarselService] for å avgjøre om en sak skal ha et aktivt varsel.
 *
 * @param meldeperiodeId Id til den nyeste meldeperiode-versjonen i kjeden.
 * @param kjedeId Identifikator for kjeden (en fast 14-dagersperiode).
 * @param kanFyllesUtFraOgMed Tidspunktet bruker tidligst kan fylle ut meldekort for denne kjeden.
 */
data class KjedeSomManglerInnsending(
    val sakId: SakId,
    val meldeperiodeId: MeldeperiodeId,
    val kjedeId: MeldeperiodeKjedeId,
    val kanFyllesUtFraOgMed: LocalDateTime,
)
