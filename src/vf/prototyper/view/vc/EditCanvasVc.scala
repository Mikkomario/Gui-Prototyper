package vf.prototyper.view.vc

import utopia.firmament.drawing.template.CustomDrawer
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.graphics.DrawLevel.Foreground
import utopia.genesis.graphics.Priority.Low
import utopia.genesis.graphics.{DrawLevel, DrawSettings, Drawer, StrokeSettings}
import utopia.genesis.handling.event.mouse._
import utopia.genesis.util.Screen
import utopia.paradigm.color.Color
import utopia.paradigm.color.ColorShade.Light
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.ViewImageLabel
import vf.prototyper.model.event.CanvasEvent.{ClickEvent, DragEvent}
import vf.prototyper.model.event.CanvasListener
import vf.prototyper.model.mutable.ViewBuilder
import vf.prototyper.util.Common.Colors._

/**
 * Used for controlling the canvas view
 * @author Mikko Hilpinen
 * @since 15.2.2023, v0.1
 */
class EditCanvasVc(viewPointer: Changing[ViewBuilder], hierarchy: ComponentHierarchy)
{
	// ATTRIBUTES   ---------------------------
	
	private val minDragDistance = 25
	
	private lazy val leftDragColor = primary.against(Color.white, Light).withAlpha(0.5)
	private lazy val linkColor = secondary.against(Color.white, Light).withAlpha(0.33)
	
	private val maxImageSize = Screen.size * Vector2D(1, 0.8)
	private val imagePointer = viewPointer.map { _.image.fittingWithin(maxImageSize) }
	
	/**
	 * The controlled view
	 */
	val view = ViewImageLabel(hierarchy)
		.withCustomDrawers(Vector(LinksDrawer, CanvasMouseListener))(imagePointer)
	
	private var listeners = Vector[CanvasListener]()
	
	
	// INITIAL CODE --------------------------
	
	// Listens to user clicks and drags
	view.addMouseButtonListener(CanvasMouseListener)
	view.addMouseMoveListener(CanvasMouseListener)
	
	
	// COMPUTED ------------------------------
	
	private def currentView = viewPointer.value
	
	
	// OTHER    ------------------------------
	
	/**
	 * @param listener A new listener to be informed of canvas events
	 */
	def addListener(listener: CanvasListener) = listeners :+= listener
	
	
	// NESTED   ------------------------------
	
	private object LinksDrawer extends CustomDrawer
	{
		// ATTRIBUTES   --------------------
		
		private implicit val ds: DrawSettings = DrawSettings.onlyFill(linkColor)
		
		
		// IMPLEMENTED  --------------------
		
		override def opaque: Boolean = false
		override def drawLevel: DrawLevel = Foreground
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = {
			currentView.links.foreach { link =>
				drawer.draw(link.area * bounds.size + bounds.position)
			}
		}
	}
	
	private object CanvasMouseListener extends MouseButtonStateListener with MouseMoveListener with CustomDrawer
	{
		// ATTRIBUTES   --------------------
		
		private var atClickButton: MouseButton = MouseButton.Left
		
		private val dragPointer = EventfulPointer.empty[Pair[Point]]()
		
		private val dragBoundsPointer = dragPointer.map { _.map(Bounds.between) }
		private val isDraggingPointer: FlagLike = dragPointer.strongMap { _.isDefined }
		
		override val mouseButtonStateEventFilter = MouseEvent.filter.over(view.bounds)
		
		
		// INITIAL CODE --------------------
		
		// Repaints when drag changes
		dragBoundsPointer.addContinuousListener { event =>
			view.repaintArea(Bounds.around(event.values.flatten).enlarged(2) - view.position, Low)
		}
		
		
		// COMPUTED ------------------------
		
		private def isDragging = isDraggingPointer.value
		
		
		// IMPLEMENTED  --------------------
		
		override def opaque: Boolean = false
		override def drawLevel: DrawLevel = Foreground
		
		override def handleCondition: FlagLike = AlwaysTrue
		override def mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
			// Case: Drag or click start
			if (event.pressed) {
				atClickButton = event.button
				dragPointer.value = Some(Pair.twice(event.position.relative))
			}
			// Case: Drag or click end / release
			else {
				// Ends the drag
				dragPointer.pop().foreach { drag =>
					// Case: Too short a distance for drag => click
					if (drag.merge { _ - _ }.length < minDragDistance) {
						lazy val clickEvent = ClickEvent(toCanvasCoordinate(event.position), atClickButton)
						listeners.foreach { _.onClick(clickEvent) }
					}
					// Case: Drag
					else {
						lazy val dragEvent = DragEvent(Bounds.between(drag.map(toCanvasCoordinate)), atClickButton)
						listeners.foreach { _.onDrag(dragEvent) }
					}
				}
			}
		}
		
		// Updates the drag coordinates
		override def onMouseMove(event: MouseMoveEvent): Unit = {
			if (isDragging)
				dragPointer.update { _.map { _.withSecond(event.position) } }
		}
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = {
			dragBoundsPointer.value.foreach { drag =>
				implicit val ds: DrawSettings = {
					if (atClickButton == MouseButton.Left)
						StrokeSettings(leftDragColor, 2)
					else
						DrawSettings.onlyFill(linkColor)
				}
				drawer.draw(drag)
			}
		}
		
		
		// OTHER    ---------------------
		
		private def toCanvasCoordinate(componentCoordinate: Point) =
			(componentCoordinate - view.position) / view.size
	}
}
