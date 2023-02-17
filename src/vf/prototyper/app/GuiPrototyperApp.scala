package vf.prototyper.app

import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.genesis.handling.KeyStateListener
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.paradigm.generic.ParadigmDataType
import utopia.reflection.container.stack.StackHierarchyManager
import vf.prototyper.model.immutable.View
import vf.prototyper.view.EditVc
import vf.prototyper.util.Common._

import java.awt.event.KeyEvent
import java.nio.file.Paths

/**
 * @author Mikko Hilpinen
 * @since 15.2.2023, v0.1
 */
object GuiPrototyperApp extends App
{
	ParadigmDataType.setup()
	
	val idGenerator = Iterator.iterate(1) { _ + 1 }
	
	// Loads UI files for testing
	val baseViews = Paths.get("data/design/ui").children.get
		.map { p => View(idGenerator.next(), p.fileName.untilLast("."), p) }
	// Displays the edit view
	val frame = new EditVc(baseViews).display()
	
	StackHierarchyManager.startRevalidationLoop()
	
	GlobalKeyboardEventHandler += KeyStateListener.onKeyPressed(KeyEvent.VK_F5) { _ => frame.content.repaint() }
}
