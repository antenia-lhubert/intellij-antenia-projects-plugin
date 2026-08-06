package fr.antenia.commit

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.JBSplitter
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import fr.antenia.MyMessageBundle.message
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.AbstractTableModel

class CommitTemplatesConfigurable : SearchableConfigurable {
    private val model = TemplateTableModel()
    private val table = JBTable(model)
    private val contentArea = JBTextArea(8, 48)
    private var component: JComponent? = null
    private var updatingFields = false

    override fun getDisplayName(): String = message("configurable.commit.templates.display.name")
    override fun getId(): String = "fr.antenia.commitTemplates"

    override fun createComponent(): JComponent {
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.setShowGrid(false)
        table.emptyText.text = message("commit.templates.empty")
        table.columnModel.getColumn(0).preferredWidth = 220
        table.putClientProperty("terminateEditOnFocusLost", true)
        table.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting && !updatingFields) showSelectedTemplate()
        }

        contentArea.lineWrap = false
        contentArea.document.addDocumentListener(fieldListener {
            model.rowAt(table.selectedRow)?.content = contentArea.text
        })

        val templateTable = ToolbarDecorator.createDecorator(table)
            .setAddAction { addTemplate() }
            .setRemoveAction { removeSelectedTemplate() }
            .setRemoveActionUpdater { model.canRemove(table.selectedRow) }
            .setMoveUpAction { moveSelectedTemplate(-1) }
            .setMoveUpActionUpdater { model.canMove(table.selectedRow, -1) }
            .setMoveDownAction { moveSelectedTemplate(1) }
            .setMoveDownActionUpdater { model.canMove(table.selectedRow, 1) }
            .addExtraAction(object : DumbAwareAction(message("commit.templates.clone"), message("commit.templates.clone.description"), AllIcons.Actions.Copy) {
                override fun actionPerformed(event: AnActionEvent) = cloneSelectedTemplate()

                override fun update(event: AnActionEvent) {
                    event.presentation.isEnabled = model.rowAt(table.selectedRow) != null
                }

                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
            })
            .createPanel()
        val valuePanel = JPanel(BorderLayout(0, JBUI.scale(4))).apply {
            border = JBUI.Borders.emptyLeft(8)
            add(JLabel(message("commit.templates.value")), BorderLayout.NORTH)
            add(JBScrollPane(contentArea), BorderLayout.CENTER)
        }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8)
            add(
                JBSplitter(false, 0.35f).apply {
                    firstComponent = templateTable
                    secondComponent = valuePanel
                },
                BorderLayout.CENTER,
            )
            component = this
            loadTemplates()
        }
    }

    override fun isModified(): Boolean = component != null && model.templates() != CommitTemplateSettings.getInstance().templates()

    override fun apply() {
        if (table.isEditing) table.cellEditor.stopCellEditing()
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
    }

    private fun loadTemplates() {
        updatingFields = true
        model.load(CommitTemplateSettings.getInstance().templates())
        updatingFields = false
        if (model.rowCount > 0) {
            table.selectionModel.setSelectionInterval(0, 0)
        }
        showSelectedTemplate()
    }

    private fun addTemplate() {
        val row = model.addAfter(
            table.selectedRow,
            CommitTemplate(uniqueTemplateName(message("commit.templates.new")), ""),
        )
        selectAndEdit(row)
    }

    private fun cloneSelectedTemplate() {
        val selected = model.rowAt(table.selectedRow) ?: return
        val row = model.addAfter(
            table.selectedRow,
            CommitTemplate(uniqueTemplateName(message("commit.templates.copy", selected.name)), selected.content),
        )
        selectAndEdit(row)
    }

    private fun selectAndEdit(row: Int) {
        table.selectionModel.setSelectionInterval(row, row)
        table.editCellAt(row, 0)
        table.requestFocusInWindow()
    }

    private fun removeSelectedTemplate() {
        val row = table.selectedRow
        if (row < 0 || !model.remove(row)) return
        if (model.rowCount > 0) {
            val nextRow = row.coerceAtMost(model.rowCount - 1)
            table.selectionModel.setSelectionInterval(nextRow, nextRow)
        }
        showSelectedTemplate()
    }

    private fun moveSelectedTemplate(direction: Int) {
        val destination = model.move(table.selectedRow, direction)
        if (destination >= 0) table.selectionModel.setSelectionInterval(destination, destination)
    }

    private fun showSelectedTemplate() {
        val selected = model.rowAt(table.selectedRow)
        updatingFields = true
        contentArea.text = selected?.content.orEmpty()
        updatingFields = false
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

    private class TemplateTableModel : AbstractTableModel() {
        private val rows = mutableListOf<EditableTemplate>()

        override fun getRowCount(): Int = rows.size
        override fun getColumnCount(): Int = 1
        override fun getColumnName(column: Int): String = message("commit.templates.column")
        override fun getValueAt(row: Int, column: Int): Any = rows[row].name
        override fun isCellEditable(row: Int, column: Int): Boolean = !rows[row].isDefault

        override fun setValueAt(value: Any?, row: Int, column: Int) {
            if (rows[row].isDefault) return
            rows[row].name = value?.toString().orEmpty()
            fireTableCellUpdated(row, column)
        }

        fun load(templates: List<CommitTemplate>) {
            rows.clear()
            rows.addAll(templates.map { EditableTemplate(it.name, it.content, it.isDefault) })
            fireTableDataChanged()
        }

        fun templates(): List<CommitTemplate> = rows.map { CommitTemplate(it.name, it.content, it.isDefault) }

        fun rowAt(row: Int): EditableTemplate? = rows.getOrNull(row)

        fun addAfter(selectedRow: Int, template: CommitTemplate): Int {
            val firstCustomRow = rows.indexOfFirst { !it.isDefault }.takeIf { it >= 0 } ?: rows.size
            val row = if (selectedRow >= firstCustomRow) {
                (selectedRow + 1).coerceAtMost(rows.size)
            } else {
                firstCustomRow
            }
            rows.add(row, EditableTemplate(template.name, template.content, isDefault = false))
            fireTableRowsInserted(row, row)
            return row
        }

        fun canRemove(row: Int): Boolean = row in rows.indices && !rows[row].isDefault

        fun remove(row: Int): Boolean {
            if (!canRemove(row)) return false
            rows.removeAt(row)
            fireTableRowsDeleted(row, row)
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
            fireTableRowsUpdated(minOf(row, destination), maxOf(row, destination))
            return destination
        }
    }

    private data class EditableTemplate(var name: String, var content: String, val isDefault: Boolean)
}
