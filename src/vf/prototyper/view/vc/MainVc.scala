package vf.prototyper.view.vc

import utopia.flow.operator.EqualsFunction
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.reach.component.button.image.ImageButton
import utopia.reach.component.factory.Mixed
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.wrapper.Open
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.stack.{MutableStack, Stack, ViewStack}
import utopia.reach.container.wrapper.Framing
import utopia.reflection.component.context.TextContext
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.controller.data.ContainerContentDisplayer2
import utopia.reflection.shape.stack.StackLength
import utopia.reflection.shape.LengthExtensions._
import vf.prototyper.model.immutable.Project
import vf.prototyper.util.Common._
import vf.prototyper.view.Icon
import vf.prototyper.view.component.ProjectRow

import scala.util.{Failure, Success}

/**
 * The main view-controller. Used for selecting the target project and for starting new projects.
 * @author Mikko Hilpinen
 * @since 15.2.2023, v0.1
 */
class MainVc
{
	// ATTRIBUTES   -------------------------
	
	private val projectsPointer = new PointerWithEvents(Project.loadAll() match {
		case Success((failures, projects)) =>
			failures.headOption.foreach { log(_, s"Failed to open ${ failures.size } projects") }
			projects.sortBy { _.name }
		case Failure(error) =>
			log(error, "Project-loading failed")
			Vector()
	})
	
	// The view consists of a header and a projects -list
	private val (view, projectsStack) = ReachCanvas(cursors) { hierarchy =>
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
			
			Vector(header.parent, projectsFrame) -> projectsStack
		}
	}.parentAndResult
	
	
	// OTHER    --------------------------
	
	private def newProject() = ???
	
	private def viewProject(project: Project) = ???
	private def editProject(project: Project) = ???
	private def deleteProject(project: Project) = ???
}
