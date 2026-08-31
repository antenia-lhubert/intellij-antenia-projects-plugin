package fr.antenia.database

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBSplitter
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import fr.antenia.MyMessageBundle.message
import fr.antenia.credentials.DatabaseCredentials
import fr.antenia.credentials.ProfileDatabaseCredentials
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

class DatabaseConnectionProfilesConfigurable : SearchableConfigurable {
    private val model = ProfileListModel()
    private val profileList = JBList(model)
    private val nameField = JBTextField()
    private val profileFields = DatabaseProfileFields(
        message("configuration.database.host.save.tooltip"),
        message("configuration.database.hosts.manage.tooltip"),
        manageHosts = {
            ShowSettingsUtil.getInstance().showSettingsDialog(null, DatabaseHostsConfigurable::class.java)
        },
        openGlobalCredentials = {
            ShowSettingsUtil.getInstance()
                .showSettingsDialog(null, fr.antenia.credentials.GlobalDatabaseConfigurable::class.java)
        },
    )
    private val hostSelector = profileFields.hostSelector
    private val portField = profileFields.portField
    private val databaseField = profileFields.databaseField
    private val databaseEdiField = profileFields.databaseEdiField
    private val overrideField = profileFields.overrideField
    private val usernameField = profileFields.usernameField
    private val passwordField = profileFields.passwordField
    private var component: JComponent? = null
    private var uiDisposable: Disposable? = null
    private var updatingFields = false
    private var listenersConfigured = false

    override fun getDisplayName(): String = message("configurable.database.profiles.display.name")
    override fun getId(): String = "fr.antenia.databaseProfiles"

    override fun createComponent(): JComponent {
        uiDisposable?.let(Disposer::dispose)
        uiDisposable = Disposer.newDisposable("Antenia database profiles UI")
        if (!listenersConfigured) {
            configureList()
            configureFields()
            listenersConfigured = true
        }
        configureValidation()

        val profileMaster = ToolbarDecorator.createDecorator(profileList)
            .setAddAction { addProfile() }
            .setRemoveAction { removeSelectedProfile() }
            .setRemoveActionUpdater { model.canRemove(profileList.selectedIndex) }
            .disableUpDownActions()
            .addExtraAction(object : DumbAwareAction(
                message("database.profiles.clone"),
                message("database.profiles.clone.description"),
                AllIcons.Actions.Copy,
            ) {
                override fun actionPerformed(event: AnActionEvent) = cloneSelectedProfile()

                override fun update(event: AnActionEvent) {
                    event.presentation.isEnabled = model.rowAt(profileList.selectedIndex) != null
                }

                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
            })
            .createPanel()
        val details = profileFields.createComponent(message("database.profiles.name"), nameField).apply {
            border = JBUI.Borders.emptyLeft(8)
            minimumSize = java.awt.Dimension(0, 0)
        }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8)
            add(
                JBSplitter(false, 0.35f).apply {
                    firstComponent = profileMaster
                    secondComponent = details
                    splitterProportionKey = "fr.antenia.databaseProfiles.splitter"
                },
                BorderLayout.CENTER,
            )
            component = this
            loadProfiles()
        }
    }

    override fun isModified(): Boolean =
        component != null && (
            model.profiles() != DatabaseConnectionProfileSettings.getInstance().profiles() || model.credentialsModified()
        )

    override fun apply() {
        val profiles = model.profiles()
        if (profiles.any { it.name.isBlank() || it.host.isBlank() || it.port !in 1..65535 }) {
            throw ConfigurationException(message("database.profiles.validation.required"))
        }
        if (profiles.map { it.name.lowercase(Locale.ROOT) }.distinct().size != profiles.size) {
            throw ConfigurationException(message("database.profiles.validation.unique"))
        }
        val settings = DatabaseConnectionProfileSettings.getInstance()
        val removedIds = settings.profiles().mapTo(mutableSetOf()) { it.id } - profiles.mapTo(mutableSetOf()) { it.id }
        model.rows().forEach { row ->
            ProfileDatabaseCredentials.save(row.id, DatabaseCredentials(row.username, row.password))
        }
        settings.replaceProfiles(profiles)
        model.rows().forEach(EditableProfile::markCredentialsSaved)
        removedIds.forEach(ProfileDatabaseCredentials::clear)
    }

    override fun reset() {
        if (component != null) loadProfiles()
    }

    override fun disposeUIResources() {
        component = null
        model.load(emptyList())
        uiDisposable?.let(Disposer::dispose)
        uiDisposable = null
    }

    private fun configureList() {
        profileList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        profileList.emptyText
            .appendText(message("database.profiles.empty"))
            .appendLine("")
            .appendText(message("database.profiles.empty.action"), SimpleTextAttributes.LINK_ATTRIBUTES) { addProfile() }
        profileList.accessibleContext.accessibleName = message("database.profiles.list.accessible.name")
        profileList.cellRenderer = ProfileRenderer()
        profileList.addListSelectionListener {
            if (!it.valueIsAdjusting && !updatingFields) showSelectedProfile()
        }
    }

    private fun configureFields() {
        nameField.accessibleContext.accessibleName = message("database.profiles.name")
        hostSelector.comboBox.accessibleContext.accessibleName = message("database.profiles.host")
        portField.accessibleContext.accessibleName = message("database.profiles.port")
        databaseField.accessibleContext.accessibleName = message("database.profiles.database")
        databaseEdiField.accessibleContext.accessibleName = message("database.profiles.database.edi")
        overrideField.accessibleContext.accessibleName = message("database.profiles.override")
        usernameField.accessibleContext.accessibleName = message("database.profiles.username")
        passwordField.accessibleContext.accessibleName = message("database.profiles.password")
        nameField.document.addDocumentListener(fieldListener {
            model.rowAt(profileList.selectedIndex)?.name = nameField.text
            model.changed(profileList.selectedIndex)
        })
        hostSelector.addChangeListener {
            if (!updatingFields) model.rowAt(profileList.selectedIndex)?.host = currentHost()
        }
        portField.document.addDocumentListener(fieldListener {
            model.rowAt(profileList.selectedIndex)?.port = portField.text
        })
        databaseField.document.addDocumentListener(fieldListener {
            model.rowAt(profileList.selectedIndex)?.database = databaseField.text
        })
        databaseEdiField.document.addDocumentListener(fieldListener {
            model.rowAt(profileList.selectedIndex)?.databaseEdi = databaseEdiField.text
        })
        overrideField.addActionListener {
            if (!updatingFields) {
                model.rowAt(profileList.selectedIndex)?.overrideGlobalCredentials = overrideField.isSelected
                updateCredentialFields()
            }
        }
        usernameField.document.addDocumentListener(fieldListener {
            model.rowAt(profileList.selectedIndex)?.username = usernameField.text
        })
        passwordField.document.addDocumentListener(fieldListener {
            model.rowAt(profileList.selectedIndex)?.password = passwordField.password.concatToString()
        })
    }

    private fun configureValidation() {
        ComponentValidator(uiDisposable!!)
            .withValidator {
                val selected = model.rowAt(profileList.selectedIndex)
                when {
                    selected == null -> null
                    nameField.text.isBlank() -> ValidationInfo(message("database.profiles.validation.name"), nameField)
                    model.hasDuplicateName(nameField.text, profileList.selectedIndex) ->
                        ValidationInfo(message("database.profiles.validation.unique"), nameField)
                    else -> null
                }
            }
            .andRegisterOnDocumentListener(nameField)
            .installOn(nameField)
        profileFields.installValidation(uiDisposable!!) { model.rowAt(profileList.selectedIndex) != null }
    }

    private fun loadProfiles() {
        updatingFields = true
        hostSelector.refresh()
        model.load(DatabaseConnectionProfileSettings.getInstance().profiles())
        updatingFields = false
        profileList.selectedIndex = if (model.rowCount > 0) 0 else -1
        showSelectedProfile()
    }

    private fun addProfile() {
        val row = model.add(
            DatabaseConnectionProfile(
                uniqueProfileName(message("database.profiles.new")),
                "",
                3306,
                "",
                id = DatabaseConnectionProfiles.newId(),
            ),
        )
        selectAndFocusName(row)
    }

    private fun cloneSelectedProfile() {
        val selected = model.rowAt(profileList.selectedIndex) ?: return
        val row = model.add(
            selected.toProfile().copy(
                name = uniqueProfileName(message("database.profiles.copy", selected.name)),
                id = DatabaseConnectionProfiles.newId(),
            ),
            DatabaseCredentials(selected.username, selected.password),
        )
        selectAndFocusName(row)
    }

    private fun removeSelectedProfile() {
        val row = profileList.selectedIndex
        if (!model.remove(row)) return
        profileList.selectedIndex = row.coerceAtMost(model.rowCount - 1)
        showSelectedProfile()
    }

    private fun selectAndFocusName(row: Int) {
        profileList.selectedIndex = row
        profileList.ensureIndexIsVisible(row)
        nameField.requestFocusInWindow()
        nameField.selectAll()
    }

    private fun showSelectedProfile() {
        val selected = model.rowAt(profileList.selectedIndex)
        updatingFields = true
        nameField.text = selected?.name.orEmpty()
        hostSelector.setHost(selected?.host.orEmpty())
        portField.text = selected?.port.orEmpty()
        databaseField.text = selected?.database.orEmpty()
        databaseEdiField.text = selected?.databaseEdi.orEmpty()
        overrideField.isSelected = selected?.overrideGlobalCredentials == true
        usernameField.text = selected?.username.orEmpty()
        passwordField.text = selected?.password.orEmpty()
        updatingFields = false
        val editable = selected != null
        nameField.isEnabled = selected != null
        nameField.isEditable = editable
        profileFields.setEnabled(editable)
    }

    private fun uniqueProfileName(baseName: String): String {
        val names = model.profiles().mapTo(mutableSetOf()) { it.name.lowercase(Locale.ROOT) }
        var suffix = 2
        var candidate = baseName
        while (candidate.lowercase(Locale.ROOT) in names) {
            candidate = "$baseName $suffix"
            suffix++
        }
        return candidate
    }

    private fun currentHost(): String = hostSelector.host()

    private fun updateCredentialFields() {
        profileFields.updateCredentialFields(model.rowAt(profileList.selectedIndex) != null)
    }

    private fun fieldListener(update: () -> Unit): DocumentListener = object : DocumentListener {
        override fun insertUpdate(event: DocumentEvent) = changed()
        override fun removeUpdate(event: DocumentEvent) = changed()
        override fun changedUpdate(event: DocumentEvent) = changed()

        private fun changed() {
            if (!updatingFields) update()
        }
    }

    private inner class ProfileRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            selected: Boolean,
            focus: Boolean,
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, selected, focus)
            text = (value as? EditableProfile)?.name.orEmpty()
            return component
        }
    }

    private class ProfileListModel : AbstractListModel<EditableProfile>() {
        private val rows = mutableListOf<EditableProfile>()

        val rowCount: Int get() = rows.size
        override fun getSize(): Int = rows.size
        override fun getElementAt(index: Int): EditableProfile = rows[index]

        fun load(profiles: List<DatabaseConnectionProfile>) {
            rows.clear()
            rows.addAll(profiles.map { profile ->
                EditableProfile(profile, ProfileDatabaseCredentials.credentials(profile.id) ?: DatabaseCredentials("", ""))
            })
            fireContentsChanged(this, 0, maxOf(0, rows.lastIndex))
        }

        fun profiles(): List<DatabaseConnectionProfile> = rows.map(EditableProfile::toProfile)
        fun rows(): List<EditableProfile> = rows
        fun credentialsModified(): Boolean = rows.any(EditableProfile::credentialsModified)
        fun rowAt(row: Int): EditableProfile? = rows.getOrNull(row)
        fun hasDuplicateName(name: String, exceptRow: Int): Boolean = rows.indices.any {
            it != exceptRow && rows[it].name.equals(name, ignoreCase = true)
        }

        fun changed(row: Int) {
            if (row in rows.indices) fireContentsChanged(this, row, row)
        }

        fun add(profile: DatabaseConnectionProfile, credentials: DatabaseCredentials = DatabaseCredentials("", "")): Int {
            val row = rows.size
            rows.add(EditableProfile(profile, credentials))
            fireIntervalAdded(this, row, row)
            return row
        }

        fun canRemove(row: Int): Boolean = row in rows.indices

        fun remove(row: Int): Boolean {
            if (!canRemove(row)) return false
            rows.removeAt(row)
            fireIntervalRemoved(this, row, row)
            return true
        }
    }

    private data class EditableProfile(
        var name: String,
        var host: String,
        var port: String,
        var database: String,
        var databaseEdi: String,
        var overrideGlobalCredentials: Boolean,
        val id: String,
        var username: String,
        var password: String,
        private var savedUsername: String,
        private var savedPassword: String,
    ) {
        constructor(profile: DatabaseConnectionProfile, credentials: DatabaseCredentials) : this(
            profile.name,
            profile.host,
            profile.port.toString(),
            profile.database,
            profile.databaseEdi,
            profile.overrideGlobalCredentials,
            profile.id,
            credentials.username,
            credentials.password,
            credentials.username,
            credentials.password,
        )

        fun toProfile(): DatabaseConnectionProfile = DatabaseConnectionProfile(
            name = name,
            host = host,
            port = port.toIntOrNull() ?: 0,
            database = database,
            overrideGlobalCredentials = overrideGlobalCredentials,
            id = id,
            databaseEdi = databaseEdi,
        )

        fun credentialsModified(): Boolean = username != savedUsername || password != savedPassword

        fun markCredentialsSaved() {
            savedUsername = username
            savedPassword = password
        }
    }
}
