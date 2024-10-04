package vf.prototyper.view.vc

import utopia.firmament.localization.DisplayFunction
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackInsets
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.mutable.eventful.{EventfulPointer, ResettableFlag}
import utopia.genesis.handling.event.mouse.MouseButton
import utopia.paradigm.color.ColorRole.Primary
import utopia.paradigm.color.ColorShade.Dark
import utopia.paradigm.color.{Color, ColorRole}
import utopia.reach.component.button.image.ImageButton
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.check.CheckBox
import utopia.reach.component.input.text.TextField
import utopia.reach.container.multi.{Stack, ViewStack}
import utopia.reach.container.wrapper.Framing
import utopia.reach.window.ReachWindow
import vf.prototyper.model.event.{CanvasEvent, CanvasListener}
import vf.prototyper.model.immutable.View
import vf.prototyper.model.mutable.{LinkBuilder, ViewBuilder}
import vf.prototyper.util.Common._
import vf.prototyper.view.Icon

/**
 * Controls the main editing view
 * @author Mikko Hilpinen
 * @since 15.2.2023, v1.0
 */
class EditVc(original: Vector[View])
{
	// ATTRIBUTES   --------------------------
	
	private val idGen = Iterator.iterate(1) { _ + 1 }.dropWhile { i => original.exists { _.id >= i } }
	
	private val viewsPointer = EventfulPointer(original.map(ViewBuilder.apply))
	
	private var firstView = views.head
	
	private val currentViewPointer = EventfulPointer(firstView)
	private val currentLinkPointer = EventfulPointer.empty[LinkBuilder]
	
	private val viewNameInputPointer = EventfulPointer(currentView.name)
	private val isFirstViewPointer = ResettableFlag(initialValue = true)
	private val selectedLinkTargetPointer = EventfulPointer.empty[ViewBuilder]
	
	private val isLinkSelectedPointer = currentLinkPointer.map { _.isDefined }
	
	private val lazyWindow = ResettableLazy {
		// The view consists of 3 vertical elements where the 3rd is hidden at times:
		// 1) Header
		// 2) Canvas
		// 3) Link View
		// The main background is the canvas color: i.e. white
		ReachWindow.withContext(context.window.withWindowBackground(Color.white))
			.using(ViewStack, title = "Prototyper") { (_, stackF) =>
				stackF.withoutMargin.build(Mixed) { factories =>
					// 1: The header contains left & right -arrows, as well as an input field for the view name
					// Also allows for setting the view as the first view, as well as view deletion
					// [< | View Name | 1 | Delete | >]
					val header = factories.next()(Framing)
						.small.withBackground(Primary, Dark)
						.build(Stack) { stackF =>
							stackF.row.build(Mixed) { factories =>
								// Left & Right arrows
								val arrows = Pair(Icon.arrowLeft, Icon.arrowRight).mapWithSides { (icon, end) =>
									factories(ImageButton)
										.withInsets(StackInsets.any.mapVertical { _.lowPriority })
										.icon(icon.large, Some(Primary)) {
											val currentIndex = views.indexOf(currentView)
											val nextIndex = end match {
												case First => if (currentIndex == 0) views.size - 1 else currentIndex - 1
												case Last => if (currentIndex == views.size - 1) 0 else currentIndex + 1
											}
											currentView = views(nextIndex)
										}
								}
								// Name field
								val nameField = factories(TextField)
									.withFieldName("View Name")
									.string(length.field.medium.upscaling, viewNameInputPointer)
								// Is first -flag
								val isFirstFlag = factories(CheckBox.usingIcons(Icon.oneFilled.large, Icon.oneEmpty.large))
									.withEnabledPointer(!isFirstViewPointer)
									.apply(isFirstViewPointer)
								// Delete button
								// TODO: Add enabled state
								val deleteButton = factories(ImageButton)
									.icon(Icon.delete.large, Some(ColorRole.Failure)) {
										if (views.hasSize > 1) {
											// Deletes the view
											val viewToRemove = currentView
											val index = views.indexOf(viewToRemove)
											if (index == 0)
												currentView = views(1)
											else
												currentView = views(index - 1)
											views = views.withoutIndex(index)
											// Also deletes all links to that view
											views.foreach { _.removeLinksTo(viewToRemove) }
										}
									}
								
								Vector(arrows.first, nameField, isFirstFlag, deleteButton, arrows.second)
							}
						}
					// 2: Link view
					// [ Target Dropdown | OK | Delete ]
					val linkArea = factories.next()(Framing).expandingToRight.withBackground(Primary)
						.build(Stack) { stackF =>
							stackF.centeredRow.related.build(Mixed) { factories =>
								val targetDd = factories
									.mapContext(context.window.withTextContext)(component.dropDown)
									.withFieldName("Link target")
									.withPrompt("Select a view")
									.simple(viewsPointer, selectedLinkTargetPointer,
										displayFunction = DisplayFunction.noLocalization[ViewBuilder] { _.name })
								val closeButton = factories(ImageButton).icon(Icon.closeTop.medium) {
									currentLinkPointer.clear()
								}
								val deleteButton = factories(ImageButton)
									.icon(Icon.delete.medium, Some(ColorRole.Failure)) {
										currentLinkPointer.pop().foreach(currentView.removeLink)
									}
								
								Vector(targetDd, closeButton, deleteButton)
							}
						}
					// 3: Canvas
					val canvas = new EditCanvasVc(currentViewPointer, factories.next().parentHierarchy)
					canvas.addListener(EditCanvasListener)
					
					// The link area is shown only when a link is selected
					Vector(
						header.parent -> AlwaysTrue,
						linkArea.parent -> isLinkSelectedPointer,
						canvas.view -> AlwaysTrue
					)
				}
			}
	}
	
	
	// INITIAL CODE --------------------------
	
	// Converts the original links to builders and assigns them to the newly created view builders
	original.zip(views).foreach { case (original, builder) =>
		builder.links = original.links.map { link => new LinkBuilder(link.area, builderForId(link.targetId).get) }
	}
	
	currentViewPointer.addContinuousListener { e =>
		currentLink = None
		viewNameInputPointer.value = e.newValue.name
		isFirstViewPointer.value = e.newValue == firstView
	}
	viewNameInputPointer.addContinuousListener { e => currentView.name = e.newValue }
	isFirstViewPointer.addContinuousListener { e => if (e.newValue) firstView = currentView }
	
	currentLinkPointer.addContinuousListener { _.newValue.foreach { link =>
		selectedLinkTargetPointer.value = Some(link.target)
	} }
	selectedLinkTargetPointer.addContinuousListener { _.newValue
		.foreach { newTarget => currentLink.foreach { _.target = newTarget } }
	}
	
	
	// COMPUTED ------------------------------
	
	private def views = viewsPointer.value
	private def views_=(newViews: Vector[ViewBuilder]) = viewsPointer.value = newViews
	
	private def currentView = currentViewPointer.value
	private def currentView_=(newView: ViewBuilder) = currentViewPointer.value = newView
	
	private def currentLink = currentLinkPointer.value
	private def currentLink_=(newLink: Option[LinkBuilder]) = currentLinkPointer.value = newLink
	
	
	// OTHER    ------------------------------
	
	/**
	 * Displays this view
	 * @return Future that resolves into edit results (views)
	 */
	def display() = {
		lazyWindow.filterNot { _.hasClosed }
		val window = lazyWindow.value
		window.display(centerOnParent = true)
		window.closeFuture.map { _ =>
			val firstViewIndex = views.indexOf(firstView)
			val resultViews = views.map { _.result }
			if (firstViewIndex >= 0)
				resultViews(firstViewIndex) +: resultViews.withoutIndex(firstViewIndex)
			else
				resultViews
		}
	}
	
	private def builderForId(viewId: Int) = views.find { _.id == viewId }
	
	
	// NESTED   ------------------------------
	
	private object EditCanvasListener extends CanvasListener
	{
		override def onClick(event: CanvasEvent.ClickEvent): Unit =
			currentView.links.find { _.area.contains(event.point) }.foreach { link =>
				// Case: Ctrl + click => Move
				if (event.wasCtrlPressed)
					currentView = link.target
				// Case: Click => Edit
				else
					currentLink = Some(link)
			}
		
		override def onDrag(event: CanvasEvent.DragEvent): Unit = {
			// Either creates a new sub-view or a link
			if (event.mouseButton == MouseButton.Left) {
				val newView = currentView.subView(idGen.next(), event.area)
				views = views.inserted(newView, views.indexOf(currentView) + 1)
				currentView = newView
			}
			else
				currentLink = Some(currentView.addLink(event.area))
		}
	}
}
