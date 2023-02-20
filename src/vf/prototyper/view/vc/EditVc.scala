package vf.prototyper.view.vc

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.End.{First, Last}
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.event.MouseButton
import utopia.paradigm.enumeration.Alignment
import utopia.reach.component.button.image.ImageButton
import utopia.reach.component.factory.Mixed
import utopia.reach.component.input.check.CheckBox
import utopia.reach.component.input.selection.DropDown
import utopia.reach.component.input.text.TextField
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.stack.{Stack, ViewStack}
import utopia.reach.container.wrapper.Framing
import utopia.reflection.color.ColorRole
import utopia.reflection.color.ColorRole.Primary
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.container.stack.StackLayout.Center
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.stack.{StackInsets, StackLength}
import vf.prototyper.model.event.{CanvasEvent, CanvasListener}
import vf.prototyper.model.immutable.View
import vf.prototyper.model.mutable.{LinkBuilder, ViewBuilder}
import vf.prototyper.util.Common.Colors._
import vf.prototyper.util.Common._
import vf.prototyper.view.Icon

/**
 * Controls the main editing view
 * @author Mikko Hilpinen
 * @since 15.2.2023, v0.1
 */
class EditVc(original: Vector[View])
{
	// ATTRIBUTES   --------------------------
	
	private val idGen = Iterator.iterate(1) { _ + 1 }.dropWhile { i => original.exists { _.id >= i } }
	
	private val viewsPointer = new PointerWithEvents(original.map(ViewBuilder.apply))
	
	private var firstView = views.head
	
	private val currentViewPointer = new PointerWithEvents(firstView)
	private val currentLinkPointer = PointerWithEvents.empty[LinkBuilder]()
	
	private val viewNameInputPointer = new PointerWithEvents(currentView.name)
	private val isFirstViewPointer = new PointerWithEvents(true)
	private val selectedLinkTargetPointer = PointerWithEvents.empty[ViewBuilder]()
	
	private val isLinkSelectedPointer = currentLinkPointer.map { _.isDefined }
	
	private val view = ReachCanvas(cursors) { hierarchy =>
		// The view consists of 3 vertical elements where the 3rd is hidden at times:
		// 1) Header
		// 2) Canvas
		// 3) Link View
		ViewStack(hierarchy).withContext(context.base).build(Mixed)
			.apply(marginPointer = Fixed(StackLength.fixedZero)) { factories =>
				// 1: The header contains left & right -arrows, as well as an input field for the view name
				// Also allows for setting the view as the first view, as well as view deletion
				// [< | View Name | 1 | Delete | >]
				val headerBg = primary.dark
				val header = factories.next()(Framing).mapContext { _.inContextWithBackground(headerBg) }.build(Stack)
					.apply(margins.small.any, customDrawers = Vector(BackgroundDrawer(headerBg))) { stackF =>
						stackF.build(Mixed).row() { factories =>
							// Left & Right arrows
							val arrows = Pair(Icon.arrowLeft, Icon.arrowRight).mapWithSides { (icon, end) =>
								factories(ImageButton).withColouredIcon(icon.large, Primary,
									insets = StackInsets.any.mapVertical { _.withLowPriority }) {
									val currentIndex = views.indexOf(currentView)
									val nextIndex = end match {
										case First => if (currentIndex == 0) views.size - 1 else currentIndex - 1
										case Last => if (currentIndex == views.size - 1) 0 else currentIndex + 1
									}
									currentView = views(nextIndex)
								}
							}
							// Name field
							val nameField = factories.mapContext { _.forTextComponents }(TextField)
								.forString(length.field.medium.upscaling, fieldNamePointer = Fixed("View Name"),
									textPointer = viewNameInputPointer)
							// Is first -flag
							val isFirstFlag = factories(CheckBox.full(Icon.oneFilled.large, Icon.oneEmpty.large))
								.apply(isFirstViewPointer, isFirstViewPointer.map { !_ })
							// Delete button
							// TODO: Add enabled state
							val deleteButton = factories(ImageButton)
								.withColouredIcon(Icon.delete.large, ColorRole.Error) {
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
				val linkAreaBg = primary.default
				val linkArea = factories.next()(Framing).mapContext { _.inContextWithBackground(linkAreaBg) }.build(Stack)
					.apply(margins.medium.any.toInsets.expandingToRight,
						customDrawers = Vector(BackgroundDrawer(linkAreaBg))) { stackF =>
						stackF.build(Mixed).row(Center, areRelated = true) { factories =>
							val cFactories = factories.mapContext { _.forTextComponents }
							val targetDd = cFactories(DropDown).simple(viewsPointer, selectedLinkTargetPointer,
								Some(Icon.arrowDown.small), Some(Icon.arrowUp.small),
								DisplayFunction.noLocalization[ViewBuilder] { _.name },
								Fixed("Link target"), Fixed("Select a view"))
							val closeButton = cFactories(ImageButton).withIcon(Icon.closeTop.medium) {
								currentLinkPointer.clear()
							}
							val deleteButton = cFactories(ImageButton)
								.withColouredIcon(Icon.delete.medium, ColorRole.Error) {
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
	
	private val lazyWindow = ResettableLazy {
		val frame = Frame.windowed(view.parent, "Prototyper", Program, margins.medium,
			getAnchor = Alignment.Top.origin)
		frame.startEventGenerators(context.base.actorHandler)
		frame.visible = true
		frame
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
	def display() =
		lazyWindow.resettingValueIterator.find { !_.isClosed }.get.closeFuture.map { _ =>
			val firstViewIndex = views.indexOf(firstView)
			val resultViews = views.map { _.result }
			if (firstViewIndex >= 0)
				resultViews(firstViewIndex) +: resultViews.withoutIndex(firstViewIndex)
			else
				resultViews
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
