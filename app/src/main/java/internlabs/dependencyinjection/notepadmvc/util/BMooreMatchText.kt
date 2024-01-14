package internlabs.dependencyinjection.notepadmvc.util

class BMooreMatchText {
    companion object {
        private fun preprocessStrongSuffix(
            shift: IntArray,
            bpos: IntArray,
            pat: CharArray,
            m: Int
        ) {
            var i = m
            var j = m + 1
            bpos[i] = j
            while (i > 0) {
                while (j <= m && pat[i - 1] != pat[j - 1]) {
                    if (shift[j] == 0) shift[j] = j - i
                    j = bpos[j]
                }
                i--
                j--
                bpos[i] = j
            }
        }

        private fun preprocessCase2(shift: IntArray, bpos: IntArray, m: Int) {
            var i: Int
            var j: Int
            j = bpos[0]
            i = 0
            while (i <= m) {
                if (shift[i] == 0) shift[i] = j
                if (i == j) j = bpos[j]
                i++
            }
        }

        fun search(text: CharArray, pat: CharArray): ArrayList<Int> {
            val answer: ArrayList<Int> = ArrayList()
            var s = 0
            var j: Int
            val m = pat.size
            val n = text.size
            val bpos = IntArray(m + 1)
            val shift = IntArray(m + 1)
            for (i in 0 until m + 1) shift[i] = 0
            preprocessStrongSuffix(shift, bpos, pat, m)
            preprocessCase2(shift, bpos, m)
            while (s <= n - m) {
                j = m - 1
                while (j >= 0 && pat[j] == text[s + j]) j--
                s += if (j < 0) {
                    answer.add(s)
                    shift[0]
                } else shift[j + 1]
            }
            return answer
        }
    }
}