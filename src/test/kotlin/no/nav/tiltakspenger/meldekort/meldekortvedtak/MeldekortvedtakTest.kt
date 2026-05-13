package no.nav.tiltakspenger.meldekort.meldekortvedtak

import io.kotest.assertions.throwables.shouldThrowWithMessage
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import org.junit.jupiter.api.Test

class MeldekortvedtakTest {

    @Test
    fun `kaster feil når meldeperiodebehandlinger er tom`() {
        shouldThrowWithMessage<IllegalArgumentException>("Et meldekortvedtak må ha minst én meldeperiodebehandling") {
            Meldekortvedtak(
                id = VedtakId.random(),
                sakId = SakId.random(),
                opprettet = nå(fixedClock),
                erKorrigering = false,
                erAutomatiskBehandlet = false,
                meldeperiodebehandlinger = emptyList(),
            )
        }
    }
}
