package fr.antenia.commit

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vcs.VcsDataKeys
import fr.antenia.MyMessageBundle.message

class CommitTemplateActionGroup : ActionGroup(message("commit.template.group"), true), DumbAware {
    init {
        templatePresentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
    }

    override fun getChildren(event: AnActionEvent?): Array<AnAction> {
        val templateActions = CommitTemplateSettings.getInstance().templates()
            .map { ApplyCommitTemplateAction(it) }
        return if (templateActions.isEmpty()) {
            arrayOf(EditCommitTemplatesAction())
        } else {
            (templateActions + Separator.getInstance() + EditCommitTemplatesAction()).toTypedArray()
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

class CommitMessageStatusAction : DumbAwareAction() {
    init {
        templatePresentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
    }

    override fun update(event: AnActionEvent) {
        val document = event.getData(VcsDataKeys.COMMIT_MESSAGE_DOCUMENT)
        if (document == null) {
            event.presentation.isEnabledAndVisible = false
            return
        }

        val validation = CommitMessageValidator.validate(document.text)
        event.presentation.isEnabledAndVisible = true
        event.presentation.text = message("commit.status.count", validation.characterCount)
        event.presentation.description = validation.description
        event.presentation.icon = if (validation.isValid) AllIcons.General.InspectionsOK else AllIcons.General.InspectionsError
    }

    override fun actionPerformed(event: AnActionEvent) = Unit

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

private class ApplyCommitTemplateAction(private val template: CommitTemplate) : DumbAwareAction(
    template.name,
    message("commit.template.apply.description", template.name),
    null,
) {
    override fun actionPerformed(event: AnActionEvent) {
        event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)?.setCommitMessage(template.content)
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

private class EditCommitTemplatesAction : DumbAwareAction(message("commit.template.edit"), message("commit.template.edit.description"), AllIcons.General.Settings) {
    override fun actionPerformed(event: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(event.project, CommitTemplatesConfigurable::class.java)
    }
}
