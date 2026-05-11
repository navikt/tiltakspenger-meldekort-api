package no.nav.tiltakspenger.meldekort.arena.infra

import no.nav.tiltakspenger.meldekort.arena.ArenaFravaerType
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekort
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortOversikt

data class ArenaMeldekortResponse(
    val personId: Long,
    val etternavn: String,
    val fornavn: String,
    val maalformkode: String,
    val meldeform: String,
    val antallGjenstaaendeFeriedager: Int? = 0,
    val meldekortListe: List<ArenaMeldekort>? = null,
    val fravaerListe: List<ArenaFravaerType>? = null,
)

fun ArenaMeldekortResponse.tilArenaMeldekortOversikt(): ArenaMeldekortOversikt =
    ArenaMeldekortOversikt(
        personId = personId,
        etternavn = etternavn,
        fornavn = fornavn,
        maalformkode = maalformkode,
        meldeform = meldeform,
        antallGjenstaaendeFeriedager = antallGjenstaaendeFeriedager,
        meldekortListe = meldekortListe,
        fravaerListe = fravaerListe,
    )
