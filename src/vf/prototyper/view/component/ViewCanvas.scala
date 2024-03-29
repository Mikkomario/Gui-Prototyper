package vf.prototyper.view.component

import utopia.firmament.drawing.template.CustomDrawer
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.operator.filter.Filter
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.graphics.DrawLevel.Foreground
import utopia.genesis.graphics.Priority.High
import utopia.genesis.graphics.{DrawLevel, DrawSettings, Drawer, StrokeSettings}
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}
import utopia.genesis.handling.event.mouse._
import utopia.genesis.image.Image
import utopia.genesis.util.Screen
import utopia.paradigm.measurement.DistanceExtensions._
import utopia.paradigm.path.BezierPath
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
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
		.mapToFuture(currentView.image.fittingWithin(maxImageSize),
			mapCondition = hierarchy.linkPointer) { viewImageCache(_) }
	private val imagePointer = imageLoadPointer.map { res =>
		res.queuedOrigin.orElse(res.activeOrigin) match {
			case Some(origin) => origin.image.fittingWithin(maxImageSize)
			case None => res.current
		}
	}
	
	override val wrapped = ViewImageLabel(hierarchy).withCustomDrawer(BezierHandler)(imagePointer)
	
	
	// INITIAL CODE ----------------------------
	
	addMouseButtonListener(CanvasMouseListener)
	
	currentViewPointer.addContinuousAnyChangeListener { BezierHandler.reset() }
	/*
	parentHierarchy.linkPointer.addListenerAndSimulateEvent(false) { isLinked =>
		if (isLinked.newValue)
			GlobalMouseEventHandler += BezierHandler
		else
			GlobalMouseEventHandler -= BezierHandler
	}*/
	
	private val tracker = new DragTracker(BezierHandler)
	addMouseMoveListener(tracker)
	addMouseButtonListener(tracker)
	
	// Pre-loads all view images
	/*
	imageLoadPointer.futureWhere { _.isNotProcessing }.foreach { _ =>
		possibleViews.iterator.foreach { viewImageCache(_).waitFor() }
	}*/
	
	
	// COMPUTED --------------------------------
	
	private def currentView = currentViewPointer.value
	
	
	// NESTED   --------------------------------
	
	private object CanvasMouseListener extends MouseButtonStateListener
	{
		// ATTRIBUTES   ------------------------
		
		// Follows links upon clicks
		override lazy val mouseButtonStateEventFilter: Filter[MouseButtonStateEvent] =
			MouseButtonStateEvent.filter.leftReleased
		
		
		// IMPLEMENTED  ------------------------
		
		override def handleCondition: FlagLike = AlwaysTrue
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
			lazy val coordinates = (event.position - position) / size
			currentView.links.find { _.area.contains(coordinates) } match {
				case Some(link) =>
					changeView(link.targetId)
					Consume("Clicked link")
				case None => Preserve
			}
		}
	}
	
	private object BezierHandler extends MouseDragListener with CustomDrawer
	{
		// ATTRIBUTES   ------------------------
		
		implicit val ds: DrawSettings = StrokeSettings.rounded(color.primary, 5)
		
		private val minDistance = 1.0.cm.toPixels
		private val step = 0.2
		
		override val mouseDragEventFilter = MouseDragEvent.filter.left && MouseEvent.filter.over(boundsInsideTop)
		
		private var currentDistance = 0.0
		
		private val pointsPointer = EventfulPointer(Vector[Point]())
		
		private val imagePointer = pointsPointer.mapAsync(Image.empty, skipInitialMap = true) { points =>
			if (points.size < 3)
				Image.empty
			else {
				// println(s"Creating a path from ${points.size} points")
				val path = BezierPath(points, 0)
				val actualStep = step / points.size
				val pathPoints = Iterator.iterate(0.0) { _ + actualStep }.takeWhile { _ <= 1.0 }.map(path.apply).toVector
				val pathBounds = Bounds.between(Point.topLeft(pathPoints), Point.bottomRight(pathPoints))
				// println(s"Path bounds: $pathBounds")
				val res = Image.paint(pathBounds.size) { drawer =>
					pathPoints.map { _ - pathBounds.position }.paired.foreach { p => drawer.draw(Line(p)) }
				}.withOrigin(-pathBounds.position)
				// println("Image created")
				res
			}
		}.map { _.current }
		
		
		// INITIAL CODE ------------------------
		
		imagePointer.addContinuousListener { e =>
			val area = Bounds.around(e.values.filterNot { _.isEmpty }.map { _.bounds }).enlarged(5)
			// println(s"Repainting $area")
			repaintArea(area, High)
			// repaint()
		}
		
		
		// IMPLEMENTED  ------------------------
		
		override def opaque: Boolean = false
		override def drawLevel: DrawLevel = Foreground
		
		override def handleCondition: FlagLike = AlwaysTrue
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = imagePointer.value.drawWith(drawer, bounds.position)
		
		override def onMouseDrag(event: MouseDragEvent): Unit = {
			val movement = event.lastMove.transition.length
			if (event.isDragStart) {
				// println("Drag starts")
				currentDistance = movement
				val p = event.dragOrigin - positionInTop
				// println(p)
				// println(s"First point is ${p.relative} (${ p.absolute } / ${ p.anchor })")
				pointsPointer.value = Vector(p)
			}
			else {
				currentDistance += movement
				if (currentDistance >= minDistance || event.isDragEnd) {
					// println("New breakpoint")
					currentDistance -= minDistance
					val p = event.position - positionInTop
					// println(s"Latest position is $p")
					pointsPointer.value :+= p
				}
			}
		}
		
		
		// OTHER    ---------------------------
		
		def reset() = {
			currentDistance = 0.0
			pointsPointer.clear()
		}
	}
}
