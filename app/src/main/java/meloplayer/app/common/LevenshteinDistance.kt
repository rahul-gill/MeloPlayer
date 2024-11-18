package meloplayer.app.common

import kotlin.math.exp
import kotlin.math.min

const val LevenshteinDistanceThreshold = 0.5

/**
 * Finds the Levenshtein distance between two Strings.
 * https://github.com/apache/commons-text/blob/master/src/main/java/org/apache/commons/text/similarity/LevenshteinDistance.java#L259
 * @param left the first CharSequence, must not be null
 * @param right the second CharSequence, must not be null
 * @return result distance between 0 and 1
 */
fun levenshteinDistanceNormalized(leftArg: CharSequence, rightArg: CharSequence): Double {
    var left: CharSequence = leftArg
    var right: CharSequence = rightArg

    /*
       This implementation use two variable to record the previous cost counts,
       So this implementation use less memory than previous impl.
     */
    var n = left.length // length of left
    var m = right.length // length of right

    if (n == 0) {
        return 1.0
    }
    if (m == 0) {
        return 1.0
    }

    if (n > m) {
        // swap the input strings to consume less memory
        val tmp: CharSequence = left
        left = right
        right = tmp
        n = m
        m = right.length
    }

    val p = IntArray(n + 1)

    // indexes into strings left and right
    var i: Int // iterates through left
    var upperLeft: Int
    var upper: Int

    var rightJ: Char // jth character of right
    var cost: Int // cost

    i = 0
    while (i <= n) {
        p[i] = i
        i++
    }

    var j = 1 // iterates through right
    while (j <= m) {
        upperLeft = p[0]
        rightJ = right[j - 1]
        p[0] = j

        i = 1
        while (i <= n) {
            upper = p[i]
            cost = if (left[i - 1] == rightJ) 0 else 1
            // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
            p[i] = min(
                min((p[i - 1] + 1).toDouble(), (p[i] + 1).toDouble()),
                (upperLeft + cost).toDouble()
            )
                .toInt()
            upperLeft = upper
            i++
        }
        j++
    }

    val distance = p[n]
    return 1 / exp(distance.toDouble() / (min(m, n) - distance))
}