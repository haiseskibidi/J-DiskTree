package com.jdisktree.ui

import javax.swing.JFileChooser
import javax.swing.UIManager
import java.io.File

/**
 * Utility to open a native directory picker dialog.
 */
object DirectoryPicker {
    
    init {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            // Fallback to default look and feel
        }
    }

    /**
     * Opens a directory chooser and returns the selected path.
     * @return Absolute path of the selected directory, or null if cancelled.
     */
    fun pickDirectory(): String? {
        val chooser = JFileChooser()
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        chooser.dialogTitle = "Select Directory to Scan"
        
        val result = chooser.showOpenDialog(null)
        return if (result == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.absolutePath
        } else {
            null
        }
    }
}
