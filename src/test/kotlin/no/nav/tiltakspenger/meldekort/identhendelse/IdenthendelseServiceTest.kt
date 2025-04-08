package no.nav.tiltakspenger.meldekort.identhendelse

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.util.UUID

class IdenthendelseServiceTest {
    @Test
    fun `behandleIdenthendelse - finnes meldeperiode for gammelt fnr - oppdaterer`() {
        withMigratedDb { helper ->
            val repo = helper.meldeperiodeRepo
            val identhendelseService = IdenthendelseService(repo)
            val gammeltFnr = Fnr.random()
            val nyttFnr = Fnr.random()
            val meldekort = ObjectMother.meldekort(fnr = gammeltFnr, mottatt = nå(fixedClock), varselId = VarselId("varsel1"))
            repo.lagre(meldekort.meldeperiode)

            identhendelseService.behandleIdenthendelse(
                id = UUID.randomUUID(),
                identhendelseDto = IdenthendelseDto(gammeltFnr = gammeltFnr.verdi, nyttFnr = nyttFnr.verdi),
            )

            val oppdatertMeldeperiode = repo.hentForId(meldekort.meldeperiode.id)

            oppdatertMeldeperiode shouldNotBe null
            oppdatertMeldeperiode?.fnr shouldBe nyttFnr
        }
    }
}
