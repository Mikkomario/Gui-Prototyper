package vf.prototyper.model.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.mutable.MutableMatrix
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.{IntType, StringType}
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.util.TryExtensions._
import utopia.flow.view.mutable.Pointer
import utopia.genesis.image.{Image, Pixels}
import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.size.Size
import vf.prototyper.util.Common._

import java.nio.file.{Path, Paths}
import scala.concurrent.Future
import scala.util.Try

object View extends FromModelFactory[View]
{
	private lazy val schema = ModelDeclaration("id" -> IntType, "name" -> StringType, "path" -> StringType)
	
	override def apply(model: ModelLike[Property]): Try[View] = schema.validate(model).flatMap { model =>
		model("links").tryVectorWith { v => Link(v.getModel) }.map { links =>
			apply(model("id").getInt, model("name").getString, Paths.get(model("path").getString),
				model("region").bounds, links)
		}
	}
}

/**
 * Represents a single UI view
 * @author Mikko Hilpinen
 * @since 15.2.2023, v1.0
 * @param id Unique id of this view
 * @param name Name of this view
 * @param path Path to the used image file
 * @param region Applicable region of the source image file.
 *               In relative coordinate system, where (1,1) is the bottom right corner and (0,0) is the top-left corner.
 * @param links sub-regions relative to the top-left corner of this view that link to specific views
 */
case class View(id: Int, name: String, path: Path, region: Option[Bounds] = None, links: Vector[Link] = Vector())
	extends ModelConvertible
{
	// ATTRIBUTES   ----------------------------
	
	/**
	 * The image used by this view
	 */
	lazy val image = {
		// Loads the main image
		val base = Image.readFrom(path).getOrMap { e =>
			log(e)
			Image.empty
		}
		// Cuts out the correct sub-region, if appropriate
		region match {
			case Some(region) => base.subImage(region * base.size)
			case None => base
		}
	}
	
	
	// IMPLEMENTED  ---------------------------
	
	override def toModel: Model =
		Model.from("id" -> id, "name" -> name, "path" -> path.toString, "region" -> region,
			"links" -> links)
	
	
	// OTHER    ------------------------------
	
	/**
	 * @param view A view
	 * @return Whether this view is a parent view of the specified view
	 */
	def isParentOf(view: View) =
		path == view.path && view.region.exists { subRegion =>
			region.forall { _.contains(subRegion) }
		}
	
	/**
	 * Converts this view into an image where links have been updated
	 * @param maxSize Maximum resolution of the resulting image
	 * @return Future that resolves into the view image when completed
	 */
	def viewImage(maxSize: Size) = Future {
		val base = image.fittingWithin(maxSize).withMinimumResolution
		if (links.nonEmpty) {
			println("Starting image conversion")
			// Converts link area hues
			val linkColor = color.secondary.dark
			val linkLuminosity = linkColor.luminosity
			
			def replaceColor(color: Color) = if (color.luminosity <= linkLuminosity) linkColor else color
			
			base.mapPixels { pixels =>
				links.only match {
					case Some(link) => pixels.mapArea(link.area * base.sourceResolution)(replaceColor)
					case None =>
						val mutablePixels = MutableMatrix(pixels.map { Pointer(_) })
						links.foreach { link =>
							mutablePixels.view((link.area * base.sourceResolution)
								.xyPair.map { s => NumericSpan(s.start.toInt, s.end.toInt) }).pointers
								.iterator.foreach { _.update(replaceColor) }
						}
						Pixels(mutablePixels.currentState)
				}
			}
		}
		else
			base
	}
}