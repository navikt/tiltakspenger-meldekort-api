package no.nav.tiltakspenger.generators

import arrow.atomic.Atomic

/**
 * Trådsikker generator for saksnumre i tester.
 * Saksnummer er på formatet yyyyMMddLLLL (8 siffer dato + 4 siffer løpenr).
 */
class SaksnummerGeneratorForTest(
    første: String = "202101011001",
) {
    private val neste = Atomic(første)

    fun generer(): String = neste.getAndUpdate { it.nesteSaksnummer() }

    private fun String.nesteSaksnummer(): String {
        val prefiks = substring(0, 8)
        val nesteLøpenummer = substring(8).toInt().plus(1).toString().padStart(4, '0')
        return prefiks + nesteLøpenummer
    }
}
