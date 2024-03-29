package vf.prototyper.model.immutable

import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.paradigm.generic.ParadigmDataType.BoundsType
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

object Link extends FromModelFactoryWithSchema[Link]
{
	override lazy val schema: ModelDeclaration = ModelDeclaration("area" -> BoundsType, "targetId" -> IntType)
	
	override protected def fromValidatedModel(model: Model): Link =
		apply(model("area").getBounds, model("targetId").getInt)
}

/**
 * Represents a link to another view
 * @author Mikko Hilpinen
 * @since 15.2.2023, v0.1
 * @param area Area from which this link is triggered.
 *             (0,0) is top-left corner of the parent view. (1,1) is the bottom right corner.
 * @param targetId Id of the referred view
 */
case class Link(area: Bounds, targetId: Int) extends ModelConvertible
{
	override def toModel: Model = Model.from("area" -> area, "targetId" -> targetId)
}
