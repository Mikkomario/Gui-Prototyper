package vf.prototyper.view.component

import utopia.firmament.component.display.RefreshableWithPointer
import utopia.firmament.context.TextContext
import utopia.firmament.model.enumeration.StackLayout.Center
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.paradigm.color.ColorRole
import utopia.paradigm.color.ColorRole.Secondary
import utopia.reach.component.button.image.ImageButton
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.{Mixed, TextContextualFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.container.multi.Stack
import vf.prototyper.model.immutable.Project
import vf.prototyper.util.Common._
import vf.prototyper.view.Icon

object ProjectRow extends Ccff[TextContext, ContextualProjectRowFactory]
{
	override def withContext(hierarchy: ComponentHierarchy, context: TextContext): ContextualProjectRowFactory =
		new ContextualProjectRowFactory(hierarchy, context)
}

class ContextualProjectRowFactory(hierarchy: ComponentHierarchy, override val context: TextContext)
	extends TextContextualFactory[ContextualProjectRowFactory]
{
	// IMPLEMENTED  ---------------------
	
	override def self: ContextualProjectRowFactory = this
	
	override def withContext(newContext: TextContext): ContextualProjectRowFactory =
		new ContextualProjectRowFactory(hierarchy, newContext)
		
	
	// OTHER    -------------------------
	
	/**
	 * Creates a new project row
	 * @param initialProject The initially displayed row
	 * @param view View action
	 * @param edit Edit action
	 * @param delete Delete action
	 * @return A new project row
	 */
	def apply(initialProject: Project)(view: Project => Unit)(edit: Project => Unit)(delete: Project => Unit) =
		new ProjectRow(hierarchy, context, initialProject, view, edit, delete)
}

/**
 * Displays a single project's information
 * @author Mikko Hilpinen
 * @since 20.2.2023, v0.1
 */
class ProjectRow(hierarchy: ComponentHierarchy, context: TextContext, initialProject: Project,
                 viewAction: Project => Unit, editAction: Project => Unit, deleteAction: Project => Unit)
	extends ReachComponentWrapper with RefreshableWithPointer[Project]
{
	// ATTRIBUTES   -----------------------
	
	override val contentPointer = new PointerWithEvents(initialProject)
	
	private val namePointer = contentPointer.map { _.name }
	
	// Wraps a stack where the project name is displayed on the left and the buttons on the right
	override protected val wrapped = Stack(hierarchy).withContext(context).build(Mixed)
		.row(Center, cap = margins.medium.any, areRelated = true) { factories =>
			val nameLabel = factories(ViewTextLabel).mapContext { _.withTextExpandingToRight }.apply(namePointer)
			val viewButton = factories(ImageButton)
				.withColouredIcon(Icon.slideShow.medium, Secondary) { viewAction(content) }
			val editButton = factories(ImageButton).withColouredIcon(Icon.edit.medium, Secondary) { editAction(content) }
			val deleteButton = factories(ImageButton).withColouredIcon(Icon.delete.medium,
				ColorRole.Failure) { deleteAction(content) }
			
			Vector(nameLabel, viewButton, editButton, deleteButton)
		}
}
