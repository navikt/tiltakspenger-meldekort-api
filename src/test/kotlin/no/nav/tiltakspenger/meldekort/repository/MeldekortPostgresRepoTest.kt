package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.time.LocalDateTime

class MeldekortPostgresRepoTest {
    @Nested
    inner class Lagre {
        @Test
        fun `lagres og kan hentes ut`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo

                val meldekort = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = VarselId("varsel1"))
                repo.lagre(meldekort)

                val result = repo.hentForMeldekortId(meldekort.id, meldekort.meldeperiode.fnr)

                assertNotNull(result)
                assertEquals(meldekort.id, result.id, "id")
                assertEquals(meldekort.meldeperiode.id, result.meldeperiode.id, "meldeperiode.id")
                assertEquals(meldekort.mottatt, result.mottatt, "mottatt")
                assertEquals(meldekort.varselId, result.varselId, "varselId")
            }
        }
    }

    @Nested
    inner class Oppdater {
        @Test
        fun `kan oppdatere varselId`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo

                val meldekort = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = VarselId("varsel1"))
                repo.lagre(meldekort)
                assertNotNull(meldekort.varselId, "varselId skal være satt til å begynne med")

                val oppdatertMeldekort = meldekort.copy(varselId = null)
                repo.oppdater(oppdatertMeldekort)

                val result = repo.hentForMeldekortId(meldekort.id, meldekort.meldeperiode.fnr)
                assertEquals(oppdatertMeldekort.varselId, result!!.varselId, "varselId")
            }
        }
    }

    @Nested
    inner class HentMottatteSomDetVarslesFor {
        @Test
        fun `alle matcher kriteriene`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo

                val meldekort1 = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = VarselId("varsel1"))
                val meldekort2 = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = VarselId("varsel2"))

                repo.lagre(meldekort1)
                repo.lagre(meldekort2)

                val result = repo.hentMottatteSomDetVarslesFor()

                assertEquals(2, result.size, "Antall relevante meldekort")
                assertEquals("varsel1", result[0].varselId, "varselId")
                assertEquals("varsel2", result[1].varselId, "varselId")
            }
        }

        @Test
        fun `henter bare ut relevante meldekort`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort1 = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = VarselId("varsel1"))
                val meldekort2 = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = VarselId("varsel2"))
                val meldekort3 = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = null)
                val meldekort4 = ObjectMother.meldekort(mottatt = null, varselId = VarselId("varsel4"))

                repo.lagre(meldekort1)
                repo.lagre(meldekort2)
                repo.lagre(meldekort3)
                repo.lagre(meldekort4)

                val result = repo.hentMottatteSomDetVarslesFor()

                assertEquals(2, result.size, "Antall relevante meldekort")
                assertEquals("varsel1", result[0].varselId, "varselId")
                assertEquals("varsel2", result[1].varselId, "varselId")
            }
        }
    }
}
