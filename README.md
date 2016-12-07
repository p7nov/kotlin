# Kotlin-native backend #

Download dependencies:

	./gradlew dependencies:update

Then build the compiler:

	./gradlew dist

After that you should be able to compile your programs like that:

	./dist/bin/konanc hello.kt -o hello

For an optimized compilation use -opt:

	./dist/bin/konanc hello.kt -o hello -opt

For more tests, use:

	./gradlew backend.native:tests:run
