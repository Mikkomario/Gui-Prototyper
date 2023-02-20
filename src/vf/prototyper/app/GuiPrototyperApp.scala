package vf.prototyper.app

import utopia.paradigm.generic.ParadigmDataType
import utopia.reflection.container.stack.StackHierarchyManager
import vf.prototyper.util.Common._
import vf.prototyper.view.vc.StartProjectEditVc

/**
 * @author Mikko Hilpinen
 * @since 15.2.2023, v0.1
 */
object GuiPrototyperApp extends App
{
	ParadigmDataType.setup()
	
	StackHierarchyManager.startRevalidationLoop()
	new StartProjectEditVc().display().foreach { _ => System.exit(0) }
}
