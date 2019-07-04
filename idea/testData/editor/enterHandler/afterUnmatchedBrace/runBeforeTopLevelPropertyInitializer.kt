val test = run {<caret>foo()

fun foo() = 42
//-----
val test = run {
    <caret>foo()
}

fun foo() = 42