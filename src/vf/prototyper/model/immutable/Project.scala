package vf.prototyper.model.immutable

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.parse.file.FileExtensions._
import vf.prototyper.util.Common._

import scala.util.Try

object Project extends FromModelFactory[Project]
{
	// ATTRIBUTES   ------------------------
	
	private lazy val projectsDirectory = dataDirectory/"projects"
	
	private lazy val schema = ModelDeclaration("name" -> StringType)
	
	
	// IMPLEMENTED  ------------------------
	
	override def apply(model: ModelLike[Property]): Try[Project] = schema.validate(model).toTry.flatMap { model =>
		model("views").tryVectorWith { v => View(v.getModel) }.map { views =>
			apply(model("name").getString, views)
		}
	}
	
	
	// OTHER    ----------------------------
	
	/**
	 * Loads all saved projects
	 * @return Success or failure. On success, returns failed project loads and successful loads.
	 */
	def loadAll() = projectsDirectory
		.tryIterateChildren { _.map { p => JsonBunny.munchPath(p).flatMap { v => apply(v.getModel) } }.toTryCatch }
}

/**
 * Represents a project or an application, which consists of multiple views
 * @author Mikko Hilpinen
 * @since 19.2.2023, v0.1
 * @param name Name of this project
 * @param views Views that belong to this project
 */
case class Project(name: String, views: Vector[View]) extends ModelConvertible
{
	// IMPLEMENTED   -------------------------
	
	override def toModel: Model = Model.from("name" -> name, "views" -> views)
}
