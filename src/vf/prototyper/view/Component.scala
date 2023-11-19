package vf.prototyper.view

import utopia.firmament.context.TextContext
import utopia.flow.collection.immutable.Pair
import utopia.reach.component.factory.ContextualMixed
import utopia.reach.component.input.selection.DropDown
import vf.prototyper.util.Common.context

/**
 * Contains some component factories with preset settings
 * @author Mikko Hilpinen
 * @since 18.11.2023, v1.0.1
 */
object Component
{
	// ATTRIBUTES   -------------------------
	
	/**
	 * Factory for building drop-down fields
	 */
	lazy val dropDown = DropDown.withExpandAndCollapseIcon(Pair(Icon.arrowDown.small, Icon.arrowUp.small))
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param factories Contextual component creation factories
	 * @return A factory suitable for constructing drop-down components
	 */
	def dropDownFrom(factories: ContextualMixed[TextContext]) =
		factories.mapContext { tx => context.window.withTextContext(tx).borderless }.apply(dropDown)
}
