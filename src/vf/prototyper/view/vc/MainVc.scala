package vf.prototyper.view.vc

import utopia.flow.operator.EqualsFunction
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.paradigm.enumeration.Alignment.TopLeft
import utopia.reach.component.button.image.ImageButton
import utopia.reach.component.factory.Mixed
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.wrapper.Open
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.stack.{MutableStack, Stack}
import utopia.reach.container.wrapper.Framing
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.controller.data.ContainerContentDisplayer2
import utopia.reflection.shape.LengthExtensions._
import vf.prototyper.model.immutable.Project
import vf.prototyper.util.Common._
import vf.prototyper.view.Icon
import vf.prototyper.view.component.ProjectRow

/**
 * The main view-controller. Used for selecting the target project and for starting new projects.
 * @author Mikko Hilpinen
 * @since 15.2.2023, v0.1
 */
class MainVc(initialProjects: Vector[Project])
{
	// ATTRIBUTES   -------------------------
	
	private val openProjectIdsPointer = Volatile(Set[String]())
	
	private val projectsPointer = new PointerWithEvents(initialProjects)
	
	// The view consists of a header and a projects -list
	private val view = ReachCanvas(cursors) { hierarchy =>
		Stack(hierarchy).withContext(context.base).build(Framing).withoutMargin() { framingF =>
			implicit def canvas: ReachCanvas = hierarchy.top
			// Header
			// [ Projects | + ]
			val headerBg = color.primary.dark
			val header = framingF.mapContext { _.inContextWithBackground(headerBg) }.build(Stack)
				.apply(margins.small.any, Vector(BackgroundDrawer(headerBg))) { stackF =>
					stackF.build(Mixed).row(Center) { factories =>
						// The header contains a title label and a button for adding new projects
						val titleLabel = factories
							.mapContext { _.forTextComponents.mapFont { _ * 1.2 }.expandingToRight }(TextLabel)
							.apply("Projects")
						val newButton = factories(ImageButton).withIcon(Icon.addCircle.large) { newProject() }
						Vector(titleLabel, newButton)
					}
				}
			// The projects list is mutable and separately managed
			val listBg = color.gray.forBackgroundPreferringLight(headerBg)
			val listFramingF = framingF.mapContext { _.inContextWithBackground(listBg) }
			val (projectsFrame, projectsStack) = listFramingF.build(MutableStack)
				.apply(margins.small.any, Vector(BackgroundDrawer(listBg))) { stackF =>
					stackF.apply[ProjectRow](areRelated = true)
				}.toTuple
			// Sets up content management for the stack
			ContainerContentDisplayer2.forImmutableStates(projectsStack, projectsPointer) {
				EqualsFunction.by { p: Project => p.id }(EqualsFunction.default)
			} { project =>
				Open.withContext(ProjectRow, listFramingF.context.forTextComponents) {
					_.apply(project)(viewProject)(editProject)(deleteProject)
				}
			}
			
			Vector(header.parent, projectsFrame)
		}
	}.parent
	
	// TODO: Remove test prints
	openProjectIdsPointer.addContinuousListener { e => println(s"${ e.newValue.size } projects open") }
	
	
	// COMPUTED --------------------------
	
	private def projects = projectsPointer.value
	private def projects_=(newProjects: Vector[Project]) = projectsPointer.value = newProjects
	
	
	// OTHER    --------------------------
	
	/**
	 * Displays the main view
	 * @return Future that resolves once all resulting windows have closed
	 */
	def display() = {
		val frame = Frame.windowed(view, "GUI-Prototyper", Program, margins.medium, getAnchor = TopLeft.origin)
		frame.startEventGenerators(actorHandler)
		frame.visible = true
		frame.closeFuture.flatMap { _ =>
			openProjectIdsPointer.findMapFuture { p => if (p.nonEmpty) Some(()) else None }
		}
	}
	
	private def newProject() = new StartProjectEditVc().display().foreach { _.foreach { project =>
		projectsPointer.update { p => (p :+ project).sortBy { _.name } }
	} }
	
	// TODO: Implement
	private def viewProject(project: Project) = println("View")
	private def editProject(project: Project) = {
		// Only allows a single edit view per project at a time
		val shouldOpen = openProjectIdsPointer.pop { ids =>
			if (ids.contains(project.id))
				false -> ids
			else
				true -> (ids + project.id)
		}
		if (shouldOpen)
			new StartProjectEditVc(Some(project)).display().foreach { result =>
				result.foreach { newVersion =>
					projectsPointer
						.update { p => (p.filterNot { _.id == newVersion.id } :+ newVersion).sortBy { _.name } }
				}
				openProjectIdsPointer.update { _ - project.id }
			}
	}
	private def deleteProject(project: Project) = {
		if (!openProjectIdsPointer.value.contains(project.id))
			projectsPointer.update { _.filterNot { _.id == project.id } }
	}
}
