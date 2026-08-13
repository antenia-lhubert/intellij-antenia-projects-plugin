package fr.antenia.commit

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBSplitter
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import fr.antenia.MyMessageBundle.message
import fr.antenia.ui.RowReorderSupport
import fr.antenia.ui.RowMove
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JComponent
import javax.swing.DefaultListCellRenderer
import javax.swing.JPanel
import javax.swing.JList
import javax.swing.ListSelectionModel
import javax.swing.AbstractListModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class CommitTemplatesConfigurable : SearchableConfigurable {
    private val model = TemplateListModel()
    private val templateList = JBList(model)
    private val nameField = JBTextField()
    private val contentArea = JBTextArea(8, 48)
    private var component: JComponent? = null
    private var uiDisposable: Disposable? = null
    private var updatingFields = false

    override fun getDisplayName(): String = message("configurable.commit.templates.display.name")
    override fun getId(): String = "fr.antenia.commitTemplates"

    override fun createComponent(): JComponent {
        uiDisposable?.let(Disposer::dispose)
        uiDisposable = Disposer.newDisposable("Antenia commit templates UI")
        templateList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        templateList.emptyText
            .appendText(message("commit.templates.empty"))
            .appendLine("")
            .appendText(message("commit.templates.empty.action"), com.intellij.ui.SimpleTextAttributes.LINK_ATTRIBUTES) { addTemplate() }
        templateList.accessibleContext.accessibleName = message("commit.templates.list.accessible.name")
        templateList.cellRenderer = TemplateRenderer()
        templateList.addListSelectionListener {
            if (!it.valueIsAdjusting && !updatingFields) showSelectedTemplate()
        }
        RowReorderSupport.install(templateList, ::moveTemplate)

        nameField.accessibleContext.accessibleName = message("commit.templates.name")
        nameField.document.addDocumentListener(fieldListener {
            model.rowAt(templateList.selectedIndex)?.let {
                it.name = nameField.text
                model.changed(templateList.selectedIndex)
            }
        })
        contentArea.lineWrap = false
        contentArea.accessibleContext.accessibleName = message("commit.templates.value")
        contentArea.document.addDocumentListener(fieldListener {
            model.rowAt(templateList.selectedIndex)?.content = contentArea.text
        })
        ComponentValidator(uiDisposable!!)
            .withValidator {
                val selected = model.rowAt(templateList.selectedIndex)
                when {
                    selected == null || selected.isDefault -> null
                    nameField.text.isBlank() -> ValidationInfo(message("commit.templates.validation.name"), nameField)
                    model.hasDuplicateName(nameField.text, templateList.selectedIndex) -> ValidationInfo(message("commit.templates.validation.unique"), nameField)
                    else -> null
                }
            }
            .andRegisterOnDocumentListener(nameField)
            .installOn(nameField)

        val templateMaster = ToolbarDecorator.createDecorator(templateList)
            .setAddAction { addTemplate() }
            .setRemoveAction { removeSelectedTemplate() }
            .setRemoveActionUpdater { model.canRemove(templateList.selectedIndex) }
            .setMoveUpAction { moveSelectedTemplate(-1) }
            .setMoveUpActionUpdater { model.canMove(templateList.selectedIndex, -1) }
            .setMoveDownAction { moveSelectedTemplate(1) }
            .setMoveDownActionUpdater { model.canMove(templateList.selectedIndex, 1) }
            .addExtraAction(object : DumbAwareAction(message("commit.templates.clone"), message("commit.templates.clone.description"), AllIcons.Actions.Copy) {
                override fun actionPerformed(event: AnActionEvent) = cloneSelectedTemplate()

                override fun update(event: AnActionEvent) {
                    event.presentation.isEnabled = model.rowAt(templateList.selectedIndex) != null
                }

                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
            })
            .createPanel()
        val detailFields = JPanel(BorderLayout(0, JBUI.scale(8))).apply {
            add(JPanel(BorderLayout(0, JBUI.scale(4))).apply {
                add(JBLabel(message("commit.templates.name")).apply { labelFor = nameField }, BorderLayout.NORTH)
                add(nameField, BorderLayout.CENTER)
            }, BorderLayout.NORTH)
            add(JPanel(BorderLayout(0, JBUI.scale(4))).apply {
                add(JBLabel(message("commit.templates.value")).apply { labelFor = contentArea }, BorderLayout.NORTH)
                add(JBScrollPane(contentArea), BorderLayout.CENTER)
            }, BorderLayout.CENTER)
        }
        val valuePanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyLeft(8)
            add(detailFields, BorderLayout.CENTER)
        }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8)
            add(
                JBSplitter(false, 0.35f).apply {
                    firstComponent = templateMaster
                    secondComponent = valuePanel
                    splitterProportionKey = "fr.antenia.commitTemplates.splitter"
                },
                BorderLayout.CENTER,
            )
            component = this
            loadTemplates()
        }
    }

    override fun isModified(): Boolean = component != null && model.templates() != CommitTemplateSettings.getInstance().templates()

    override fun apply() {
        val templates = model.templates()
        if (templates.any { it.name.isBlank() || it.content.lineSequence().firstOrNull().isNullOrBlank() }) {
            throw ConfigurationException(message("commit.templates.validation.required"))
        }
        if (templates.map { it.name }.distinct().size != templates.size) {
            throw ConfigurationException(message("commit.templates.validation.unique"))
        }
        CommitTemplateSettings.getInstance().replaceTemplates(templates)
    }

    override fun reset() {
        if (component != null) loadTemplates()
    }

    override fun disposeUIResources() {
        component = null
        model.load(emptyList())
        uiDisposable?.let(Disposer::dispose)
        uiDisposable = null
    }

    private fun loadTemplates() {
        updatingFields = true
        model.load(CommitTemplateSettings.getInstance().templates())
        updatingFields = false
        if (model.rowCount > 0) {
            templateList.selectedIndex = 0
        }
        showSelectedTemplate()
    }

    private fun addTemplate() {
        val row = model.addAfter(
            templateList.selectedIndex,
            CommitTemplate(uniqueTemplateName(message("commit.templates.new")), ""),
        )
        selectAndFocusName(row)
    }

    private fun cloneSelectedTemplate() {
        val selected = model.rowAt(templateList.selectedIndex) ?: return
        val row = model.addAfter(
            templateList.selectedIndex,
            CommitTemplate(uniqueTemplateName(message("commit.templates.copy", selected.name)), selected.content),
        )
        selectAndFocusName(row)
    }

    private fun selectAndFocusName(row: Int) {
        templateList.selectedIndex = row
        templateList.ensureIndexIsVisible(row)
        nameField.requestFocusInWindow()
        nameField.selectAll()
    }

    private fun removeSelectedTemplate() {
        val row = templateList.selectedIndex
        if (row < 0 || !model.remove(row)) return
        if (model.rowCount > 0) {
            val nextRow = row.coerceAtMost(model.rowCount - 1)
            templateList.selectedIndex = nextRow
        }
        showSelectedTemplate()
    }

    private fun moveSelectedTemplate(direction: Int) {
        val destination = model.move(templateList.selectedIndex, direction)
        if (destination >= 0) templateList.selectedIndex = destination
    }

    private fun moveTemplate(source: Int, insertion: Int): Boolean {
        val destination = model.moveTo(source, insertion)
        if (destination < 0) return false
        templateList.selectedIndex = destination
        templateList.ensureIndexIsVisible(destination)
        return true
    }

    private fun showSelectedTemplate() {
        val selected = model.rowAt(templateList.selectedIndex)
        updatingFields = true
        nameField.text = selected?.name.orEmpty()
        contentArea.text = selected?.content.orEmpty()
        updatingFields = false
        nameField.isEnabled = selected != null
        nameField.isEditable = selected != null && !selected.isDefault
        contentArea.isEnabled = selected != null
        contentArea.isEditable = selected != null && !selected.isDefault
    }

    private fun uniqueTemplateName(baseName: String): String {
        val names = model.templates().mapTo(mutableSetOf()) { it.name }
        var suffix = 2
        var candidate = baseName
        while (candidate in names) {
            candidate = "$baseName $suffix"
            suffix++
        }
        return candidate
    }

    private fun fieldListener(update: () -> Unit): DocumentListener = object : DocumentListener {
        override fun insertUpdate(event: DocumentEvent) = changed()
        override fun removeUpdate(event: DocumentEvent) = changed()
        override fun changedUpdate(event: DocumentEvent) = changed()

        private fun changed() {
            if (!updatingFields) update()
        }
    }

    private inner class TemplateRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, selected: Boolean, focus: Boolean): Component {
            val component = super.getListCellRendererComponent(list, value, index, selected, focus)
            text = (value as? EditableTemplate)?.let {
                if (it.isDefault) message("commit.templates.built.in", it.name) else it.name
            }.orEmpty()
            return component
        }
    }

    private class TemplateListModel : AbstractListModel<EditableTemplate>() {
        private val rows = mutableListOf<EditableTemplate>()

        val rowCount: Int get() = rows.size
        override fun getSize(): Int = rows.size
        override fun getElementAt(index: Int): EditableTemplate = rows[index]

        fun load(templates: List<CommitTemplate>) {
            rows.clear()
            rows.addAll(templates.map { EditableTemplate(it.name, it.content, it.isDefault) })
            fireContentsChanged(this, 0, maxOf(0, rows.lastIndex))
        }

        fun templates(): List<CommitTemplate> = rows.map { CommitTemplate(it.name, it.content, it.isDefault) }

        fun rowAt(row: Int): EditableTemplate? = rows.getOrNull(row)
        fun hasDuplicateName(name: String, exceptRow: Int): Boolean = rows.indices.any { it != exceptRow && rows[it].name == name }
        fun changed(row: Int) { if (row in rows.indices) fireContentsChanged(this, row, row) }

        fun addAfter(selectedRow: Int, template: CommitTemplate): Int {
            val firstCustomRow = rows.indexOfFirst { !it.isDefault }.takeIf { it >= 0 } ?: rows.size
            val row = if (selectedRow >= firstCustomRow) {
                (selectedRow + 1).coerceAtMost(rows.size)
            } else {
                firstCustomRow
            }
            rows.add(row, EditableTemplate(template.name, template.content, isDefault = false))
            fireIntervalAdded(this, row, row)
            return row
        }

        fun canRemove(row: Int): Boolean = row in rows.indices && !rows[row].isDefault

        fun remove(row: Int): Boolean {
            if (!canRemove(row)) return false
            rows.removeAt(row)
            fireIntervalRemoved(this, row, row)
            return true
        }

        fun canMove(row: Int, direction: Int): Boolean {
            val destination = row + direction
            return row in rows.indices &&
                destination in rows.indices &&
                !rows[row].isDefault &&
                !rows[destination].isDefault
        }

        fun move(row: Int, direction: Int): Int {
            val destination = row + direction
            if (!canMove(row, direction)) return -1
            val moved = rows.removeAt(row)
            rows.add(destination, moved)
            fireContentsChanged(this, minOf(row, destination), maxOf(row, destination))
            return destination
        }

        fun moveTo(row: Int, insertion: Int): Int {
            val destination = RowMove.move(rows, row, insertion) { source, target ->
                !rows[source].isDefault && !rows[target].isDefault
            }
            if (destination < 0) return -1
            fireContentsChanged(this, minOf(row, destination), maxOf(row, destination))
            return destination
        }
    }

    private data class EditableTemplate(var name: String, var content: String, val isDefault: Boolean)
}
