package vf.prototyper.view.vc

import utopia.firmament.controller.data.ContainerContentDisplayer
import utopia.flow.operator.EqualsFunction
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.paradigm.enumeration.Alignment.TopLeft
import utopia.reach.component.button.image.ImageButton
import utopia.reach.component.factory.Mixed
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.wrapper.Open
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.{MutableStack, Stack}
import utopia.reach.container.wrapper.Framing
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.model.enumeration.StackLayout.Center
import utopia.firmament.model.stack.LengthExtensions._
import utopia.paradigm.color.ColorShade.Light
import utopia.reach.window.ReachWindow
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
	
	
	// COMPUTED --------------------------
	
	private def projects = projectsPointer.value
	private def projects_=(newProjects: Vector[Project]) = projectsPointer.value = newProjects
	
	
	// OTHER    --------------------------
	
	/**
	 * Displays the main view
	 * @return Future that resolves once all resulting windows have closed
	 */
	def display() = {
		// The view consists of a header and a projects -list
		val window = ReachWindow.withContext(context.window.withAnchorAlignment(TopLeft))
			.using(Stack, title = "GUI-Prototyper") { (canvas, stackF) =>
				stackF.build(Framing).withoutMargin() { framingF =>
					implicit val cnv: ReachCanvas = canvas
					// Header
					// [ Projects | + ]
					val headerBg = color.primary.dark
					val header = framingF.mapContext { _.against(headerBg) }.build(Stack)
						.apply(margins.small.any, Vector(BackgroundDrawer(headerBg))) { stackF =>
							stackF.build(Mixed).row(Center) { factories =>
								// The header contains a title label and a button for adding new projects
								val titleLabel = factories
									.mapContext { _.withTextExpandingToRight.larger }(TextLabel)
									.apply("Projects")
								val newButton = factories(ImageButton).withIcon(Icon.addCircle.large) { newProject() }
								Vector(titleLabel, newButton)
							}
						}
					// The projects list is mutable and separately managed
					val listBg = color.gray.against(headerBg, Light)
					val listFramingF = framingF.mapContext { _.against(listBg) }
					val (projectsFrame, projectsStack) = listFramingF.build(MutableStack)
						.apply(margins.small.any, Vector(BackgroundDrawer(listBg))) { stackF =>
							stackF.apply[ProjectRow](areRelated = true)
						}.toTuple
					// Sets up content management for the stack
					ContainerContentDisplayer.forImmutableStates(projectsStack, projectsPointer) {
						EqualsFunction.by { p: Project => p.id }(EqualsFunction.default)
					} { project =>
						Open.withContext(listFramingF.context.forTextComponents).apply(ProjectRow) {
							_.apply(project)(viewProject)(editProject)(deleteProject)
						}
					}
					
					Vector(header.parent, projectsFrame)
				}
			}
		
		window.display(centerOnParent = true)
		window.closeFuture.flatMap { _ =>
			openProjectIdsPointer.findMapFuture { p => if (p.isEmpty) Some(()) else None }
		}
	}
	
	private def newProject() = new StartProjectEditVc().display().foreach { _.foreach { project =>
		projectsPointer.update { p => (p :+ project).sortBy { _.name } }
	} }
	
	private def viewProject(project: Project) = {
		if (tryOpen(project))
			new PresentationVc(project).display().foreach { _ =>
				openProjectIdsPointer.update { _ - project.id }
			}
	}
	private def editProject(project: Project) = {
		// Only allows a single edit view per project at a time
		if (tryOpen(project))
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
	
	// Returns true if can open
	private def tryOpen(project: Project) = openProjectIdsPointer.pop { ids =>
		if (ids.contains(project.id))
			false -> ids
		else
			true -> (ids + project.id)
	}
}
