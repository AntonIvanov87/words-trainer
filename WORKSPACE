load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# SCALA

rules_scala_version="bc4896727a40e89d6b92e267c12a28964bc9a76b"
http_archive(
    name = "io_bazel_rules_scala",
    strip_prefix = "rules_scala-%s" % rules_scala_version,
    type = "zip",
    url = "https://github.com/bazelbuild/rules_scala/archive/%s.zip" % rules_scala_version,
    sha256 = "db147ab5abfe4380a441daea4420922b5416fccd70092604b6acec5262b0ff72"
)

http_archive(
    name = "bazel_skylib",
    urls = [
        "https://github.com/bazelbuild/bazel-skylib/releases/download/1.0.3/bazel-skylib-1.0.3.tar.gz",
        "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/1.0.3/bazel-skylib-1.0.3.tar.gz",
    ],
    sha256 = "1c531376ac7e5a180e0237938a2536de0c54d93f5c278634818e0efc952dd56c",
)

# Stores Scala version and other configuration
load("@io_bazel_rules_scala//:scala_config.bzl", "scala_config")
scala_config(
    scala_version = "2.12.12"
)

load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")
scala_register_toolchains()

load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")
scala_repositories()
