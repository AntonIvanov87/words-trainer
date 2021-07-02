load("@io_bazel_rules_scala//scala:scala.bzl", "scala_binary", "scala_library")

scala_library(
    name = "words-trainer-lib",
    srcs = glob(["src/main/scala/wordstrainer/*.scala"]),
    data = ["settings.properties"],
)

scala_binary(
    name = "words-trainer",
    main_class = "wordstrainer.Main",
    unused_dependency_checker_mode = "error",
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
