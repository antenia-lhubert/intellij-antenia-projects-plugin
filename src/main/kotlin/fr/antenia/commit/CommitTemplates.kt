package fr.antenia.commit

data class CommitTemplate(
    val name: String,
    val content: String,
    val isDefault: Boolean = false,
)

object CommitTemplates {
    fun defaults(): List<CommitTemplate> = listOf(
        CommitTemplate("Evolution", "[EVO] - Mantis : MANTIS_NO : TITRE\n\n> MESSAGE", isDefault = true),
        CommitTemplate("Bug", "[BUG] - Mantis : MANTIS_NO : TITRE\n\n> MESSAGE", isDefault = true),
        CommitTemplate("Transversal bug", "[BUG_TRANSVERSAL] - Mantis : MANTIS_NO : TITRE\n\n> MESSAGE", isDefault = true),
        CommitTemplate("Structure", "[STRUCT] - Mantis : MANTIS_NO : TITRE\n\n> MESSAGE", isDefault = true),
        CommitTemplate("Code review", "[CODE_REVIEW] - TITRE\n\n> MESSAGE", isDefault = true),
        CommitTemplate("Merge", "[MERGE] rREVISION | AUTHOR | DATE", isDefault = true),
    )

    fun withDefaults(templates: List<CommitTemplate>): List<CommitTemplate> = defaults() + customOnly(templates)

    fun customOnly(templates: List<CommitTemplate>): List<CommitTemplate> {
        val defaultNames = defaults().mapTo(mutableSetOf()) { it.name }
        return templates
            .filterNot { it.isDefault || it.name in defaultNames }
            .map { it.copy(isDefault = false) }
    }
}

data class CommitMessageValidation(
    val isValid: Boolean,
    val characterCount: Int,
    val description: String,
)

object CommitMessageValidator {
    private const val MINIMUM_MESSAGE_LENGTH = 25
    private val mantisPattern = Regex(
        """^\[(EVO|BUG|BUG_TRANSVERSAL|STRUCT)] - Mantis : [1-9]\d* : \S.*$""",
    )
    private val codeReviewPattern = Regex("""^\[CODE_REVIEW] - \S.*$""")
    private val mergePattern = Regex("""^\[MERGE] r[1-9]\d* \| [A-Za-z0-9._-]+ \| \S.*$""")

    fun validate(message: String): CommitMessageValidation {
        val firstLine = message.lineSequence().firstOrNull().orEmpty().trimEnd('\r')
        if (message.length < MINIMUM_MESSAGE_LENGTH) {
            return CommitMessageValidation(
                isValid = false,
                characterCount = message.length,
                description = "The commit message must contain at least $MINIMUM_MESSAGE_LENGTH characters.",
            )
        }

        val matchesFormat = mantisPattern.matches(firstLine) ||
            codeReviewPattern.matches(firstLine) ||
            mergePattern.matches(firstLine)
        return CommitMessageValidation(
            isValid = matchesFormat,
            characterCount = message.length,
            description = if (matchesFormat) {
                "The commit message has a valid Antenia format."
            } else {
                "The first line does not match an Antenia commit format."
            },
        )
    }
}
