package no.nav.tiltakspenger.meldekort.domene.varsler

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.meldekort.domene.VarselId
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Fire-and-forget-beskjed til bruker om endringer på tidligere innsendte meldeperioder.
 *
 * Beskjeder har ikke samme livsløp som oppgave-varsler ([Varsel]): de inaktiveres aldri av oss.
 * Bruker kan lukke/inaktivere beskjedene selv på Min side.
 */
data class BeskjedVarsel(
    val varselId: VarselId,
    val sakId: SakId,
    val saksnummer: String,
    val fnr: Fnr,
    val meldeperioder: List<BeskjedMeldeperiode>,
    val opprettet: LocalDateTime,
) {
    init {
        require(meldeperioder.isNotEmpty()) {
            "BeskjedVarsel må gjelde minst én meldeperiode"
        }
        require(meldeperioder.all { it.sakId == sakId }) {
            "Alle meldeperioder i en beskjed må tilhøre samme sak"
        }
        require(meldeperioder.distinctBy { it.kjedeId to it.versjon }.size == meldeperioder.size) {
            "BeskjedVarsel kan ikke ha duplikate meldeperiodeversjoner"
        }
    }
}

data class BeskjedMeldeperiode(
    val sakId: SakId,
    val meldeperiodeId: MeldeperiodeId,
    val kjedeId: MeldeperiodeKjedeId,
    val versjon: Int,
    val sisteInnsendteVersjon: Int,
    val endring: MeldeperiodeEndring,
) {
    init {
        require(versjon > sisteInnsendteVersjon) {
            "BeskjedMeldeperiode: versjon $versjon må være nyere enn siste innsendte versjon $sisteInnsendteVersjon"
        }
    }
}

data class MeldeperiodeEndring(
    val maksAntallDagerForPeriode: Verdiendring<Int>?,
    val girRett: List<GirRettEndring>,
) {
    init {
        require(maksAntallDagerForPeriode != null || girRett.isNotEmpty()) {
            "MeldeperiodeEndring må inneholde minst én endring"
        }
    }
}

data class Verdiendring<T>(
    val fra: T,
    val til: T,
)

data class GirRettEndring(
    val dato: LocalDate,
    val fra: Boolean,
    val til: Boolean,
)
