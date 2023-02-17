package vf.prototyper.model.immutable

import utopia.paradigm.shape.shape2d.Bounds

/**
 * Represents a link to another view
 * @author Mikko Hilpinen
 * @since 15.2.2023, v0.1
 * @param area Area from which this link is triggered.
 *             (0,0) is top-left corner of the parent view. (1,1) is the bottom right corner.
 * @param targetId Id of the referred view
 */
case class Link(area: Bounds, targetId: Int)
