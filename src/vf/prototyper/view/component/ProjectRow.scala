package vf.prototyper.view.component

import utopia.firmament.component.display.RefreshableWithPointer
import utopia.firmament.context.TextContext
import utopia.firmament.model.enumeration.SizeCategory.Medium
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.paradigm.color.ColorRole
import utopia.paradigm.color.ColorRole.Secondary
import utopia.reach.component.button.image.ImageButton
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.Mixed
import utopia.reach.component.factory.contextual.TextContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.container.multi.Stack
import vf.prototyper.model.immutable.Project
import vf.prototyper.view.Icon

object ProjectRow extends Ccff[TextContext, ContextualProjectRowFactory]
{
	override def withContext(hierarchy: ComponentHierarchy, context: TextContext): ContextualProjectRowFactory =
		ContextualProjectRowFactory(hierarchy, context)
}

case class ContextualProjectRowFactory(hierarchy: ComponentHierarchy, override val context: TextContext)
	extends TextContextualFactory[ContextualProjectRowFactory]
{
	// IMPLEMENTED  ---------------------
	
	override def self: ContextualProjectRowFactory = this
	
	override def withContext(context: TextContext): ContextualProjectRowFactory = copy(context = context)
	
	
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
	
	override val contentPointer = EventfulPointer(initialProject)
	
	private val namePointer = contentPointer.map { _.name }
	
	// Wraps a stack where the project name is displayed on the left and the buttons on the right
	override protected val wrapped = Stack(hierarchy).withContext(context).row.withCap(Medium).related
		.build(Mixed) { factories =>
			val nameLabel = factories(ViewTextLabel).mapContext { _.withTextExpandingToRight }.apply(namePointer)
			val viewButton = factories(ImageButton)
				.icon(Icon.slideShow.medium, Some(Secondary)) { viewAction(content) }
			val editButton = factories(ImageButton).icon(Icon.edit.medium, Some(Secondary)) { editAction(content) }
			val deleteButton = factories(ImageButton).icon(Icon.delete.medium, Some(ColorRole.Failure)) {
				deleteAction(content) }
			
			Vector(nameLabel, viewButton, editButton, deleteButton)
		}
}
