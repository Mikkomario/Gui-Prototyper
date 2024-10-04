package vf.prototyper.view.vc

import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.parse.file.FileConflictResolution.Rename
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._
import utopia.flow.util.TryExtensions._
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.paradigm.color.ColorRole.{Primary, Secondary}
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.reach.component.button.image.ImageAndTextButton
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.text.TextField
import utopia.reach.component.wrapper.WindowCreationResult
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.window.ReachWindow
import vf.prototyper.model.immutable.{Project, View}
import vf.prototyper.util.Common._
import vf.prototyper.view.Icon
import vf.prototyper.view.component.FileDropArea
import vf.prototyper.view.vc.StartProjectEditVc.acceptedFileTypes

import java.nio.file.Path
import java.util.UUID
import scala.concurrent.Future

object StartProjectEditVc
{
	private val acceptedFileTypes = Set("png", "jpg", "jpeg")
}

/**
 * Controls a view used for starting a new project
 * @author Mikko Hilpinen
 * @since 19.2.2023, v1.0
 */
class StartProjectEditVc(project: Option[Project] = None)
{
	// ATTRIBUTES   ----------------------------
	
	private val initialPaths = project match {
		case Some(project) => project.views.map { _.path }.toSet
		case None => Set[Path]()
	}
	
	private val projectNamePointer = EventfulPointer(project match {
		case Some(project) => project.name
		case None => "New Project"
	})
	private val viewFilesPointer = EventfulPointer(initialPaths.toVector)
	private val windowPointer: ResettableLazy[WindowCreationResult[Framing, Unit]] = ResettableLazy {
		// Contains a framed stack with 3 rows:
		// 1: Project name field
		// 2: File input area
		// 3: Start & Cancel -buttons
		ReachWindow.withContext(Context.window.withBackground(Primary))
			.using(Framing, title = "Start project") { (_, framingF) =>
				framingF.build(Stack) { stackF =>
					stackF.build(Mixed) { factories =>
						val dropArea = factories(FileDropArea)
							.apply(viewFilesPointer) { p =>
								if (acceptedFileTypes.contains(p.fileType))
									Right(p)
								else
									Left("Only image files are supported")
							}
						val projectNameField = factories(TextField)
							.withFieldName("Project name")
							.string(length.field.medium.upscaling, textPointer = projectNamePointer)
						val buttons = factories(Stack)
							.mapContext { _.mapTextInsets { i => (i * 2).expandingHorizontally } }
							.row
							.build(Mixed) { factories =>
								val imageInsets = Insets.symmetric(margins.small).any
								val startButton = factories(ImageAndTextButton).mapContext { _ / Secondary }
									.triggeredWithEnter.withImageInsets(imageInsets)
									.apply(Icon.start.medium, "Start") {
										// Moves to the edit view (at least one view is required, however)
										if (viewFilesPointer.value.nonEmpty) {
											start()
										}
									}
								val cancelButton = factories.mapContext { _ / Primary }(ImageAndTextButton)
									.triggeredWithEscape.withImageInsets(imageInsets)
									.apply(Icon.cancel.medium, "Cancel") { windowPointer.current.foreach { _.close() } }
								Vector(startButton, cancelButton)
							}
						
						Vector(dropArea, projectNameField, buttons.parent)
					}
				}
		}
	}
	
	private var startFuture: Option[Future[Project]] = None
	
	
	// OTHER    -----------------------------
	
	/**
	 * Displays this window
	 * @return Future that resolves into the created project, or None if project creation was cancelled
	 */
	def display() = {
		windowPointer.filterNot { _.hasClosed }
		val window = windowPointer.value
		window.display(centerOnParent = true)
		window.closeFuture.flatMap { _ =>
			startFuture match {
				case Some(future) => future.map { Some(_) }
				case None => Future.successful(None)
			}
		}
	}
	
	private def start() = {
		// Copies the view files into a separate directory
		val projectName = projectNamePointer.value.untilFirst(".").nonEmptyOrElse("project")
		lazy val viewsDirectory = (directory.views/projectName).asExistingDirectory.getOrMap { error =>
			log(error, "Failed to create a sub-directory for project views")
			directory.views
		}
		val newViewFiles = viewFilesPointer.value.filterNot(initialPaths.contains).map { p =>
			if (p.isChildOf(directory.views))
				p
			else
				p.copyTo(viewsDirectory, Rename).getOrMap { error => log(error, "Failed to copy a view file"); p }
		}
		val existingViews = project match {
			case Some(project) => project.views
			case None => Vector()
		}
		val firstNewId = existingViews.map { _.id }.maxOption.getOrElse(1)
		val newViews = newViewFiles.zipWithIndex.map { case (path, index) =>
			View(firstNewId + index, path.fileName.untilLast("."), path)
		}
		// Displays the edit view
		startFuture = Some(new EditVc(existingViews ++ newViews).display()
			.map { views =>
				val newVersion = project match {
					case Some(project) => project.copy(name = projectName, views = views)
					case None =>
						// Constructs and saves the project
						val projectPath = (directory.projects/
							s"${projectName.replaceEachMatchOf(Regex.whiteSpace, "-")}.json").unique
						Project(UUID.randomUUID().toString, projectName, projectPath, views)
				}
				newVersion.save().failure.foreach { log(_, "Failed to save the project") }
				newVersion
			})
		windowPointer.popCurrent().foreach { _.close() }
	}
}
