package wordstrainer

import wordstrainer.google.GoogleSavedWords
import wordstrainer.local.LocalWords

private object Main {

  def main(args: Array[String]): Unit = {

    val settings = Settings()

    val localWords = LocalWords(settings.dataDir)
    val lastLocalPair = localWords.getLastPair

    // TODO: do it async
    val newPairs =
      GoogleSavedWords.getNew(lastLocalPair, settings.googleSecrets)
    localWords.saveNewPairs(newPairs)
    println("Got " + newPairs.length + " new translations")

    val trainingData = localWords.getTrainingData
    println(s"${trainingData.totalToTrain} words to train")
    println(s"${trainingData.totalTrained} words trained")

    val answers = Trainer.train(trainingData.wordsToTrain)

    localWords.saveAnswers(trainingData.wordsToTrain, answers)

  }
}
