package vf.prototyper.model.event

import utopia.genesis.event.{KeyStatus, MouseButton}
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.paradigm.shape.shape2d.{Bounds, Point}

import java.awt.event.KeyEvent

/**
 * Common trait for different events triggered from a canvas component
 * @author Mikko Hilpinen
 * @since 16.2.2023, v0.1
 */
sealed trait CanvasEvent
{
	// ABSTRACT --------------------------
	
	/**
	 * @return The keyboard state during this event
	 */
	def keyState: KeyStatus
	/**
	 * @return The mouse button used in this event
	 */
	def mouseButton: MouseButton
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return Whether a control key was pressed during this event
	 */
	def wasCtrlPressed = keyState(KeyEvent.VK_CONTROL)
	/**
	 * @return Whether a shift key was pressed during this event
	 */
	def wasShiftPressed = keyState(KeyEvent.VK_SHIFT)
}

object CanvasEvent
{
	/**
	 * Represents an event where the user dragged within the canvas
	 * @param area        The drag area as a set of bounds. Between (0,0) and (1,1).
	 * @param mouseButton The mouse button used in the drag (default = left)
	 * @param keyState    Keystate during the drag (default = current key status)
	 */
	case class DragEvent(area: Bounds, mouseButton: MouseButton = MouseButton.Left,
	                     keyState: KeyStatus = GlobalKeyboardEventHandler.keyStatus)
		extends CanvasEvent
	
	/**
	 * Represents an event where the user clicks within the canvas
	 * @param point       The point where the user clicked. Between (0,0) and (1,1).
	 * @param mouseButton The mouse button pressed (default = left)
	 * @param keyState    Keystate during the click (default = current key status)
	 */
	case class ClickEvent(point: Point, mouseButton: MouseButton = MouseButton.Left,
	                      keyState: KeyStatus = GlobalKeyboardEventHandler.keyStatus)
		extends CanvasEvent
}