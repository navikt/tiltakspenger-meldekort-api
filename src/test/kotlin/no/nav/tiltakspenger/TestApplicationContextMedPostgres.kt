package no.nav.tiltakspenger

import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import java.time.Clock

/**
 * Test-kontekst som bruker ekte Postgres via testcontainers.
 * Repos bruker ekte implementasjoner mot databasen, mens eksterne klienter er fakes.
 */
class TestApplicationContextMedPostgres(
    clock: Clock,
    override val texasClient: TexasClient,
    override val sessionFactory: PostgresSessionFactory,
) : TestApplicationContext(clock)
