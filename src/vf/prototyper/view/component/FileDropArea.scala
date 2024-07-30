package vf.prototyper.view.component

import utopia.firmament.context.TextContext
import utopia.firmament.localization.LocalString._
import utopia.firmament.localization.LocalizedString
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.mutable.eventful.{EventfulPointer, ResettableFlag}
import utopia.paradigm.color.ColorRole.Gray
import utopia.paradigm.color.ColorShade.Light
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.Mixed
import utopia.reach.component.factory.contextual.TextContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.ViewImageLabel
import utopia.reach.component.label.text.{TextLabel, ViewTextLabel}
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.dnd.DragAndDropEvent.{Drop, Enter, Exit}
import utopia.reach.dnd.{DragAndDropEvent, DragAndDropTarget}
import vf.prototyper.util.Common._
import vf.prototyper.view.Icon

import java.nio.file.Path

object FileDropArea extends Ccff[TextContext, ContextualFileDropAreaFactory]
{
	override def withContext(hierarchy: ComponentHierarchy, context: TextContext): ContextualFileDropAreaFactory =
		ContextualFileDropAreaFactory(hierarchy, context)
}

case class ContextualFileDropAreaFactory(hierarchy: ComponentHierarchy, override val context: TextContext)
	extends TextContextualFactory[ContextualFileDropAreaFactory]
{
	// IMPLEMENTED  -----------------------
	
	override def self: ContextualFileDropAreaFactory = this
	
	override def withContext(context: TextContext): ContextualFileDropAreaFactory = copy(context = context)
	
	
	// OTHER    ---------------------------
	
	/**
	 * Creates a new file drop area
	 * @param resultsPointer Pointer to the collected results (default = empty)
	 * @param parseFile A function that parses items from paths.
	 *                  Returns Left(localized error message) in case of a failure and Right(...) in case of a success
	 * @tparam A Type of parsed items
	 * @return A new file drop area component
	 */
	def apply[A](resultsPointer: EventfulPointer[Vector[A]] = EventfulPointer[Vector[A]](Vector()))
	            (parseFile: Path => Either[LocalizedString, A]) =
		new FileDropArea[A](hierarchy, context, resultsPointer, parseFile)
}

/**
 * Used for accepting file drops from the user
 * @author Mikko Hilpinen
 * @since 19.2.2023, v1.0
 */
class FileDropArea[A](hierarchy: ComponentHierarchy, context: TextContext,
                      resultsPointer: EventfulPointer[Vector[A]], parseFile: Path => Either[LocalizedString, A])
	extends ReachComponentWrapper with DragAndDropTarget
{
	// ATTRIBUTES   ------------------------
	
	private val isDropOverPointer = ResettableFlag()
	
	private val errorPointer = EventfulPointer(LocalizedString.empty)
	private val hasErrorPointer = errorPointer.map { !_.isEmpty }
	
	private val numberOfFilesPointer = resultsPointer.map { _.size }
	private val hasFilesPointer = numberOfFilesPointer.map { _ > 0 }
	
	// Contains small margins in a white / light gray background
	override protected val wrapped = Framing(hierarchy).withContext(context).small.withBackground(Gray, Light)
		.build(Stack) { stackF =>
			// Contains 3 rows of items:
			// 1: Header
			// 2: Number of files -indicator
			// 3: Hint
			stackF.centered.build(Mixed) { factories =>
				val header = factories(TextLabel).apply("Drop files here")
				// The file indicator shows a changing icon, and the actual read file count
				val fileCountIndicator = factories(Stack).related.row.build(Mixed) { factories =>
					val countLabel = factories(ViewTextLabel).apply(numberOfFilesPointer)
					val context = factories.context
					val imagePointer = hasFilesPointer
						.mergeWith(hasErrorPointer, isDropOverPointer) { (hasFiles, hasError, isDropOver) =>
							val icon = {
								if (isDropOver)
									Icon.scanDocument.large
								else if (hasFiles)
									Icon.fileSuccess.medium
								else
									Icon.scanDocument.medium
							}
							if (hasError)
								icon(context.color.failure)
							else if (hasFiles)
								icon(context.color.success)
							else
								icon.against(context.background)
						}
					val iconLabel = factories(ViewImageLabel).apply(imagePointer)
					Vector(countLabel, iconLabel)
				}
				val hintTextPointer = errorPointer
					.map[LocalizedString] { _.nonEmptyOrElse("You can also add\nfiles while editing") }
				val hintLabel = factories(ViewTextLabel)
					.mapContext { _.mapFont { _ * 0.8 }.withShrinkingText.manyLines }
					.text(hintTextPointer)
				
				Vector(header, fileCountIndicator.parent, hintLabel)
			}
		}
	
	
	// INITIAL CODE ------------------------
	
	// Registers to receive drag-and-drop -events
	hierarchy.top.dragAndDropManager.addTargetWhileAttached(this)
	
	
	// IMPLEMENTED  ------------------------
	
	override def dropArea: Bounds = boundsInsideTop
	
	override def onDragAndDropEvent(event: DragAndDropEvent): Unit = event match {
		case _: Enter =>
			isDropOverPointer.set()
			errorPointer.value = LocalizedString.empty
		case _: Exit => isDropOverPointer.reset()
		case d: Drop =>
			isDropOverPointer.reset()
			if (d.files.isEmpty)
				errorPointer.value = "No files were read"
			else {
				val (rejected, accepted) = d.files.divideWith(parseFile)
				if (rejected.hasSize > 1)
					errorPointer.value = "Failed to read ${count} files\n${msg}".autoLocalized
						.interpolated(Map("count" -> s"${ rejected.size }/${ d.files.size }", "msg" -> rejected.head))
				else if (rejected.nonEmpty)
					errorPointer.value = rejected.head
				resultsPointer.value ++= accepted
			}
		case e => if (e.isFinal) isDropOverPointer.reset()
	}
	
	override def onDropNearby(relativeDropPoint: Point, dropPointInCanvas: Point): Boolean = true
}
