package no.nav.tiltakspenger.generators

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr

/**
 * Trådsikker, deterministisk fnr-generator for tester.
 *
 * Genererer sekvensielle 11-sifrede fnr slik at vi garantert unngår kollisjoner – og dermed
 * flaky tester – som tilfeldige fnr (`Fnr.random()`) kan gi når flere tester deler samme test-db.
 *
 * Instansen deles på et høyere nivå (se [no.nav.tiltakspenger.db.IdGenerators] som holdes av
 * test-db-manageren), på samme måte som [SaksnummerGeneratorForTest].
 */
class FnrGenerator(
    start: Long = 0L,
) {
    private val neste = Atomic(start)

    fun generer(): Fnr = Fnr.fromString(neste.getAndUpdate { it + 1 }.toString().padStart(11, '0'))
}
