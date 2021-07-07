package wordstrainer

import wordstrainer.google.GoogleSavedWords
import wordstrainer.local.LocalWords

private object Main {

  def main(args: Array[String]): Unit = {

    val settings = Settings()

    // TODO: do it async
    val pairsFromGoogle = GoogleSavedWords.get(settings.googleSecrets)

    val localWords = new LocalWords(settings.dataDir)
    try {
      localWords.saveNewPairs(pairsFromGoogle)

      val trainingData = localWords.getTrainingData
      println(s"${trainingData.totalToTrain} words to train")
      println(s"${trainingData.totalTrained} words trained")

      val answers = Trainer.train(trainingData.wordsToTrain)

      localWords.saveAnswers(trainingData.wordsToTrain, answers)
    } finally {
      localWords.close()
    }

  }
}
