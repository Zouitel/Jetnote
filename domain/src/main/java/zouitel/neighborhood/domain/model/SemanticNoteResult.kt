package zouitel.neighborhood.domain.model

data class SemanticNoteResult(
    val noteId: String,
    val score: Double,
) {
    init {
        require(noteId.isNotBlank()) { "Note ID must not be blank." }
        require(score.isFinite() && score in 0.0..1.0) {
            "Semantic score must be finite and between 0 and 1."
        }
    }
}
