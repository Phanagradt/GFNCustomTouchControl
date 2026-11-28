package com.example.input

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * Pure geometry & scaling helpers for XenosPad trackpad camera movement.
 *
 * Solves the "hits screen edge / dead zone faster at high sensitivity" problem by:
 * 1. Allowing wide sensitivity scaling (0.4x .. 8.0x) instead of clamping early.
 * 2. Anchoring Accessibility swipe strokes near the edge *opposite* the swipe direction
 *    so each single stroke gets the full width/height of the safe pan zone.
 * 3. Splitting large high-sensitivity flicks into chained consecutive strokes so the full
 *    requested camera rotation is delivered instead of getting truncated at the screen border.
 * 4. Preserving overflow deltas during continuous panning when recentering at a zone edge.
 */
object TouchpadSwipePlanner {

    const val MIN_SENSITIVITY = 0.4f
    const val MAX_SENSITIVITY = 8.0f

    data class PanZone(
        val anchorX: Float,
        val anchorY: Float,
        val minX: Float,
        val maxX: Float,
        val minY: Float,
        val maxY: Float
    ) {
        val width: Float get() = (maxX - minX).coerceAtLeast(1f)
        val height: Float get() = (maxY - minY).coerceAtLeast(1f)
    }

    data class SwipeSegment(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val durationMs: Long
    ) {
        val dx: Float get() = endX - startX
        val dy: Float get() = endY - startY
    }

    data class ContinuousPanStep(
        val endX: Float,
        val endY: Float,
        val hitEdge: Boolean,
        val overflowDx: Float,
        val overflowDy: Float,
        val nextAnchorX: Float,
        val nextAnchorY: Float
    )

    fun scaleTouchpadDelta(rawPx: Float, sensitivity: Float): Float =
        rawPx * sensitivity.coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY)

    /**
     * Picks an in-bounds starting anchor biased toward the edge opposite the travel vector
     * so a single stroke has maximum runway before reaching the opposite boundary.
     */
    fun optimalStartAnchor(zone: PanZone, dxPx: Float, dyPx: Float): Pair<Float, Float> {
        val padX = (zone.width * 0.08f).coerceAtMost(24f)
        val padY = (zone.height * 0.08f).coerceAtMost(24f)
        val halfW = zone.width * 0.45f
        val halfH = zone.height * 0.45f

        val startX = when {
            dxPx > halfW -> (zone.minX + padX).coerceAtMost(zone.maxX)
            dxPx < -halfW -> (zone.maxX - padX).coerceAtLeast(zone.minX)
            else -> zone.anchorX.coerceIn(zone.minX, zone.maxX)
        }
        val startY = when {
            dyPx > halfH -> (zone.minY + padY).coerceAtMost(zone.maxY)
            dyPx < -halfH -> (zone.maxY - padY).coerceAtLeast(zone.minY)
            else -> zone.anchorY.coerceIn(zone.minY, zone.maxY)
        }
        return startX to startY
    }

    /**
     * Breaks an arbitrary camera flick (dxPx, dyPx) into 1..[maxSegments] in-bounds screen strokes
     * whose summed (dx, dy) delivers the full turn angle while preserving diagonal direction.
     */
    fun planFlickSegments(
        zone: PanZone,
        dxPx: Float,
        dyPx: Float,
        maxSegments: Int = 6
    ): List<SwipeSegment> {
        if (hypot(dxPx, dyPx) < 1f || maxSegments <= 0) return emptyList()

        val segments = mutableListOf<SwipeSegment>()
        var remDx = dxPx
        var remDy = dyPx

        repeat(maxSegments) {
            if (hypot(remDx, remDy) < 1f) return segments

            val (startX, startY) = optimalStartAnchor(zone, remDx, remDy)
            val availX = when {
                remDx > 0f -> (zone.maxX - startX).coerceAtLeast(1f)
                remDx < 0f -> (startX - zone.minX).coerceAtLeast(1f)
                else -> Float.POSITIVE_INFINITY
            }
            val availY = when {
                remDy > 0f -> (zone.maxY - startY).coerceAtLeast(1f)
                remDy < 0f -> (startY - zone.minY).coerceAtLeast(1f)
                else -> Float.POSITIVE_INFINITY
            }

            val tx = if (abs(remDx) > 0.01f) availX / abs(remDx) else 1f
            val ty = if (abs(remDy) > 0.01f) availY / abs(remDy) else 1f
            val t = min(1f, min(tx, ty)).coerceIn(0f, 1f)
            if (t <= 0.001f) return segments

            val segDx = remDx * t
            val segDy = remDy * t
            val endX = (startX + segDx).coerceIn(zone.minX, zone.maxX)
            val endY = (startY + segDy).coerceIn(zone.minY, zone.maxY)
            val segDist = hypot(endX - startX, endY - startY)
            if (segDist < 1f) return segments

            val durationMs = (segDist * 0.16f).toLong().coerceIn(38L, 115L)
            segments.add(
                SwipeSegment(
                    startX = startX,
                    startY = startY,
                    endX = endX,
                    endY = endY,
                    durationMs = durationMs
                )
            )

            remDx -= (endX - startX)
            remDy -= (endY - startY)
        }

        return segments
    }

    /**
     * Advances an active continuous pan stroke inside [zone] without discarding overshoot.
     * When an edge is reached, returns exact remaining overflow deltas and an opposite-side
     * re-anchor so high-sensitivity 360° camera panning never stalls at a wall.
     */
    fun planContinuousPanStep(
        zone: PanZone,
        cursorX: Float,
        cursorY: Float,
        dxPx: Float,
        dyPx: Float
    ): ContinuousPanStep {
        val rawNextX = cursorX + dxPx
        val rawNextY = cursorY + dyPx

        val inside = rawNextX > zone.minX && rawNextX < zone.maxX &&
            rawNextY > zone.minY && rawNextY < zone.maxY

        if (inside) {
            return ContinuousPanStep(
                endX = rawNextX,
                endY = rawNextY,
                hitEdge = false,
                overflowDx = 0f,
                overflowDy = 0f,
                nextAnchorX = rawNextX,
                nextAnchorY = rawNextY
            )
        }

        // Compute parametric intersection t along the ray so diagonal movement stays accurate
        val tx = when {
            dxPx > 0.001f -> ((zone.maxX - cursorX) / dxPx).coerceIn(0f, 1f)
            dxPx < -0.001f -> ((zone.minX - cursorX) / dxPx).coerceIn(0f, 1f)
            else -> 1f
        }
        val ty = when {
            dyPx > 0.001f -> ((zone.maxY - cursorY) / dyPx).coerceIn(0f, 1f)
            dyPx < -0.001f -> ((zone.minY - cursorY) / dyPx).coerceIn(0f, 1f)
            else -> 1f
        }
        val t = min(tx, ty).coerceIn(0f, 1f)

        val endX = (cursorX + dxPx * t).coerceIn(zone.minX, zone.maxX)
        val endY = (cursorY + dyPx * t).coerceIn(zone.minY, zone.maxY)
        val consumedDx = endX - cursorX
        val consumedDy = endY - cursorY
        val overflowDx = dxPx - consumedDx
        val overflowDy = dyPx - consumedDy

        val padX = (zone.width * 0.08f).coerceAtMost(24f)
        val padY = (zone.height * 0.08f).coerceAtMost(24f)
        val reAnchorX = when {
            rawNextX >= zone.maxX -> (zone.minX + padX).coerceAtMost(zone.maxX)
            rawNextX <= zone.minX -> (zone.maxX - padX).coerceAtLeast(zone.minX)
            else -> endX
        }
        val reAnchorY = when {
            rawNextY >= zone.maxY -> (zone.minY + padY).coerceAtMost(zone.maxY)
            rawNextY <= zone.minY -> (zone.maxY - padY).coerceAtLeast(zone.minY)
            else -> endY
        }
        return ContinuousPanStep(
            endX = endX,
            endY = endY,
            hitEdge = true,
            overflowDx = overflowDx,
            overflowDy = overflowDy,
            nextAnchorX = reAnchorX,
            nextAnchorY = reAnchorY
        )
    }
}
