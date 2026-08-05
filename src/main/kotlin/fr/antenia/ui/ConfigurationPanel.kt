package fr.antenia.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.Alarm
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import fr.antenia.config.ConfigurationFiles
import fr.antenia.config.OrderedProperties
import fr.antenia.config.ProjectConfigurationState
import fr.antenia.config.PropertyLine
import fr.antenia.credentials.GlobalDatabaseSettings
import fr.antenia.credentials.DatabaseCredentials
import fr.antenia.credentials.ProjectDatabaseCredentials
import fr.antenia.database.DatabaseProfileSynchronizer
import fr.antenia.database.MysqlConnection
import fr.antenia.project.DatabaseKeys
import fr.antenia.project.NeoProject
import fr.antenia.project.NeoSchema
import fr.antenia.notifications.AnteniaNotifications
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.FlowLayout
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.DefaultCellEditor
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent as SwingDocumentEvent
import javax.swing.event.DocumentListener as SwingDocumentListener
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

class ConfigurationPanel(
    private val project: Project,
    private val neoProject: NeoProject,
) : Disposable {
    private val logger = Logger.getInstance(ConfigurationPanel::class.java)
    private val schema = NeoSchema.forType(neoProject.type)
    private val state = ProjectConfigurationState.getInstance(project)
    private lateinit var document: OrderedProperties
    private lateinit var file: Path
    private var virtualFile: VirtualFile? = null
    private var changingFile = false
    private var model = ConfigurationTableModel(schema, ::persist)
    private val table = JBTable(model)
    private val cards = JPanel(CardLayout())
    private val databaseForm = schema.database?.let { DatabaseForm(it) }
    private val environmentForm = schema.environmentKey?.let { EnvironmentForm(it) }
    private val status = JBLabel()
    private val databaseProfileAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private lateinit var editorSplitter: JBSplitter
    private lateinit var formPane: JComponent

    val component: JComponent = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty(8)
        add(createEditor(), BorderLayout.CENTER)
        add(status, BorderLayout.SOUTH)
    }

    init {
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.setShowGrid(false)
        table.emptyText.text = "No configuration entries"
        table.columnModel.getColumn(0).preferredWidth = 220
        table.columnModel.getColumn(1).preferredWidth = 500
        table.columnModel.getColumn(0).cellEditor = DefaultCellEditor(JComboBox<String>().apply {
            isEditable = true
            model = DefaultComboBoxModel(schema.knownKeys.toTypedArray())
        })
        table.setDefaultRenderer(Any::class.java, RowRenderer())
        table.selectionModel.addListSelectionListener { showSelectedForm() }
        reloadFromDisk()
        installFileListeners()
    }

    override fun dispose() = Unit

    private fun createEditor(): JComponent {
        databaseForm?.let { cards.add(it.component, "database") }
        environmentForm?.let { cards.add(it.component, "environment") }
        val decorator = ToolbarDecorator.createDecorator(table)
            .setAddAction { addEntry() }
            .setRemoveAction { removeSelected() }
            .setMoveUpAction { moveSelected(-1) }
            .setMoveDownAction { moveSelected(1) }
            .addExtraAction(object : com.intellij.openapi.actionSystem.AnAction("Add comment", "Add a comment line", com.intellij.icons.AllIcons.FileTypes.Text) {
                override fun actionPerformed(event: com.intellij.openapi.actionSystem.AnActionEvent) = addLine(PropertyLine.Comment("# "))
            })
            .addExtraAction(object : com.intellij.openapi.actionSystem.AnAction("Add blank", "Add an empty line", com.intellij.icons.AllIcons.Actions.SplitVertically) {
                override fun actionPerformed(event: com.intellij.openapi.actionSystem.AnActionEvent) = addLine(PropertyLine.Blank())
            })
            .addExtraAction(object : com.intellij.openapi.actionSystem.AnAction("Reset", "Reset to the default configuration", com.intellij.icons.AllIcons.Actions.Rollback) {
                override fun actionPerformed(event: com.intellij.openapi.actionSystem.AnActionEvent) = reset()
            })
        formPane = JBScrollPane(cards).apply { border = JBUI.Borders.emptyTop(8) }
        editorSplitter = JBSplitter(true, 0.67f).apply {
            firstComponent = decorator.createPanel()
        }
        return editorSplitter
    }

    private fun addEntry() = addLine(PropertyLine.Entry(nextAvailableKey(), ""))

    private fun addLine(line: PropertyLine) {
        val row = model.addAfter(table.selectedRow, line)
        table.selectionModel.setSelectionInterval(row, row)
        if (line is PropertyLine.Entry) table.editCellAt(row, 0)
    }

    private fun removeSelected() {
        val row = table.selectedRow
        if (row < 0 || !model.remove(row)) return
        if (model.rowCount > 0) table.selectionModel.setSelectionInterval(row.coerceAtMost(model.rowCount - 1), row.coerceAtMost(model.rowCount - 1))
    }

    private fun moveSelected(direction: Int) {
        val destination = model.move(table.selectedRow, direction)
        if (destination >= 0) table.selectionModel.setSelectionInterval(destination, destination)
    }

    private fun nextAvailableKey(): String = schema.knownKeys.firstOrNull { key ->
        key !in schema.specialKeys && document.lines.filterIsInstance<PropertyLine.Entry>().none { it.key == key }
    } ?: "new.key"

    private fun reset() {
        if (Messages.showYesNoDialog(project, "Reset ${neoProject.type.configurationFile} to its defaults?", "Reset Neo Configuration", Messages.getWarningIcon()) != Messages.YES) return
        changingFile = true
        try {
            ConfigurationFiles.reset(project, neoProject.type)
        } catch (exception: Exception) {
            reportFailure("configuration-reset", "Neo configuration could not be reset", exception)
            return
        } finally {
            changingFile = false
        }
        reloadFromDisk()
    }

    private fun reloadFromDisk() {
        runCatching {
            file = ConfigurationFiles.ensureCreated(project, neoProject.type)
            load(ConfigurationFiles.read(file))
            attachDocumentListener()
            DatabaseProfileSynchronizer.update(project, neoProject)
            status.text = "${neoProject.type.displayName} | $file"
        }.onFailure {
            logger.warn("Unable to load Neo configuration for '${project.name}'", it)
            AnteniaNotifications.failure(
                project,
                "configuration-panel-load",
                "Neo configuration could not be loaded",
                "${it.message ?: it.javaClass.simpleName}. See the IDE log for details.",
            )
            status.text = "Unable to load configuration: ${it.message}"
            status.foreground = JBColor.RED
        }
    }

    private fun load(newDocument: OrderedProperties) {
        val selectedRow = table.selectedRow
        schema.database?.let { newDocument.regroup(databaseKeys(it)) }
        schema.environmentKey?.let { newDocument.regroup(setOf(it)) }
        document = newDocument
        model.load(document)
        databaseForm?.load()
        environmentForm?.load()
        val rowToSelect = selectedRow.takeIf { it in 0 until model.rowCount } ?: model.databaseRow()
        if (rowToSelect >= 0) {
            table.selectionModel.setSelectionInterval(rowToSelect, rowToSelect)
        }
        showSelectedForm()
    }

    private fun persist() {
        if (changingFile) return
        changingFile = true
        try {
            ConfigurationFiles.write(project, file, document)
            databaseProfileAlarm.cancelAllRequests()
            databaseProfileAlarm.addRequest({
                runCatching { DatabaseProfileSynchronizer.update(project, neoProject) }
                    .onFailure { reportFailure("database-profile-update", "MySQL profile could not be synchronized", it) }
            }, 500)
            status.text = "Saved $file"
            status.foreground = UIUtil.getLabelForeground()
            logger.debug("Saved Neo configuration for '${project.name}': $file")
        } catch (exception: Exception) {
            reportFailure("configuration-save", "Neo configuration could not be saved", exception)
        } finally {
            changingFile = false
        }
    }

    private fun installFileListeners() {
        project.messageBus.connect(this).subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                if (!changingFile && events.any { it.path.replace('\\', '/') == file.toString().replace('\\', '/') }) {
                    ApplicationManager.getApplication().invokeLater { if (!changingFile && Files.exists(file)) reloadFromDisk() }
                }
            }
        })
    }

    private fun attachDocumentListener() {
        val current = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(file) ?: return
        if (current == virtualFile) return
        virtualFile = current
        FileDocumentManager.getInstance().getDocument(current)?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (!changingFile && current == virtualFile) load(fr.antenia.config.OrderedPropertiesCodec.parse(event.document.text))
            }
        }, this)
    }

    private fun showSelectedForm() {
        val card = when (model.rowAt(table.selectedRow)) {
            is LogicalRow.Database -> "database"
            is LogicalRow.Environment -> "environment"
            else -> null
        }
        if (card == null) {
            editorSplitter.secondComponent = null
        } else {
            (cards.layout as CardLayout).show(cards, card)
            editorSplitter.secondComponent = formPane
        }
    }

    private fun refreshTablePreservingSelection() {
        model.sync(document)
    }

    private fun reportFailure(key: String, title: String, exception: Throwable) {
        logger.error("$title for '${project.name}'", exception)
        AnteniaNotifications.failure(
            project,
            key,
            title,
            "${exception.message ?: exception.javaClass.simpleName}. See the IDE log for details.",
        )
        status.text = "$title: ${exception.message ?: exception.javaClass.simpleName}"
        status.foreground = JBColor.RED
    }

    private fun databaseKeys(keys: DatabaseKeys): Set<String> = buildSet {
        add(keys.url)
        keys.database?.let(::add)
        addAll(keys.usernames)
        addAll(keys.passwords)
    }

    private inner class DatabaseForm(private val keys: DatabaseKeys) {
        private val host = JComboBox(arrayOf(
            "antenia-dev-mysql5.leaderinfo.com",
            "antenia-dev-mysql8.leaderinfo.com",
            "mysql8-4-5-dev.antenia.com",
        )).apply { isEditable = true }
        private val port = JBTextField("3306")
        private val database = JBTextField()
        private val override = JBCheckBox("Override global credentials for this project")
        private val username = JBTextField()
        private val password = JBPasswordField()
        private var loading = false
        private var query = "?autoReconnect=true"

        val component: JComponent = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("Database").apply { font = font.deriveFont(font.style or java.awt.Font.BOLD) })
            .addLabeledComponent("Host:", host)
            .addLabeledComponent("Port:", port)
            .addLabeledComponent("Database:", database)
            .addComponent(override)
            .addLabeledComponent("Username:", username)
            .addLabeledComponent("Password:", password)
            .addComponentFillVertically(JPanel(), 0)
            .panel.apply { border = JBUI.Borders.empty(12) }

        init {
            host.addActionListener { update() }
            override.addActionListener {
                if (loading) return@addActionListener
                val wasOverriding = state.overrideGlobalCredentials
                if (wasOverriding && !override.isSelected) saveOverride()
                state.overrideGlobalCredentials = override.isSelected
                updateEnabled()
                loading = true
                val restoredOverride = if (override.isSelected) loadOverride() else {
                    loadGlobal()
                    false
                }
                loading = false
                logger.info(
                    "Database credential override changed for '${project.name}': enabled=${override.isSelected}, " +
                        "activeSource=${if (override.isSelected) "project" else "global"}, " +
                        "restoredSavedOverride=$restoredOverride, retainedProjectOverride=${ProjectDatabaseCredentials.credentials(project) != null}",
                )
                update()
            }
            listOf(port, database, username).forEach { it.document.onChange(::update) }
            password.document.onChange(::update)
        }

        fun load() {
            loading = true
            val parsed = MysqlConnection.parse(document.value(keys.url).orEmpty())
            query = parsed?.query ?: "?autoReconnect=true"
            host.selectedItem = parsed?.host.orEmpty().ifEmpty {
                if (neoProject.javaVersion >= 17) "mysql8-4-5-dev.antenia.com" else "antenia-dev-mysql8.leaderinfo.com"
            }
            port.text = (parsed?.port ?: 3306).toString()
            database.text = keys.database?.let(document::value)?.takeIf { it.isNotEmpty() } ?: parsed?.database.orEmpty()
            override.isSelected = state.overrideGlobalCredentials
            username.text = keys.usernames.firstNotNullOfOrNull(document::value).orEmpty()
            password.text = keys.passwords.firstNotNullOfOrNull(document::value).orEmpty()
            if (override.isSelected) saveOverride()
            updateEnabled()
            loading = false
        }

        private fun loadGlobal() {
            val global = GlobalDatabaseSettings.getInstance().credentials()
            username.text = global.username
            password.text = global.password
        }

        private fun loadOverride(): Boolean {
            return ProjectDatabaseCredentials.credentials(project)?.let {
                username.text = it.username
                password.text = it.password
                true
            } ?: false
        }

        private fun saveOverride() {
            ProjectDatabaseCredentials.save(
                project,
                DatabaseCredentials(username.text, password.password.concatToString()),
            )
        }

        private fun updateEnabled() {
            val local = override.isSelected
            username.isEnabled = local
            password.isEnabled = local
        }

        private fun update() {
            if (loading) return
            val selectedHost = host.editor.item?.toString().orEmpty()
            val selectedPort = port.text.toIntOrNull() ?: 3306
            val selectedDatabase = database.text
            document.setValue(keys.url, MysqlConnection.build(selectedHost, selectedPort, selectedDatabase, query))
            keys.database?.let { document.setValue(it, selectedDatabase) }
            keys.usernames.forEach { document.setValue(it, username.text) }
            val passwordValue = password.password.concatToString()
            keys.passwords.forEach { document.setValue(it, passwordValue) }
            if (override.isSelected) saveOverride()
            refreshTablePreservingSelection()
            persist()
        }
    }

    private inner class EnvironmentForm(private val key: String) {
        private val environment = JComboBox(arrayOf("DEV", "TEST", "RECETTE", "PROD")).apply { isEditable = true }
        private var loading = false
        val component: JComponent = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("Environment").apply { font = font.deriveFont(font.style or java.awt.Font.BOLD) })
            .addLabeledComponent("Environment:", environment)
            .addComponentFillVertically(JPanel(), 0)
            .panel.apply { border = JBUI.Borders.empty(12) }

        init {
            environment.addActionListener {
                if (!loading) {
                    document.setValue(key, environment.editor.item?.toString().orEmpty())
                    refreshTablePreservingSelection()
                    persist()
                }
            }
        }

        fun load() {
            loading = true
            environment.selectedItem = document.value(key).orEmpty()
            loading = false
        }
    }

    private inner class RowRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(table: javax.swing.JTable, value: Any?, selected: Boolean, focus: Boolean, row: Int, column: Int): Component {
            val component = super.getTableCellRendererComponent(table, value, selected, focus, row, column)
            val item = model.rowAt(row)
            val emptyValue = item is LogicalRow.Entry && column == 1 && item.line.value.isEmpty()
            font = when {
                item is LogicalRow.Database || item is LogicalRow.Environment -> table.font.deriveFont(java.awt.Font.BOLD)
                emptyValue -> table.font.deriveFont(java.awt.Font.ITALIC)
                else -> table.font.deriveFont(java.awt.Font.PLAIN)
            }
            if (emptyValue) text = "<empty>"
            foreground = when {
                selected -> table.selectionForeground
                item is LogicalRow.Comment || item is LogicalRow.Blank || emptyValue -> UIUtil.getContextHelpForeground()
                else -> table.foreground
            }
            return component
        }
    }
}

private sealed interface LogicalRow {
    val lines: List<PropertyLine>
    data class Entry(val line: PropertyLine.Entry) : LogicalRow { override val lines = listOf(line) }
    data class Comment(val line: PropertyLine.Comment) : LogicalRow { override val lines = listOf(line) }
    data class Blank(val line: PropertyLine.Blank) : LogicalRow { override val lines = listOf(line) }
    data class Database(override val lines: List<PropertyLine>) : LogicalRow
    data class Environment(override val lines: List<PropertyLine>) : LogicalRow
}

private class ConfigurationTableModel(
    private val schema: NeoSchema,
    private val changed: () -> Unit,
) : AbstractTableModel() {
    private lateinit var document: OrderedProperties
    private var rows = mutableListOf<LogicalRow>()

    fun load(document: OrderedProperties) {
        this.document = document
        rows = logicalRows(document)
        fireTableDataChanged()
    }

    fun sync(document: OrderedProperties) {
        this.document = document
        rows = logicalRows(document)
    }

    override fun getRowCount(): Int = rows.size
    override fun getColumnCount(): Int = 2
    override fun getColumnName(column: Int): String = if (column == 0) "Key / type" else "Value"
    override fun isCellEditable(row: Int, column: Int): Boolean =
        rows.getOrNull(row) is LogicalRow.Entry || (rows.getOrNull(row) is LogicalRow.Comment && column == 1)

    override fun getValueAt(row: Int, column: Int): Any = when (val item = rows[row]) {
        is LogicalRow.Entry -> if (column == 0) item.line.key else item.line.value
        is LogicalRow.Comment -> if (column == 0) "Comment" else item.line.raw.trimStart('#', '!', ' ')
        is LogicalRow.Blank -> if (column == 0) "Blank line" else ""
        is LogicalRow.Database -> if (column == 0) "Database credentials" else "Edit in the form below"
        is LogicalRow.Environment -> if (column == 0) "Environment" else "Edit in the form below"
    }

    override fun setValueAt(value: Any?, row: Int, column: Int) {
        when (val item = rows[row]) {
            is LogicalRow.Entry -> if (column == 0) item.line.key = value.toString() else item.line.value = value.toString()
            is LogicalRow.Comment -> if (column == 1) replaceLine(item.line, PropertyLine.Comment("# ${value.toString()}"))
            else -> return
        }
        load(document)
        changed()
    }

    fun rowAt(row: Int): LogicalRow? = rows.getOrNull(row)

    fun databaseRow(): Int = rows.indexOfFirst { it is LogicalRow.Database }

    fun addAfter(selected: Int, line: PropertyLine): Int {
        val insertRow = if (selected in rows.indices) selected + 1 else rows.size
        val blocks = rows.map { it.lines }.toMutableList()
        blocks.add(insertRow, listOf(line))
        rebuild(blocks)
        changed()
        return insertRow
    }

    fun remove(row: Int): Boolean {
        val item = rows.getOrNull(row) ?: return false
        if (item is LogicalRow.Database || item is LogicalRow.Environment) return false
        document.lines.removeAll(item.lines.toSet())
        load(document)
        changed()
        return true
    }

    fun move(row: Int, direction: Int): Int {
        val destination = row + direction
        if (row !in rows.indices || destination !in rows.indices) return -1
        val blocks = rows.map { it.lines }.toMutableList()
        val item = blocks.removeAt(row)
        blocks.add(destination, item)
        rebuild(blocks)
        changed()
        return destination
    }

    private fun replaceLine(old: PropertyLine, replacement: PropertyLine) {
        val index = document.lines.indexOf(old)
        if (index >= 0) document.lines[index] = replacement
    }

    private fun rebuild(blocks: List<List<PropertyLine>>) {
        document.lines.clear()
        blocks.forEach(document.lines::addAll)
        load(document)
    }

    private fun logicalRows(document: OrderedProperties): MutableList<LogicalRow> {
        val databaseKeys = schema.database?.let { keys -> buildSet {
            add(keys.url); keys.database?.let(::add); addAll(keys.usernames); addAll(keys.passwords)
        } }.orEmpty()
        val databaseLines = document.lines.filter { it is PropertyLine.Entry && it.key in databaseKeys }
        val environmentLines = document.lines.filter { it is PropertyLine.Entry && it.key == schema.environmentKey }
        var databaseAdded = false
        var environmentAdded = false
        return document.lines.mapNotNullTo(mutableListOf()) { line ->
            when {
                line is PropertyLine.Entry && line.key in databaseKeys -> if (!databaseAdded) LogicalRow.Database(databaseLines).also { databaseAdded = true } else null
                line is PropertyLine.Entry && line.key == schema.environmentKey -> if (!environmentAdded) LogicalRow.Environment(environmentLines).also { environmentAdded = true } else null
                line is PropertyLine.Entry -> LogicalRow.Entry(line)
                line is PropertyLine.Comment -> LogicalRow.Comment(line)
                line is PropertyLine.Blank -> LogicalRow.Blank(line)
                else -> null
            }
        }
    }
}


private fun javax.swing.text.Document.onChange(action: () -> Unit) {
    addDocumentListener(object : SwingDocumentListener {
        override fun insertUpdate(event: SwingDocumentEvent?) = action()
        override fun removeUpdate(event: SwingDocumentEvent?) = action()
        override fun changedUpdate(event: SwingDocumentEvent?) = action()
    })
}
