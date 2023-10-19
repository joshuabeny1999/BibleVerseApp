package ch.joshuah.bibleverseapp.data

data class VotdApiResponse(val votd: Votd) {
    data class Votd(
        val text: String,
        val display_ref: String,
        val version_id: String,
        val version: String,
        val permalink: String
    )
}
