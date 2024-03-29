package vf.prototyper.model.mutable

import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import vf.prototyper.model.immutable.View

import java.nio.file.Path

object ViewBuilder
{
	/**
	 * Converts an immutable view into a new builder
	 * @param original The original, copied view
	 * @return A new view builder based on that view
	 */
	def apply(original: View): ViewBuilder =
		new ViewBuilder(original.id, original.name, original.path, original.image, original.region)
}

/**
 * A mutable builder used for constructing views in edit mode
 * @author Mikko Hilpinen
 * @since 15.2.2023, v0.1
 * @param id Unique id of this view
 * @param initialName The initial name of this view
 * @param path Path to the image file used in this view
 * @param image Image of this view
 * @param region Region of the image used by this view. None if uses the whole image.
 */
class ViewBuilder(val id: Int, initialName: String, val path: Path, val image: Image, val region: Option[Bounds] = None)
{
	// ATTRIBUTES   --------------------------
	
	/**
	 * A mutable pointer to the name of this view
	 */
	val namePointer = EventfulPointer(initialName)
	/**
	 * A mutable pointer to the links within this view, each being represented with a mutable version
	 */
	val linksPointer = EventfulPointer(Vector[LinkBuilder]())
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return The current name given to this view
	 */
	def name = namePointer.value
	def name_=(newName: String) = namePointer.value = newName
	
	/**
	 * @return Links within this view, in builder form
	 */
	def links = linksPointer.value
	def links_=(newLinks: Vector[LinkBuilder]) = linksPointer.value = newLinks
	
	/**
	 * @return An immutable copy of the current state of this builder
	 */
	def result = View(id, name, path, region, links.map { _.result })
	
	
	// OTHER    ----------------------------
	
	/**
	 * Creates a new sub-view of this view
	 * @param id Id of the new view
	 * @param area Area of the new view. Between (0,0) and (1,1). Relative to this view.
	 * @return A new sub-region of this view
	 */
	def subView(id: Int, area: Bounds) = {
		val subRegion = region match {
			case Some(myRegion) => area * myRegion.size + myRegion.position
			case None => area
		}
		// Creates the sub-view
		val view = new ViewBuilder(id, s"$name-sub", path, image.subImage(area * image.size),
			Some(subRegion))
		// Also creates a link to that view
		links :+= new LinkBuilder(subRegion, view)
		view
	}
	
	/**
	 * Adds a new link to this view
	 * @param area The area over which the link is located. Between (0,0) and (1,1). Relative to this view.
	 * @return The newly created link
	 */
	def addLink(area: Bounds) = {
		val link = new LinkBuilder(area, this)
		links :+= link
		link
	}
	/**
	 * Removes a link from this view
	 * @param link A link to remove
	 */
	def removeLink(link: LinkBuilder) = links = links.filterNot { _ == link }
	/**
	 * Removes all links from this view that point the specified view
	 * @param view A view that shall no longer be linked
	 */
	def removeLinksTo(view: ViewBuilder) = links = links.filterNot { _.target == view }
}
