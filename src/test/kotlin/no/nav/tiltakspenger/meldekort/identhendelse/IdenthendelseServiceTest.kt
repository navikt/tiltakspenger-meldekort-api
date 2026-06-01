package no.nav.tiltakspenger.meldekort.identhendelse

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.util.UUID

class IdenthendelseServiceTest {
    @Test
    fun `behandleIdenthendelse - finnes sak for gammelt fnr - oppdaterer sak`() {
        withMigratedDb(runIsolated = false) { helper ->
            val identhendelseService = IdenthendelseService(helper.sakPostgresRepo)
            val gammeltFnr = helper.nesteFnr()
            val nyttFnr = helper.nesteFnr()
            val meldeperiode = ObjectMother.meldeperiode(fnr = gammeltFnr, opprettet = nå(fixedClock))

            val sak = ObjectMother.sak(
                id = meldeperiode.sakId,
                fnr = gammeltFnr,
                saksnummer = meldeperiode.saksnummer,
                meldeperioder = listOf(meldeperiode),
            )
            helper.sakPostgresRepo.lagre(sak)
            helper.meldeperiodeRepo.lagre(meldeperiode)

            identhendelseService.behandleIdenthendelse(
                Identhendelse(
                    id = UUID.randomUUID(),
                    gammeltFnr = gammeltFnr,
                    nyttFnr = nyttFnr,
                ),
            )

            val oppdatertSak = helper.sakPostgresRepo.hent(sak.id)
            val oppdatertMeldeperiode = helper.meldeperiodeRepo.hentForId(meldeperiode.id)

            oppdatertSak shouldNotBe null
            oppdatertSak?.fnr shouldBe nyttFnr
            oppdatertMeldeperiode shouldNotBe null
            oppdatertMeldeperiode?.fnr shouldBe nyttFnr
        }
    }
}
