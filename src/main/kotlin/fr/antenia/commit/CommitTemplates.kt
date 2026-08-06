package fr.antenia.commit

import fr.antenia.MyMessageBundle.message

data class CommitTemplate(
    val name: String,
    val content: String,
    val isDefault: Boolean = false,
)

object CommitTemplates {
    fun defaults(): List<CommitTemplate> = listOf(
        CommitTemplate(message("commit.default.evolution"), message("commit.default.evolution.content"), isDefault = true),
        CommitTemplate(message("commit.default.bug"), message("commit.default.bug.content"), isDefault = true),
        CommitTemplate(message("commit.default.transversal.bug"), message("commit.default.transversal.bug.content"), isDefault = true),
        CommitTemplate(message("commit.default.structure"), message("commit.default.structure.content"), isDefault = true),
        CommitTemplate(message("commit.default.code.review"), message("commit.default.code.review.content"), isDefault = true),
        CommitTemplate(message("commit.default.merge"), message("commit.default.merge.content"), isDefault = true),
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
                description = message("commit.validation.minimum.length", MINIMUM_MESSAGE_LENGTH),
            )
        }

        val matchesFormat = mantisPattern.matches(firstLine) ||
            codeReviewPattern.matches(firstLine) ||
            mergePattern.matches(firstLine)
        return CommitMessageValidation(
            isValid = matchesFormat,
            characterCount = message.length,
            description = if (matchesFormat) {
                message("commit.validation.valid")
            } else {
                message("commit.validation.invalid")
            },
        )
    }
}
