package vf.prototyper.view

import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.genesis.image.Image
import utopia.paradigm.measurement.DistanceExtensions._
import utopia.paradigm.shape.shape2d.Size
import utopia.reflection.image.SingleColorIcon
import vf.prototyper.util.Common._
import vf.prototyper.view.Icon.{largeSize, mediumSize, smallSize}

import java.nio.file.Path
import scala.util.{Failure, Success}

/**
 * An access point for icons
 * @author Mikko Hilpinen
 * @since 15.2.2023, v0.1
 */
object Icon
{
	// ATTRIBUTES   -------------------------
	
	/**
	 * An empty icon
	 */
	val empty = new Icon(SingleColorIcon.empty)
	
	private val iconsDir: Path = "data/icons"
	
	private val mediumSize = Size.square(1.cm.toPixels.toInt)
	private val largeSize = mediumSize * 1.25
	private val smallSize = mediumSize * 0.75
	
	/**
	 * @return A right-facing arrow icon
	 */
	lazy val arrowRight = load("arrow-right")
	/**
	 * A left-facing arrow icon
	 */
	lazy val arrowLeft = arrowRight.map { _.flippedHorizontally }
	/**
	 * A down-facing arrow icon
	 */
	lazy val arrowDown = load("arrow-down")
	/**
	 * An up-facing arrow icon
	 */
	lazy val arrowUp = arrowDown.map { _.flippedVertically }
	
	/**
	 * Close top panel -icon
	 */
	lazy val closeTop = load("close-top")
	
	/**
	 * A delete icon (thrash can)
	 */
	lazy val delete = load("delete")
	
	/**
	 * An empty / non-filled 1-icon
	 */
	lazy val oneEmpty = load("1-empty")
	/**
	 * A filled 1-icon
	 */
	lazy val oneFilled = load("1-filled")
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param isFilled Whether the icon should be filled
	 * @return A 1-icon with specified fill setting
	 */
	def one(isFilled: Boolean) = if (isFilled) oneFilled else oneEmpty
	
	private def load(name: String) = Image.readFrom(iconsDir / name.endingWith(".png")) match {
		case Success(img) => new Icon(new SingleColorIcon(img))
		case Failure(error) =>
			log(error, s"Failed to load image named '$name'")
			empty
	}
}

class Icon(raw: SingleColorIcon)
{
	// ATTRIBUTES   -----------------------
	
	/**
	 * A medium-sized copy of this icon
	 */
	lazy val medium = sized(mediumSize)
	/**
	 * A larger copy of this icon
	 */
	lazy val large = sized(largeSize)
	/**
	 * A smaller copy of this icon
	 */
	lazy val small = sized(smallSize)
	
	
	// OTHER    ----------------------------
	
	def map(f: Image => Image) = new Icon(raw.map(f))
	
	private def sized(size: Size) = raw.mapSize { _.fittingWithin(size, maximize = true) }
}
