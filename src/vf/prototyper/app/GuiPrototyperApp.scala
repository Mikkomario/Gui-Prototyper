package vf.prototyper.app

import utopia.flow.util.TryCatch
import utopia.paradigm.generic.ParadigmDataType
import vf.prototyper.model.immutable.Project
import vf.prototyper.util.Common._
import vf.prototyper.view.vc.{MainVc, StartProjectEditVc}

/**
 * @author Mikko Hilpinen
 * @since 15.2.2023, v0.1
 */
object GuiPrototyperApp extends App
{
	ParadigmDataType.setup()
	
	val initialProjects = Project.loadAll() match {
		case TryCatch.Success(projects, failures) =>
			failures.headOption.foreach { log(_, s"Failed to open ${failures.size} projects") }
			projects.sortBy { _.name }
		case TryCatch.Failure(error) =>
			log(error, "Project-loading failed")
			Vector()
	}
	
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
