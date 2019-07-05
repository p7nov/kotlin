/*
 * KOTLIN PSI SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 2 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: Empty 'when' with bound value.
 */

fun case_1() {
    val x: @Foo () -> Unit get() = {}
    val x: (@Foo () -> Unit) get() = {}
    val x: @Foo (() -> Unit) get() = {}
    val x: @Foo.Bar () -> Unit get() = {}
    val x: @Foo () -> () -> Unit get() = {}
    val x: @Foo ()->()->Unit get() = {}
    val x: @Foo @Bar () -> Unit get() = {}
    val x: @Foo(10) @Bar () -> Unit get() = {}
    val x: @Foo @Bar(10) @Foo () -> Unit get() = {}
}

fun case_2() {
    val x: @Foo (Int) -> Unit = {}
    val x: (@Foo (x: @Foo (@Foo () -> Int) -> Int) -> Unit) = {}
    val x: @Foo (x: kotlin.Any) -> Unit = {}
    val x: @Foo.Bar (x: kotlin.Any = {}) -> Unit = {}
    val x: @Foo (x: @Foo Foo) -> (y: @Foo Bar) -> Unit = {}
    val x: @Foo (x: @Foo kotlin.Any = {})->()->Unit = {}
    val x: @Foo @Bar (kotlin.Any) -> Unit = {}
    val x: @Foo(10) @Bar (Coomparable<kotlin.Any>) -> Unit = {}
    val x: @Foo @Bar(10) @Foo (Coomparable<@Foo @Bar(10) @Foo () -> Unit>) -> Unit = {}
}
