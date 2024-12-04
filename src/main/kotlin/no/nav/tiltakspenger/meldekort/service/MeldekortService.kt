package no.nav.tiltakspenger.meldekort.service

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.routes.meldekort.MeldekortFraUtfyllingDTO
import java.time.LocalDateTime

interface MeldekortService {

    fun lagreMeldekort(meldeperiode: Meldeperiode)

    fun oppdaterMeldekort(meldekort: MeldekortFraUtfyllingDTO)

    fun hentMeldekort(meldekortId: MeldekortId): Meldeperiode?

    fun hentSisteMeldekort(fnr: Fnr): Meldeperiode?

    fun hentAlleMeldekort(fnr: Fnr): List<Meldeperiode>

    fun hentMeldekortSomSkalSendesTilSaksbehandling(): List<Meldeperiode>

    fun markerSendt(meldekortId: MeldekortId, meldekortStatus: MeldekortStatus, tidspunkt: LocalDateTime)
}
