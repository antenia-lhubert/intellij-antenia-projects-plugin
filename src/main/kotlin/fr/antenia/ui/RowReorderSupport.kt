package fr.antenia.ui

import java.awt.Cursor
import java.awt.GraphicsEnvironment
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DropMode
import javax.swing.JList
import javax.swing.JTable
import javax.swing.TransferHandler

internal object RowReorderSupport {
    private val rowFlavor = DataFlavor(Int::class.javaObjectType, "Antenia row")

    fun install(table: JTable, move: (source: Int, insertion: Int) -> Boolean) {
        if (GraphicsEnvironment.isHeadless()) return
        table.dragEnabled = true
        table.dropMode = DropMode.INSERT_ROWS
        table.transferHandler = object : TransferHandler() {
            override fun getSourceActions(component: javax.swing.JComponent): Int = MOVE

            override fun createTransferable(component: javax.swing.JComponent): Transferable? {
                if (table.isEditing && !table.cellEditor.stopCellEditing()) return null
                return table.selectedRow.takeIf { it >= 0 }?.let(::RowTransferable)
            }

            override fun canImport(support: TransferSupport): Boolean =
                support.component === table && support.isDataFlavorSupported(rowFlavor) && support.dropLocation is JTable.DropLocation

            override fun importData(support: TransferSupport): Boolean {
                if (!canImport(support)) return false
                val source = support.transferable.getTransferData(rowFlavor) as Int
                val insertion = (support.dropLocation as JTable.DropLocation).row
                return move(source, insertion)
            }
        }
        installHandCursor(table) { table.rowAtPoint(it) >= 0 }
    }

    fun install(list: JList<*>, move: (source: Int, insertion: Int) -> Boolean) {
        if (GraphicsEnvironment.isHeadless()) return
        list.dragEnabled = true
        list.dropMode = DropMode.INSERT
        list.transferHandler = object : TransferHandler() {
            override fun getSourceActions(component: javax.swing.JComponent): Int = MOVE

            override fun createTransferable(component: javax.swing.JComponent): Transferable? =
                list.selectedIndex.takeIf { it >= 0 }?.let(::RowTransferable)

            override fun canImport(support: TransferSupport): Boolean =
                support.component === list && support.isDataFlavorSupported(rowFlavor) && support.dropLocation is JList.DropLocation

            override fun importData(support: TransferSupport): Boolean {
                if (!canImport(support)) return false
                val source = support.transferable.getTransferData(rowFlavor) as Int
                val insertion = (support.dropLocation as JList.DropLocation).index
                return move(source, insertion)
            }
        }
        installHandCursor(list) { list.locationToIndex(it).takeIf { row -> row >= 0 }?.let { row -> list.getCellBounds(row, row).contains(it) } == true }
    }

    private fun installHandCursor(component: javax.swing.JComponent, overItem: (java.awt.Point) -> Boolean) {
        component.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(event: MouseEvent) {
                component.cursor = Cursor.getPredefinedCursor(if (overItem(event.point)) Cursor.HAND_CURSOR else Cursor.DEFAULT_CURSOR)
            }
        })
        component.addMouseListener(object : MouseAdapter() {
            override fun mouseExited(event: MouseEvent) {
                component.cursor = Cursor.getDefaultCursor()
            }
        })
    }

    private class RowTransferable(private val row: Int) : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(rowFlavor)
        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == rowFlavor
        override fun getTransferData(flavor: DataFlavor): Any {
            require(isDataFlavorSupported(flavor))
            return row
        }
    }
}

internal object RowMove {
    fun <T> move(items: MutableList<T>, source: Int, insertion: Int, allowed: (source: Int, destination: Int) -> Boolean = { _, _ -> true }): Int {
        if (source !in items.indices || insertion !in 0..items.size) return -1
        val destination = (if (insertion > source) insertion - 1 else insertion).coerceIn(0, items.lastIndex)
        if (destination == source || !allowed(source, destination)) return -1
        val item = items.removeAt(source)
        items.add(destination, item)
        return destination
    }
}
