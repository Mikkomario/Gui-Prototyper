package vf.prototyper.util

import utopia.firmament.context.{BaseContext, ScrollingContext}
import utopia.firmament.localization.{Localizer, NoLocalization}
import utopia.firmament.model.Margins
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.immutable.Pair
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.TryCatch
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.genesis.handling.action.{ActionLoop, ActorHandler}
import utopia.genesis.handling.event.keyboard.KeyboardEvents
import utopia.genesis.text.Font
import utopia.genesis.util.Screen
import utopia.paradigm.color.{ColorScheme, ColorSet}
import utopia.paradigm.measurement.DistanceExtensions._
import utopia.paradigm.measurement.Ppi
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.input.selection.DropDown
import utopia.reach.context.ReachContentWindowContext
import utopia.reach.cursor.{CursorSet, CursorType}
import vf.prototyper.view.Icon

import java.nio.file.Paths
import scala.concurrent.ExecutionContext

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
	implicit val exc: ExecutionContext = new ThreadPool("GUI-Prototyper")
	/**
	 * Pixels per inch of the local screen
	 */
	implicit val ppi: Ppi = Screen.ppi
	/**
	 * The root actor handler used
	 */
	val actorHandler = ActorHandler()
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
	
	private val actorLoop = new ActionLoop(actorHandler)
	
	/**
	 * The standard margins used
	 */
	val margins = Margins((0.25.cm.toPixels / 2.0).toInt * 2)
	
	/**
	 * The cursors used
	 */
	val cursors = {
		// Loads the cursors from icon files. Logs failures.
		val cursorDirectory = directory.data/"cursors"
		import CursorType._
		CursorSet.loadIcons(
			Map[CursorType, (String, Point)](
				(Default, ("cursor-arrow", Point(7, 4))),
				(Interactive, ("cursor-hand", Point(9, 1))),
				(Text, ("cursor-text", Point(12, 12)))
			).view.mapValues { case (name, origin) =>
				cursorDirectory/name.endingWith(".png") -> origin
			}.toMap, drawEdgesFor = Set(Default, Interactive)) match {
				case TryCatch.Success(cursors, failures) =>
					failures.headOption.foreach { error => log(error, s"Failed to load ${ failures.size } cursors") }
					Some(cursors)
				case TryCatch.Failure(error) =>
					log(error, "Failed to load cursors")
					None
			}
	}
	
	
	// INITIAL CODE ------------------------
	
	actorLoop.runAsync()
	
	KeyboardEvents.specifyExecutionContext(exc)
	KeyboardEvents.setupKeyDownEvents(actorHandler)
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @return Access point to different directories
	 */
	def directory = Directories
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
	/**
	 * @return Access point to various pre-initialized component setups
	 */
	def component = Components
	
	
	// NESTED   ----------------------------
	
	object Directories
	{
		/**
		 * Directory that stores all project data
		 */
		val data = Paths.get("data")
		/**
		 * Directory that stores the view documents
		 */
		val views = data/"views"
		/**
		 * Directory that stores project files
		 */
		val projects = data/"projects"
	}
	
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
		val scheme = ColorScheme.default ++ ColorScheme.twoTone(primary, secondary)
		
		
		// COMPUTED --------------------------
		
		/**
		 * The gray color set
		 */
		def gray = scheme.gray
	}
	
	object Context
	{
		/**
		 * The basic component creation context
		 */
		val base = BaseContext(actorHandler, Font("Arial", 0.6.cm.toPixels.toInt), Colors.scheme, margins)
		
		/**
		 * Component creation context for windows
		 */
		implicit val window: ReachContentWindowContext =
			ReachContentWindowContext(base.against(Colors.gray.light).forTextComponents)
				.withWindowBordersEnabled(enabled = true).withResizeLogic(Program)
				.withCursors(cursors)
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
	
	object Components
	{
		/**
		 * A drop-down component setup with the correct arrow icons
		 */
		lazy val dropDown = DropDown.withExpandAndCollapseIcon(Pair(Icon.arrowDown, Icon.arrowUp).map { _.small })
	}
}
