package vf.prototyper.model.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.mutable.DataType.{IntType, StringType}
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.Bounds
import utopia.paradigm.generic.ParadigmValue._
import vf.prototyper.util.Common._

import java.nio.file.{Path, Paths}
import scala.util.Try

object View extends FromModelFactory[View]
{
	private lazy val schema = ModelDeclaration("id" -> IntType, "name" -> StringType, "path" -> StringType)
	
	override def apply(model: ModelLike[Property]): Try[View] = schema.validate(model).toTry.flatMap { model =>
		model("links").tryVectorWith { v => Link(v.getModel) }.map { links =>
			apply(model("id").getInt, model("name").getString, Paths.get(model("path").getString),
				model("region").bounds, links)
		}
	}
}

/**
 * Represents a single UI view
 * @author Mikko Hilpinen
 * @since 15.2.2023, v0.1
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
}