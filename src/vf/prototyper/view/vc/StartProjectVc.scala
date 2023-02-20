package vf.prototyper.view.vc

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.FileConflictResolution.Rename
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.paradigm.enumeration.Alignment.Top
import utopia.paradigm.shape.shape2d.Insets
import utopia.reach.component.button.image.ImageAndTextButton
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.text.TextField
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.stack.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.container.swing.window.{Frame, Window}
import utopia.reflection.event.HotKey
import utopia.reflection.shape.LengthExtensions._
import vf.prototyper.model.immutable.{Project, View}
import vf.prototyper.util.Common._
import vf.prototyper.view.Icon
import vf.prototyper.view.component.FileDropArea
import vf.prototyper.view.vc.StartProjectVc.acceptedFileTypes

import java.awt.event.KeyEvent
import java.nio.file.Path
import scala.concurrent.Future

object StartProjectVc
{
	private val acceptedFileTypes = Set("png", "jpg", "jpeg")
}

/**
 * Controls a view used for starting a new project
 * @author Mikko Hilpinen
 * @since 19.2.2023, v0.1
 */
class StartProjectVc
{
	// ATTRIBUTES   ----------------------------
	
	private val projectNamePointer = new PointerWithEvents("New Project")
	private val viewFilesPointer = new PointerWithEvents(Vector[Path]())
	private val windowPointer = Pointer.empty[Window[_]]()
	
	private var startFuture: Option[Future[Project]] = None
	
	private val view = ReachCanvas(cursors) { hierarchy =>
		val bg = color.primary.default
		// Contains a framed stack with 3 rows:
		// 1: Project name field
		// 2: File input area
		// 3: Start & Cancel -buttons
		Framing(hierarchy).build(Stack).apply(margins.medium.any, Vector(BackgroundDrawer(bg))) { stackF =>
			stackF.withContext(context.base.inContextWithBackground(bg)).build(Mixed).apply() { factories =>
				val projectNameField = factories.mapContext { _.forTextComponents }(TextField)
					.forString(length.field.medium.upscaling, Fixed("Project Name"), textPointer = projectNamePointer)
				val dropArea = factories(FileDropArea)
					.apply(viewFilesPointer) { p =>
						if (acceptedFileTypes.contains(p.fileType))
							Right(p)
						else
							Left("Only image files are supported")
					}
				val buttons = factories(Stack)
					.mapContext { _.forTextComponents.mapInsets { i => (i * 2).expandingHorizontally } }
					.build(Mixed)
					.row() { factories =>
						val imageInsets = Insets.symmetric(margins.small).any
						val startButton = factories.mapContext { _.forSecondaryColorButtons }(ImageAndTextButton)
							.withIcon(Icon.start.medium, "Start", imageInsets,
								hotKeys = Set(HotKey.keyWithIndex(KeyEvent.VK_ENTER))) {
								// Moves to the edit view (at least one view is required, however)
								if (viewFilesPointer.value.nonEmpty) {
									start()
								}
							}
						val cancelButton = factories.mapContext { _.forPrimaryColorButtons }(ImageAndTextButton)
							.withIcon(Icon.cancel.medium, "Cancel", imageInsets,
								hotKeys = Set(HotKey.keyWithIndex(KeyEvent.VK_ESCAPE))) {
								windowPointer.value.foreach { _.close() } }
						Vector(startButton, cancelButton)
					}
				
				Vector(projectNameField, dropArea, buttons.parent)
			}
		}
	}
	
	
	// OTHER    -----------------------------
	
	/**
	 * Displays this window
	 * @return Future that resolves into the created project, or None if project creation was cancelled
	 */
	def display() =
		windowPointer.filterNotCurrent { _.isClosed }
			.getOrElseUpdate {
				val window = Frame.windowed(view.parent, "Start project", Program, margins.medium,
					getAnchor = view.parent.anchorPosition(_, Top))
				window.startEventGenerators(actorHandler)
				window.visible = true
				window
			}
			.closeFuture.flatMap { _ =>
				startFuture match {
					case Some(future) => future.map { Some(_) }
					case None => Future.successful(None)
				}
			}
	
	private def start() = {
		// Copies the view files into a separate directory
		val projectName = projectNamePointer.value.untilFirst(".").nonEmptyOrElse("project")
		(directory.views/projectName).unique.asExistingDirectory.map { projectDirectory =>
			val viewFiles = viewFilesPointer.value.map { p =>
				p.copyTo(projectDirectory, Rename).getOrMap { error => log(error, "Failed to copy a view file"); p }
			}
			// Displays the edit view
			startFuture = Some(new EditVc(viewFiles.zipWithIndex
				.map { case (path, index) => View(index, path.fileName.untilLast("."), path) }).display()
				.map { views =>
					// Constructs and saves the project
					val project = Project(projectName, views)
					(directory.projects/s"${projectName.replaceEachMatchOf(Regex.whiteSpace, "-")}.json")
						.unique.writeJson(project)
						.failure.foreach { log(_, "Failed to save the project") }
					project
				})
			windowPointer.pop().foreach { _.close() }
		}
	}
}
