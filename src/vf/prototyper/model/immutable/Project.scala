package vf.prototyper.model.immutable

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.parse.file.FileExtensions._
import vf.prototyper.util.Common._

import java.nio.file.Path
import scala.util.Try

object Project
{
	// ATTRIBUTES   ------------------------
	
	private lazy val schema = ModelDeclaration("id" -> StringType, "name" -> StringType)
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param path Targeted project file path
	 * @return A new factory for reading a project from that path
	 */
	def apply(path: Path) = new ProjectFromFileFactory(path)
	
	/**
	 * Loads all saved projects
	 * @return Success or failure. On success, returns failed project loads and successful loads.
	 */
	def loadAll() =
		directory.projects.tryIterateChildrenCatching { _.map { p => apply(p).pull }.toTryCatch }
	
	
	// NESTED   ---------------------------
	
	class ProjectFromFileFactory(path: Path) extends FromModelFactory[Project]
	{
		// COMPUTED -----------------------
		
		/**
		 * Reads a project from the specified path
		 * @return Read project, if successful
		 */
		def pull = JsonBunny.munchPath(path).flatMap { v => apply(v.getModel) }
		
		
		// IMPLEMENTED  -------------------
		
		override def apply(model: ModelLike[Property]): Try[Project] = schema.validate(model).flatMap { model =>
			model("views").tryVectorWith { v => View(v.getModel) }.map { views =>
				Project(model("id").getString, model("name").getString, path, views)
			}
		}
	}
}

/**
 * Represents a project or an application, which consists of multiple views
 * @author Mikko Hilpinen
 * @since 19.2.2023, v0.1
 * @param id Unique id of this project
 * @param name Name of this project
 * @param path to this project's json file
 * @param views Views that belong to this project
 */
case class Project(id: String, name: String, path: Path, views: Vector[View]) extends ModelConvertible
{
	// IMPLEMENTED   -------------------------
	
	override def toModel: Model = Model.from("id" -> id, "name" -> name, "views" -> views)
	
	
	// OTHER    -----------------------------
	
	/**
	 * @return Saves this project into a local file
	 */
	def save() = path.writeJson(this)
}
