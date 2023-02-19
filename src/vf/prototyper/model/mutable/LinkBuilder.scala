package vf.prototyper.model.mutable

import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.paradigm.shape.shape2d.Bounds
import vf.prototyper.model.immutable.Link

/**
 * A mutable link implementation used during view-building
 * @author Mikko Hilpinen
 * @since 15.2.2023, v0.1
 * @param area The area where this link is triggered from
 */
class LinkBuilder(val area: Bounds, initialTarget: ViewBuilder)
{
	// ATTRIBUTES   -------------------------
	
	/**
	 * A mutable pointer to the link target view
	 */
	val targetPointer = new PointerWithEvents(initialTarget)
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return The current target of this link
	 */
	def target = targetPointer.value
	def target_=(view: ViewBuilder) = targetPointer.value = view
	
	/**
	 * @return An immutable copy from the current state of this builder
	 */
	def result = Link(area, target.id)
}
