fun test() {
    val test = run {<caret>1

    fun foo() = 42
}
//-----
fun test() {
    val test = run {
        <caret>1

        fun foo() = 42
    }
}