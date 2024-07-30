package vf.prototyper.model.event

import vf.prototyper.model.event.CanvasEvent.{ClickEvent, DragEvent}

/**
 * Listeners that are informed about canvas events
 * @author Mikko Hilpinen
 * @since 16.2.2023, v1.0
 */
trait CanvasListener
{
	/**
	 * This function is called when the user drags within the listened canvas
	 * @param event The event that occurred
	 */
	def onDrag(event: DragEvent): Unit
	/**
	 * This function is called when the user clicks within the listened canvas
	 * @param event The event that occurred
	 */
	def onClick(event: ClickEvent): Unit
}
