package vf.prototyper.util

import utopia.flow.async.context.ThreadPool
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.genesis.handling.ActorLoop
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.util.Screen
import utopia.paradigm.measurement.DistanceExtensions._
import utopia.paradigm.measurement.Ppi
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.cursor.{CursorSet, CursorType}
import utopia.reflection.color.{ColorScheme, ColorSet}
import utopia.reflection.component.context.{BaseContext, ScrollingContext}
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.Margins
import utopia.reflection.text.Font

import java.nio.file.Paths
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
 * An interface for commonly used / shared values and items
 * @author Mikko Hilpinen
 * @since 15.2.2023, v0.1
 */
object Common
{
	// ATTRIBUTES   -------------------------
	
	/**
	 * The logging implementation used
	 */
	implicit val log: Logger = SysErrLogger
	/**
	 * Execution context used for multi-threading
	 */
	implicit val exc: ExecutionContext = new ThreadPool("GUI-Prototyper").executionContext
	/**
	 * Pixels per inch of the local screen
	 */
	implicit val ppi: Ppi = Screen.ppi
	private val actorHandler = ActorHandler()
	/**
	 * Settings for scrolling
	 */
	implicit val scrolling: ScrollingContext = ScrollingContext.withLightRoundedBar(actorHandler)
	/**
	 * Language code for localization
	 */
	implicit val languageCode: String = "en"
	/**
	 * Localization implementation used
	 */
	implicit val localizer: Localizer = NoLocalization
	
	private val actorLoop = new ActorLoop(actorHandler)
	
	/**
	 * The standard margins used
	 */
	val margins = Margins((0.25.cm.toPixels / 2.0).toInt * 2)
	
	/**
	 * Directory that stores the data used in this application
	 */
	val dataDirectory = Paths.get("data")
	/**
	 * The cursors used
	 */
	val cursors = {
		// Loads the cursors from icon files. Logs failures.
		val cursorDirectory = dataDirectory/"cursors"
		import CursorType._
		CursorSet.loadIcons(
			Map[CursorType, (String, Point)](
				(Default, ("cursor-arrow", Point(7, 4))),
				(Interactive, ("cursor-hand", Point(9, 1))),
				(Text, ("cursor-text", Point(12, 12)))
			).view.mapValues { case (name, origin) =>
				cursorDirectory/name.endingWith(".png") -> origin
			}.toMap) match {
				case Success((failures, cursors)) =>
					failures.headOption.foreach { error => log(error, s"Failed to load ${ failures.size } cursors") }
					Some(cursors)
				case Failure(error) =>
					log(error, "Failed to load cursors")
					None
			}
	}
	
	
	// INITIAL CODE ------------------------
	
	actorLoop.runAsync()
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @return Access point to color values
	 */
	def color = Colors
	/**
	 * @return Access point to context options
	 */
	def context = Context
	/**
	 * @return Access point to various lengths
	 */
	def length = Lengths
	
	
	// NESTED   ----------------------------
	
	object Colors
	{
		// ATTRIBUTES   --------------------
		
		/**
		 * The primary color set
		 */
		val primary = ColorSet.fromHexes("#e64a19", "#ff7d47", "#ac0800").get
		/**
		 * The secondary color set
		 */
		val secondary = ColorSet.fromHexes("#1976d2", "#63a4ff", "#004ba0").get
		
		/**
		 * Color scheme used
		 */
		val scheme = ColorScheme.twoTone(primary, secondary, gray)
		
		
		// COMPUTED --------------------------
		
		/**
		 * The gray color set
		 */
		def gray = ColorScheme.defaultLightGray
	}
	
	object Context
	{
		/**
		 * The basic component creation context
		 */
		val base = BaseContext(actorHandler, Font("Arial", 0.6.cm.toPixels.toInt), Colors.scheme, margins)
	}
	
	object Lengths
	{
		def field = FieldLengths
		
		object FieldLengths
		{
			/**
			 * A medium-sized field length
			 */
			val medium = 6.cm.toPixels.toInt
		}
	}
}
