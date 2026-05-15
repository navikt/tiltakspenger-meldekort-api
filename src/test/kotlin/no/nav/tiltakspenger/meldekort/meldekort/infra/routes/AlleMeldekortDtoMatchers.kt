package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.NumberFormat
import io.kotest.assertions.json.PropertyOrder
import io.kotest.assertions.json.TypeCoercion
import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.meldekort.bruker.infra.routes.ArenaMeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortDagTilBrukerDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortMedSisteMeldeperiodeDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.meldeperiode.infra.MeldeperiodeDTO
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Sjekker at JSON-strengen `this` matcher det vi forventer av `/meldekort/innsendte`.
 *
 * Sammenligner med strict JSON-semantikk, men whitespace (space/lf/tabs) ignoreres av JSON-parseren.
 * Ingen serializer brukes til forventet JSON.
 *
 * Brukerfeltene og meldekortlisten har defaults — uten eksplisitte meldekort asserter man mot tom liste.
 */
fun String.shouldBeAlleMeldekortJson(
    nesteMeldekort: MeldekortTilBrukerDTO? = null,
    forrigeMeldekort: MeldekortTilBrukerDTO? = null,
    arenaMeldekortStatus: ArenaMeldekortStatusDTO = ArenaMeldekortStatusDTO.UKJENT,
    harSoknadUnderBehandling: Boolean = false,
    kanSendeInnHelgForMeldekort: Boolean = false,
    meldekortMedSisteMeldeperiode: List<MeldekortMedSisteMeldeperiodeDTO> = emptyList(),
) {
    val brukerJson = brukerMedSakToJson(
        nesteMeldekort = nesteMeldekort,
        forrigeMeldekort = forrigeMeldekort,
        arenaMeldekortStatus = arenaMeldekortStatus,
        harSoknadUnderBehandling = harSoknadUnderBehandling,
        kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
    )

    this.shouldEqualJson {
        propertyOrder = PropertyOrder.Strict
        arrayOrder = ArrayOrder.Strict
        fieldComparison = FieldComparison.Strict
        numberFormat = NumberFormat.Strict
        typeCoercion = TypeCoercion.Disabled

        """
            {
              "bruker": $brukerJson,
              "meldekortMedSisteMeldeperiode": ${meldekortMedSisteMeldeperiode.meldekortMedSisteMeldeperiodeToJson()}
            }
        """.trimIndent()
    }
}

private fun brukerMedSakToJson(
    nesteMeldekort: MeldekortTilBrukerDTO?,
    forrigeMeldekort: MeldekortTilBrukerDTO?,
    arenaMeldekortStatus: ArenaMeldekortStatusDTO,
    harSoknadUnderBehandling: Boolean,
    kanSendeInnHelgForMeldekort: Boolean,
): String =
    """
        {
          "nesteMeldekort": ${nesteMeldekort.toJson()},
          "forrigeMeldekort": ${forrigeMeldekort.toJson()},
          "arenaMeldekortStatus": "${arenaMeldekortStatus.name}",
          "harSoknadUnderBehandling": $harSoknadUnderBehandling,
          "kanSendeInnHelgForMeldekort": $kanSendeInnHelgForMeldekort,
          "harSak": true
        }
    """.trimIndent()

private fun MeldekortMedSisteMeldeperiodeDTO.toJson(): String =
    """
        {
          "meldekort": ${meldekort.toJson()},
          "sisteMeldeperiode": ${sisteMeldeperiode.toJson()}
        }
    """.trimIndent()

private fun MeldekortTilBrukerDTO?.toJson(): String = this?.let {
    """
        {
          "id": "$id",
          "meldeperiodeId": "$meldeperiodeId",
          "kjedeId": "$kjedeId",
          "versjon": $versjon,
          "fraOgMed": "$fraOgMed",
          "tilOgMed": "$tilOgMed",
          "uke1": $uke1,
          "uke2": $uke2,
          "status": "${status.name}",
          "maksAntallDager": $maksAntallDager,
          "innsendt": ${innsendt.toJsonValue()},
          "dager": ${dager.meldekortDagerToJson()},
          "kanSendes": ${kanSendes.toJsonValue()}
        }
    """.trimIndent()
} ?: "null"

private fun MeldekortDagTilBrukerDTO.toJson(): String =
    """
        {
          "dag": "$dag",
          "status": "${status.name}"
        }
    """.trimIndent()

private fun MeldeperiodeDTO.toJson(): String =
    """
        {
          "meldeperiodeId": "$meldeperiodeId",
          "kjedeId": "$kjedeId",
          "periode": {
            "fraOgMed": "${periode.fraOgMed}",
            "tilOgMed": "${periode.tilOgMed}"
          },
          "maksAntallDagerForPeriode": $maksAntallDagerForPeriode
        }
    """.trimIndent()

private fun List<MeldekortMedSisteMeldeperiodeDTO>.meldekortMedSisteMeldeperiodeToJson(): String =
    joinToString(separator = ",", prefix = "[", postfix = "]") { it.toJson() }

private fun List<MeldekortDagTilBrukerDTO>.meldekortDagerToJson(): String =
    joinToString(separator = ",", prefix = "[", postfix = "]") { it.toJson() }

private fun LocalDateTime?.toJsonValue(): String = this?.let { "\"${it.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}\"" } ?: "null"
