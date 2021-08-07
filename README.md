To run:
1) Copy [example-settings.properties](example-settings.properties) and follow instructions inside the file
2) Install [Bazel](https://bazel.build/) build tool 
3) Run:
    ```shell
    bazel run words-trainer
    ```
    Alternatively build a java image:
    ```shell
    bazel build words-trainer_deploy.jar
    jlink --module-path bazel-bin/words-trainer_deploy.jar --add-modules wordstrainer --launcher wordstrainer=wordstrainer/wordstrainer.Main --output $IMAGE_DIR
    ```
    Run it:
    ```shell
    $IMAGE_DIR/bin/wordstrainer
    ```
