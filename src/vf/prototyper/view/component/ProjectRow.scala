package vf.prototyper.view.component

import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.reach.component.button.image.ImageButton
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.container.multi.stack.Stack
import utopia.reflection.color.ColorRole
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.component.context.TextContext
import utopia.reflection.component.template.display.RefreshableWithPointer
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.shape.LengthExtensions._
import vf.prototyper.model.immutable.Project
import vf.prototyper.util.Common._
import vf.prototyper.view.Icon

object ProjectRow
	extends ContextInsertableComponentFactoryFactory[TextContext, ProjectRowFactory, ContextualProjectRowFactory]
{
	override def apply(hierarchy: ComponentHierarchy): ProjectRowFactory = new ProjectRowFactory(hierarchy)
}

class ProjectRowFactory(hierarcy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContext, ContextualProjectRowFactory]
{
	override def withContext[N <: TextContext](context: N): ContextualProjectRowFactory[N] =
		new ContextualProjectRowFactory[N](hierarcy, context)
}

class ContextualProjectRowFactory[N <: TextContext](hierarchy: ComponentHierarchy, override val context: N)
	extends ContextualComponentFactory[N, TextContext, ContextualProjectRowFactory]
{
	// IMPLEMENTED  ---------------------
	
	override def withContext[N2 <: TextContext](newContext: N2): ContextualProjectRowFactory[N2] =
		new ContextualProjectRowFactory[N2](hierarchy, newContext)
		
	
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
			val nameLabel = factories(ViewTextLabel).mapContext { _.expandingToRight }.apply(namePointer)
			val viewButton = factories(ImageButton)
				.withColouredIcon(Icon.slideShow.medium, Secondary) { viewAction(content) }
			val editButton = factories(ImageButton).withColouredIcon(Icon.edit.medium, Secondary) { editAction(content) }
			val deleteButton = factories(ImageButton).withColouredIcon(Icon.delete.medium,
				ColorRole.Error) { deleteAction(content) }
			
			Vector(nameLabel, viewButton, editButton, deleteButton)
		}
}
