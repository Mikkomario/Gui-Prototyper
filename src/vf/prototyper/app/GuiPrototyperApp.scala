package vf.prototyper.app

import utopia.paradigm.generic.ParadigmDataType
import utopia.reflection.container.stack.StackHierarchyManager
import vf.prototyper.model.immutable.Project
import vf.prototyper.util.Common._
import vf.prototyper.view.vc.{MainVc, StartProjectEditVc}

import scala.util.{Failure, Success}

/**
 * @author Mikko Hilpinen
 * @since 15.2.2023, v0.1
 */
object GuiPrototyperApp extends App
{
	ParadigmDataType.setup()
	
	val initialProjects = Project.loadAll() match {
		case Success((failures, projects)) =>
			failures.headOption.foreach { log(_, s"Failed to open ${failures.size} projects") }
			projects.sortBy { _.name }
		case Failure(error) =>
			log(error, "Project-loading failed")
			Vector()
	}
	
	StackHierarchyManager.startRevalidationLoop()
	
	// Case: No projects to start with => Goes directly to new project view
	if (initialProjects.isEmpty)
		new StartProjectEditVc().display().foreach {
			case Some(project) =>
				new MainVc(Vector(project)).display().foreach { _ =>
					println("Quitting")
					System.exit(0)
				}
			case None =>
				println("Quitting")
				System.exit(0)
		}
	// Case: Projects available => Allows the user to choose the next action
	else
		new MainVc(initialProjects).display().foreach { _ =>
			println("Quitting")
			System.exit(0)
		}
}
