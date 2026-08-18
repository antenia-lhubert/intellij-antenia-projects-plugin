package fr.antenia.database

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBSplitter
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import fr.antenia.MyMessageBundle.message
import java.awt.BorderLayout
import java.awt.Component
import java.util.Locale
import javax.swing.AbstractListModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class DatabaseHostsConfigurable : SearchableConfigurable {
    private val model = HostListModel()
    private val hostList = JBList(model)
    private val hostField = JBTextField()
    private var component: JComponent? = null
    private var uiDisposable: Disposable? = null
    private var updatingField = false
    private var listenersConfigured = false

    override fun getDisplayName(): String = message("configurable.database.hosts.display.name")
    override fun getId(): String = "fr.antenia.databaseHosts"

    override fun createComponent(): JComponent {
        uiDisposable?.let(Disposer::dispose)
        uiDisposable = Disposer.newDisposable("Antenia database hosts UI")
        if (!listenersConfigured) {
            configureList()
            configureField()
            listenersConfigured = true
        }
        configureValidation()

        val hostMaster = ToolbarDecorator.createDecorator(hostList)
            .setAddAction { addHost() }
            .setRemoveAction { removeSelectedHost() }
            .setRemoveActionUpdater { model.canRemove(hostList.selectedIndex) }
            .disableUpDownActions()
            .createPanel()
        val details = FormBuilder.createFormBuilder()
            .addLabeledComponent(message("database.hosts.host"), hostField)
            .addComponentFillVertically(JPanel(), 0)
            .panel.apply { border = JBUI.Borders.emptyLeft(8) }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8)
            add(
                JBSplitter(false, 0.35f).apply {
                    firstComponent = hostMaster
                    secondComponent = details
                    splitterProportionKey = "fr.antenia.databaseHosts.splitter"
                },
                BorderLayout.CENTER,
            )
            component = this
            loadHosts()
        }
    }

    override fun isModified(): Boolean =
        component != null && model.hosts() != DatabaseConnectionProfileSettings.getInstance().hosts()

    override fun apply() {
        val hosts = model.hosts()
        if (hosts.any(String::isBlank)) {
            throw ConfigurationException(message("database.hosts.validation.required"))
        }
        if (hosts.map { it.lowercase(Locale.ROOT) }.distinct().size != hosts.size) {
            throw ConfigurationException(message("database.hosts.validation.unique"))
        }
        DatabaseConnectionProfileSettings.getInstance().replaceHosts(hosts)
    }

    override fun reset() {
        if (component != null) loadHosts()
    }

    override fun disposeUIResources() {
        component = null
        model.load(emptyList(), emptyList())
        uiDisposable?.let(Disposer::dispose)
        uiDisposable = null
    }

    private fun configureList() {
        hostList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        hostList.accessibleContext.accessibleName = message("database.hosts.list.accessible.name")
        hostList.cellRenderer = HostRenderer()
        hostList.addListSelectionListener {
            if (!it.valueIsAdjusting && !updatingField) showSelectedHost()
        }
    }

    private fun configureField() {
        hostField.accessibleContext.accessibleName = message("database.hosts.host")
        hostField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = changed()
            override fun removeUpdate(event: DocumentEvent) = changed()
            override fun changedUpdate(event: DocumentEvent) = changed()

            private fun changed() {
                if (updatingField) return
                model.rowAt(hostList.selectedIndex)?.takeUnless(HostRow::provided)?.let {
                    it.host = hostField.text
                    model.changed(hostList.selectedIndex)
                }
            }
        })
    }

    private fun configureValidation() {
        ComponentValidator(uiDisposable!!)
            .withValidator {
                val selected = model.rowAt(hostList.selectedIndex)
                when {
                    selected == null || selected.provided -> null
                    hostField.text.isBlank() ->
                        ValidationInfo(message("database.hosts.validation.required"), hostField)
                    model.hasDuplicateHost(hostField.text, hostList.selectedIndex) ->
                        ValidationInfo(message("database.hosts.validation.unique"), hostField)
                    else -> null
                }
            }
            .andRegisterOnDocumentListener(hostField)
            .installOn(hostField)
    }

    private fun loadHosts() {
        val provided = DatabaseConnectionProfiles.defaultHosts()
        model.load(provided, DatabaseConnectionProfileSettings.getInstance().hosts())
        hostList.selectedIndex = if (model.rowCount > 0) 0 else -1
        showSelectedHost()
    }

    private fun addHost() {
        val row = model.add(uniqueHost(message("database.hosts.new")))
        hostList.selectedIndex = row
        hostList.ensureIndexIsVisible(row)
        hostField.requestFocusInWindow()
        hostField.selectAll()
    }

    private fun removeSelectedHost() {
        val row = hostList.selectedIndex
        if (!model.remove(row)) return
        hostList.selectedIndex = row.coerceAtMost(model.rowCount - 1)
        showSelectedHost()
    }

    private fun showSelectedHost() {
        val selected = model.rowAt(hostList.selectedIndex)
        updatingField = true
        hostField.text = selected?.host.orEmpty()
        updatingField = false
        hostField.isEnabled = selected != null
        hostField.isEditable = selected != null && !selected.provided
    }

    private fun uniqueHost(baseName: String): String {
        val hosts = model.hosts().mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }
        var suffix = 2
        var candidate = baseName
        while (candidate.lowercase(Locale.ROOT) in hosts) {
            candidate = "$baseName $suffix"
            suffix++
        }
        return candidate
    }

    private inner class HostRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            selected: Boolean,
            focus: Boolean,
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, selected, focus)
            text = (value as? HostRow)?.let {
                if (it.provided) message("database.hosts.provided", it.host) else it.host
            }.orEmpty()
            return component
        }
    }

    private class HostListModel : AbstractListModel<HostRow>() {
        private val rows = mutableListOf<HostRow>()

        val rowCount: Int get() = rows.size
        override fun getSize(): Int = rows.size
        override fun getElementAt(index: Int): HostRow = rows[index]

        fun load(providedHosts: List<String>, hosts: List<String>) {
            rows.clear()
            rows.addAll(providedHosts.map { HostRow(it, provided = true) })
            rows.addAll(
                hosts.filter { host ->
                    providedHosts.none { it.equals(host, ignoreCase = true) }
                }.map { HostRow(it, provided = false) },
            )
            fireContentsChanged(this, 0, maxOf(0, rows.lastIndex))
        }

        fun hosts(): List<String> = rows.map { it.host.trim() }
        fun rowAt(row: Int): HostRow? = rows.getOrNull(row)

        fun hasDuplicateHost(host: String, exceptRow: Int): Boolean {
            val candidate = host.trim()
            return rows.indices.any {
                it != exceptRow && rows[it].host.trim().equals(candidate, ignoreCase = true)
            }
        }

        fun changed(row: Int) {
            if (row in rows.indices) fireContentsChanged(this, row, row)
        }

        fun add(host: String): Int {
            val row = rows.size
            rows.add(HostRow(host, provided = false))
            fireIntervalAdded(this, row, row)
            return row
        }

        fun canRemove(row: Int): Boolean = row in rows.indices && !rows[row].provided

        fun remove(row: Int): Boolean {
            if (!canRemove(row)) return false
            rows.removeAt(row)
            fireIntervalRemoved(this, row, row)
            return true
        }
    }

    private data class HostRow(var host: String, val provided: Boolean)
}
