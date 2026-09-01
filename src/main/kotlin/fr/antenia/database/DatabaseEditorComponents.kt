package fr.antenia.database

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import fr.antenia.MyMessageBundle.message
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

internal class DatabaseHostSelector(
    saveTooltip: String,
    manageTooltip: String,
    private val manageHosts: () -> Unit,
) {
    val comboBox = ComboBox<String>().apply {
        isEditable = true
        prototypeDisplayValue = "mysql.example.com"
    }
    private val saveAction = DatabaseEditorAction(
        message("configuration.database.host.save"),
        saveTooltip,
        AllIcons.Actions.AddFile,
    ) {
        if (DatabaseConnectionProfileSettings.getInstance().addHost(host())) refresh()
    }
    private val manageAction = DatabaseEditorAction(
        message("configuration.database.hosts.manage"),
        manageTooltip,
        AllIcons.General.Settings,
    ) {
        manageHosts()
        refresh()
    }
    val component: JComponent = boundedActionField(comboBox, saveAction, manageAction)
    private val listeners = mutableListOf<() -> Unit>()
    private var updating = false
    private var enabled = true

    init {
        comboBox.addActionListener { changed() }
        (comboBox.editor.editorComponent as JTextField).document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = changed()
            override fun removeUpdate(event: DocumentEvent) = changed()
            override fun changedUpdate(event: DocumentEvent) = changed()
        })
        comboBox.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(event: PopupMenuEvent) = refresh()
            override fun popupMenuWillBecomeInvisible(event: PopupMenuEvent) = Unit
            override fun popupMenuCanceled(event: PopupMenuEvent) = Unit
        })
        refresh()
    }

    fun host(): String = comboBox.editor.item?.toString().orEmpty().trim()

    fun setHost(host: String) {
        updating = true
        comboBox.selectedItem = host
        updating = false
        updateActions()
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        comboBox.isEnabled = enabled
        comboBox.isEditable = enabled
        manageAction.enabled = enabled
        updateActions()
    }

    fun addChangeListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun editorField(): JTextField = comboBox.editor.editorComponent as JTextField

    fun refresh() {
        val selectedHost = host()
        updating = true
        comboBox.model = DefaultComboBoxModel(DatabaseConnectionProfileSettings.getInstance().hosts().toTypedArray())
        comboBox.isEditable = comboBox.isEnabled
        comboBox.selectedItem = selectedHost
        updating = false
        updateActions()
    }

    private fun changed() {
        if (updating) return
        updateActions()
        listeners.forEach { it() }
    }

    private fun updateActions() {
        val currentHost = host()
        saveAction.enabled = enabled && currentHost.isNotEmpty() &&
            DatabaseConnectionProfileSettings.getInstance().hosts().none { it.equals(currentHost, ignoreCase = true) }
    }
}

internal class DatabaseProfileFields(
    saveHostTooltip: String,
    manageHostsTooltip: String,
    manageHosts: () -> Unit,
    openGlobalCredentials: () -> Unit,
) {
    val hostSelector = DatabaseHostSelector(saveHostTooltip, manageHostsTooltip, manageHosts)
    val portField = JBTextField("3306")
    val databaseField = JBTextField()
    val databaseEdiField = JBTextField()
    val overrideField = JBCheckBox(message("configuration.database.override")).apply {
        accessibleContext.accessibleName = message("configuration.database.override.accessible.name")
    }
    val usernameField = JBTextField()
    val passwordField = JBPasswordField()
    private val globalCredentialsLink = ActionLink(message("configuration.database.global.credentials")) {
        openGlobalCredentials()
    }.apply {
        toolTipText = message("configuration.database.global.credentials.tooltip")
    }

    fun createComponent(
        primaryLabel: String,
        primaryComponent: JComponent,
        title: String? = null,
        primaryActions: List<DatabaseEditorAction> = emptyList(),
        showDatabaseEdi: Boolean = true,
    ): JComponent {
        val builder = FormBuilder.createFormBuilder()
        title?.let {
            builder.addComponent(JBLabel(it).apply { font = font.deriveFont(font.style or java.awt.Font.BOLD) })
        }
        builder
            .addLabeledComponent(primaryLabel, boundedActionField(primaryComponent, *primaryActions.toTypedArray()))
            .addLabeledComponent(message("database.profiles.host"), hostSelector.component)
            .addLabeledComponent(message("database.profiles.port"), boundedActionField(portField))
            .addLabeledComponent(message("database.profiles.database"), boundedActionField(databaseField))
        if (showDatabaseEdi) {
            builder.addComponent(panel {
                collapsibleGroup(message("database.profiles.advanced")) {
                    row(message("database.profiles.database.edi")) {
                        cell(boundedActionField(databaseEdiField))
                    }
                }
            })
        }
        return builder
            .addComponent(boundedActionField(JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)).apply {
                isOpaque = false
                add(overrideField)
                add(globalCredentialsLink)
                add(JBLabel(message("configuration.database.for.project")))
            }))
            .addLabeledComponent(message("database.profiles.username"), boundedActionField(usernameField))
            .addLabeledComponent(message("database.profiles.password"), boundedActionField(passwordField))
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    fun installValidation(parentDisposable: Disposable, active: () -> Boolean = { true }) {
        ComponentValidator(parentDisposable)
            .withValidator {
                if (active() && hostSelector.host().isBlank()) {
                    ValidationInfo(message("database.profiles.validation.host"), hostSelector.comboBox)
                } else {
                    null
                }
            }
            .andRegisterOnDocumentListener(hostSelector.editorField())
            .installOn(hostSelector.comboBox)
        ComponentValidator(parentDisposable)
            .withValidator {
                val port = portField.text.toIntOrNull()
                if (active() && (port == null || port !in 1..65535)) {
                    ValidationInfo(message("database.profiles.validation.port"), portField)
                } else {
                    null
                }
            }
            .andRegisterOnDocumentListener(portField)
            .installOn(portField)
    }

    fun updateCredentialFields(enabled: Boolean = true) {
        usernameField.isEnabled = enabled && overrideField.isSelected
        passwordField.isEnabled = enabled && overrideField.isSelected
    }

    fun setEnabled(enabled: Boolean) {
        hostSelector.setEnabled(enabled)
        portField.isEnabled = enabled
        portField.isEditable = enabled
        databaseField.isEnabled = enabled
        databaseField.isEditable = enabled
        databaseEdiField.isEnabled = enabled
        databaseEdiField.isEditable = enabled
        overrideField.isEnabled = enabled
        updateCredentialFields(enabled)
    }
}

internal class DatabaseEditorAction(
    text: String,
    tooltip: String,
    icon: Icon,
    private val action: () -> Unit,
) : DumbAwareAction(text, tooltip, icon) {
    private val stateRefreshers = mutableListOf<() -> Unit>()
    var enabled: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            stateRefreshers.forEach { it() }
        }

    fun refreshStateWith(refresher: () -> Unit) {
        stateRefreshers.add(refresher)
    }

    override fun actionPerformed(event: AnActionEvent) = action()

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = enabled
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

internal fun boundedActionField(field: JComponent, vararg actions: DatabaseEditorAction): JComponent {
    val toolbar = actions.takeIf { it.isNotEmpty() }?.let {
        val actionToolbar = ActionManager.getInstance().createActionToolbar(
            ActionPlaces.TOOLWINDOW_TITLE,
            DefaultActionGroup(*it),
            true,
        ).apply {
            targetComponent = field
        }
        actions.forEach { action -> action.refreshStateWith { actionToolbar.updateActionsAsync() } }
        actionToolbar.component
    }
    return object : JPanel(BorderLayout()) {
        override fun getPreferredSize(): Dimension {
            val preferred = super.getPreferredSize()
            return Dimension(JBUI.scale(320), preferred.height)
        }

        override fun getMinimumSize(): Dimension {
            return Dimension(JBUI.scale(160), super.getMinimumSize().height)
        }
    }.apply {
        isOpaque = false
        field.minimumSize = Dimension(0, field.preferredSize.height)
        add(field, BorderLayout.CENTER)
        toolbar?.let { add(it, BorderLayout.EAST) }
    }
}
