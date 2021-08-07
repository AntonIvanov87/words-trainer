load("@io_bazel_rules_scala//scala:scala.bzl", "scala_binary", "scala_library")

scala_library(
    name = "words-trainer-lib",
    srcs = glob(["src/main/scala/wordstrainer/**/*.scala"]),
    data = ["settings.properties"],
)

java_binary(
    name = "words-trainer",
    srcs = ["src/main/java/module-info.java"],
    javacopts = ["--release 11"],
    main_class = "wordstrainer.Main",
    deps = ["words-trainer-lib"],
)

scala_binary(
    name = "print-local",
    main_class = "wordstrainer.PrintLocalWordsMain",
    unused_dependency_checker_mode = "error",
    deps = ["words-trainer-lib"],
)

scala_binary(
    name = "remove-duplicates",
    main_class = "wordstrainer.RemoveDuplicatesMain",
    unused_dependency_checker_mode = "error",
    deps = ["words-trainer-lib"],
)
