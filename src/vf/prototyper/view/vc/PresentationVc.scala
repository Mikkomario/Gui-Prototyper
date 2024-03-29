package vf.prototyper.view.vc

import utopia.firmament.localization.DisplayFunction
import utopia.firmament.localization.LocalString._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.reach.component.button.image.ImageButton
import utopia.reach.component.factory.Mixed
import utopia.reach.container.multi.Stack
import utopia.reach.container.wrapper.Framing
import utopia.reach.window.ReachWindow
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
	
	private val currentViewPointer = EventfulPointer(views.head)
	private val selectedViewPointer = EventfulPointer[Option[View]](Some(currentView))
	
	private val viewHistoryPointer = Pointer(Vector[View]())
	private val undoHistoryPointer = Pointer(Vector[View]())
	
	
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
		// The view consists of a header + canvas
		val window = ReachWindow.withContext(context.window.withWindowBackground(color.primary.dark))
			.using(Stack, title = project.name.localizationSkipped) { (_, stackF) =>
				stackF.withoutMargin.build(Mixed) { factories =>
					// The header contains navigation arrow buttons and a select view -drop-down
					// [ <- | View | ^ | -> ]
					val header = factories(Framing).small.build(Stack) { stackF =>
						stackF.centeredRow.build(Mixed) { factories =>
							// Buttons for navigation
							// TODO: Add enabled states
							val buttons = factories(ImageButton)
							val backButton = buttons.icon(Icon.arrowLeft.large) { goBack() }
							val forwardButton = buttons.icon(Icon.arrowRight.large) { goForward() }
							val upButton = buttons.icon(Icon.arrowUp.large) { goToParent() }
							// Drop-down for view-selection
							val dropDown = factories.mapContext(context.window.withTextContext)(component.dropDown)
								.simple(Fixed(views), selectedViewPointer,
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
			}
		
		window.display(centerOnParent = true)
		window.closeFuture
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
