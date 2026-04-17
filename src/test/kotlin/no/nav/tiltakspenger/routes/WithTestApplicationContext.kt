package no.nav.tiltakspenger.routes

import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.TestApplicationContextMedInMemoryDb
import no.nav.tiltakspenger.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.db.TestDatabaseManager
import no.nav.tiltakspenger.fakes.clients.TexasClientFakeTest
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.test.common.TestSessionFactory
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.meldekort.context.ApplicationContext
import no.nav.tiltakspenger.meldekort.routes.meldekortApi
import java.time.Clock

private val dbManager = TestDatabaseManager()

@Suppress("unused")
fun withTestApplicationContextAndPostgres(
    additionalConfig: Application.() -> Unit = {},
    clock: TikkendeKlokke = TikkendeKlokke(fixedClockAt(1.mai(2025))),
    texasClient: TexasClient = TexasClientFakeTest(),
    runIsolated: Boolean = false,
    testBlock: suspend ApplicationTestBuilder.(TestApplicationContextMedPostgres) -> Unit,
) {
    val callSite = captureCallSite()
    dbManager.withMigratedDb(
        runIsolated = runIsolated,
        clock = clock,
    ) { sessionFactory: SessionFactory, _: Clock ->
        val context = TestApplicationContextMedPostgres(
            clock = clock,
            texasClient = texasClient,
            sessionFactory = sessionFactory as PostgresSessionFactory,
        )
        runTestApplication(context, additionalConfig, callSite, testBlock)
    }
}

/**
 * @param clock Merk at vi ikke kan behandle et meldekort før vi har passert meldekortets første dag.Derfor er det viktig at [clock] er satt til en dato etter den første meldekortdagen i testene som bruker denne eller bruk TikkendeKlokke.spolTil(...)
 */
fun withTestApplicationContext(
    additionalConfig: Application.() -> Unit = {},
    clock: TikkendeKlokke = TikkendeKlokke(),
    texasClient: TexasClientFakeTest = TexasClientFakeTest(),
    sessionFactory: TestSessionFactory = TestSessionFactory(),
    testBlock: suspend ApplicationTestBuilder.(TestApplicationContextMedInMemoryDb) -> Unit,
) {
    val callSite = captureCallSite()
    val context = TestApplicationContextMedInMemoryDb(
        clock = clock,
        texasClient = texasClient,
        sessionFactory = sessionFactory,
    )
    runTestApplication(context, additionalConfig, callSite, testBlock)
}

private fun captureCallSite(): Exception {
    val callSite = Exception("Call site")
    callSite.stackTrace = callSite.stackTrace
        .filterNot {
            @Suppress("CanConvertToMultiDollarString", "CanUnescapeDollarLiteral")
            it.className.startsWith("kotlin.coroutines") ||
                it.className.startsWith("kotlinx.coroutines") ||
                it.className.contains("\$\$")
        }
        .toTypedArray()
    return callSite
}

private fun <T : ApplicationContext> runTestApplication(
    context: T,
    additionalConfig: Application.() -> Unit,
    callSite: Exception,
    testBlock: suspend ApplicationTestBuilder.(T) -> Unit,
) {
    testApplication {
        application {
            meldekortApi(context)
            additionalConfig()
        }
        try {
            testBlock(context)
        } catch (e: AssertionError) {
            try {
                e.initCause(callSite)
            } catch (_: IllegalStateException) {
                e.addSuppressed(callSite)
            }
            throw e
        }
    }
}
