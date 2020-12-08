load("@io_bazel_rules_scala//scala:scala.bzl", "scala_binary")

scala_binary(
    name = "words-trainer",
    srcs = glob(["src/main/scala/wordstrainer/*.scala"]),
    data = ["settings.properties"],
    main_class = "wordstrainer.Main",
    unused_dependency_checker_mode = "error"
)

# TODO: add PrintLocalWordsMain
