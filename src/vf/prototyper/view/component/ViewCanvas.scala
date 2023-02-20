package vf.prototyper.view.component

import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.event.{ConsumeEvent, MouseButton, MouseButtonStateEvent}
import utopia.genesis.handling.MouseButtonStateListener
import utopia.genesis.image.MutableImage
import utopia.genesis.util.Screen
import utopia.inception.handling.HandlerType
import utopia.paradigm.shape.shape2d.Vector2D
import utopia.reach.component.factory.ComponentFactoryFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.ViewImageLabel
import utopia.reach.component.template.ReachComponentWrapper
import vf.prototyper.model.immutable.View
import vf.prototyper.util.Common._

object ViewCanvas extends ComponentFactoryFactory[ViewCanvasFactory]
{
	override def apply(hierarchy: ComponentHierarchy): ViewCanvasFactory = new ViewCanvasFactory(hierarchy)
}

class ViewCanvasFactory(hierarchy: ComponentHierarchy)
{
	/**
	 * Creates a new view canvas
	 * @param currentViewPointer Pointer to the currently displayed view
	 * @param possibleViews All possible views
	 * @param changeView A function that accepts the id of the view to present next, and presents it
	 * @return A new view canvas
	 */
	def apply(currentViewPointer: Changing[View], possibleViews: IterableOnce[View])(changeView: Int => Unit) =
		new ViewCanvas(hierarchy, currentViewPointer, possibleViews, changeView)
}

/**
 * Controls interactions with a presented canvas / view
 * @author Mikko Hilpinen
 * @since 20.2.2023, v0.1
 */
class ViewCanvas(hierarchy: ComponentHierarchy, currentViewPointer: Changing[View], possibleViews: IterableOnce[View],
                 changeView: Int => Unit)
	extends ReachComponentWrapper
{
	// ATTRIBUTES   ----------------------------
	
	private val maxImageSize = Screen.size * Vector2D(1, 0.8)
	private val viewImageCache = Cache { view: View => view.viewImage(maxImageSize) }
	// Shows a non-modified image while loading image modifications
	private val imageLoadPointer = currentViewPointer
		.mapAsyncCatching(currentView.image.fittingWithin(maxImageSize)) { viewImageCache(_) }
	private val imagePointer = imageLoadPointer.map { res =>
		res.queuedOrigin.orElse(res.activeOrigin) match {
			case Some(origin) => origin.image.fittingWithin(maxImageSize)
			case None => res.current
		}
	}
	
	private val lazyDrawImage = ResettableLazy { MutableImage.canvas(imagePointer.value.size) }
	
	// TODO: Add custom draw support
	override val wrapped = ViewImageLabel(hierarchy).apply(imagePointer)
	
	
	// INITIAL CODE ----------------------------
	
	addMouseButtonListener(CanvasMouseListener)
	
	// Pre-loads all view images
	imageLoadPointer.futureWhere { _.isNotProcessing }.foreach { _ =>
		possibleViews.iterator.foreach { viewImageCache(_).waitFor() }
	}
	
	
	// COMPUTED --------------------------------
	
	private def currentView = currentViewPointer.value
	
	
	// NESTED   --------------------------------
	
	private object CanvasMouseListener extends MouseButtonStateListener
	{
		// IMPLEMENTED  ------------------------
		
		override def onMouseButtonState(event: MouseButtonStateEvent): Option[ConsumeEvent] = {
			// Follows links upon clicks
			if (event.isUp && event.button.contains(MouseButton.Left)) {
				lazy val coordinates = (event.mousePosition - position) / size
				currentView.links.find { _.area.contains(coordinates) }.map { l =>
					changeView(l.targetId)
					ConsumeEvent("Clicked link")
				}
			}
			else
				None
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType): Boolean = true
	}
}
