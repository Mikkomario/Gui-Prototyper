package vf.prototyper.view.vc

import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.paradigm.enumeration.Alignment.Top
import utopia.reach.component.button.image.ImageButton
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.selection.DropDown
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.stack.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.localization.LocalString._
import vf.prototyper.model.immutable.{Project, View}
import vf.prototyper.util.Common._
import vf.prototyper.view.Icon
import vf.prototyper.view.component.ViewCanvas

/**
 * Control the presentation view
 * @author Mikko Hilpinen
 * @since 20.2.2023, v0.1
 */
class PresentationVc(project: Project)
{
	// ATTRIBUTES   ------------------------------
	
	private val maxHistorySize = 30
	
	// Contains the direct parent view of each view, where applicable
	private val parentViewCache = Cache { view: View =>
		views.filter { v => v.id != view.id && v.isParentOf(view) }
			.bestMatch { _.region.isDefined }
			.minByOption { _.region match {
				case Some(region) => region.area
				case None => 0
			} }
	}
	
	private val currentViewPointer = new PointerWithEvents(views.head)
	private val selectedViewPointer = new PointerWithEvents[Option[View]](Some(currentView))
	
	private val viewHistoryPointer = Pointer(Vector[View]())
	private val undoHistoryPointer = Pointer(Vector[View]())
	
	// The view consists of a header + canvas
	private val view = ReachCanvas(cursors) { hierarchy =>
		Stack(hierarchy).withContext(context.base).build(Mixed).withoutMargin() { factories =>
			// The header contains navigation arrow buttons and a select view -drop-down
			// [ <- | View | ^ | -> ]
			val headerBg = color.primary.dark
			val header = factories(Framing)
				.mapContext { _.inContextWithBackground(headerBg).forTextComponents }.build(Stack)
				.apply(margins.small.any, Vector(BackgroundDrawer(headerBg))) { stackF =>
					stackF.build(Mixed).row(Center) { factories =>
						// Buttons for navigation
						// TODO: Add enabled states
						val buttons = factories(ImageButton)
						val backButton = buttons.withIcon(Icon.arrowLeft.large) { goBack() }
						val forwardButton = buttons.withIcon(Icon.arrowRight.large) { goForward() }
						val upButton = buttons.withIcon(Icon.arrowUp.large) { goToParent() }
						// Drop-down for view-selection
						val dropDown = factories(DropDown)
							.simple(Fixed(views), selectedViewPointer,
								Some(Icon.arrowDown.small), Some(Icon.arrowUp.small),
								DisplayFunction.noLocalization[View] { _.name })
						
						Vector(backButton, dropDown, upButton, forwardButton)
					}
				}
			// The canvas view is an image with some interactivity
			// TODO: Add draw capabilities
			val canvas = factories.withoutContext(ViewCanvas).apply(currentViewPointer, views) { viewId =>
				views.find { _.id == viewId }.foreach(goToView)
			}
			Vector(header.parent, canvas)
		}
	}.parent
	
	
	// INITIAL CODE --------------------------
	
	currentViewPointer.addContinuousListener { e => selectedViewPointer.value = Some(e.newValue) }
	selectedViewPointer.addContinuousListener { e => e.newValue.foreach(goToView) }
	
	
	// COMPUTED ------------------------------
	
	private def views = project.views
	
	private def currentView = currentViewPointer.value
	private def currentView_=(newView: View) = currentViewPointer.value = newView
	
	
	// OTHER    ------------------------------
	
	/**
	 * Displays this view
	 * @return A future that resolves when this view is closed
	 */
	def display() = {
		val frame = Frame.windowed(view, project.name.localizationSkipped, Program, margins.medium,
			getAnchor = Top.origin)
		frame.startEventGenerators(actorHandler)
		frame.visible = true
		frame.closeFuture
	}
	
	private def goBack() = viewHistoryPointer.popLast().foreach { view =>
		undoHistoryPointer.update { _ :+ currentView }
		println(s"Going back from ${ currentView.name } to ${ view.name }")
		currentView = view
	}
	private def goForward() = undoHistoryPointer.popLast().foreach { view =>
		viewHistoryPointer.update { _ :+ currentView }
		println(s"Moving from ${ currentView.name } to ${ view.name }")
		currentView = view
	}
	private def goToParent() = parentViewCache(currentView).foreach(goToView)
	
	private def goToView(view: View) = {
		currentViewPointer.update { old =>
			if (old.id == view.id)
				old
			else {
				viewHistoryPointer.update { history => (history :+ old).take(maxHistorySize) }
				undoHistoryPointer.clear()
				view
			}
		}
	}
}
