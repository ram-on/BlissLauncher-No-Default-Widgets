package foundation.e.blisslauncher.common.util

import java.text.Collator

class LabelComparator : Comparator<String> {

    private val collator = Collator.getInstance()
    override fun compare(titleA: String, titleB: String): Int {

        // Ensure that we de-prioritize any titles that don't start with a
        // linguistic letter or digit
        val aStartsWithLetter = titleA.isNotEmpty() &&
            Character.isLetterOrDigit(titleA.codePointAt(0))
        val bStartsWithLetter = titleB.isNotEmpty() &&
            Character.isLetterOrDigit(titleB.codePointAt(0))
        if (aStartsWithLetter && !bStartsWithLetter) {
            return -1
        } else if (!aStartsWithLetter && bStartsWithLetter) {
            return 1
        }

        // Order by the title in the current locale
        return collator.compare(titleA, titleB)
    }
}