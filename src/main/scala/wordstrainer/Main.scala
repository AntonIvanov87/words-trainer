package wordstrainer

private object Main {

  def main(args: Array[String]): Unit = {

    val lastLocalPair = LocalWords.getLastPair()
    // TODO: do it async
    val newPairs = GoogleSavedWords.getNew(lastLocalPair)
    LocalWords.saveNewPairs(newPairs)
    println("Got " + newPairs.length + " new words")

    val trainingData = LocalWords.getTrainingData()
    println(trainingData.totalToTrain + " words to train")
    println(trainingData.totalTrained + " words trained")

    val answers = Trainer.train(trainingData.wordsToTrain)

    saveAnswers(trainingData.wordsToTrain, answers)
  }

  private def saveAnswers(trainedWords: collection.Seq[LocalWords.WordToTrain], answers: Array[Boolean]): Unit = {
    val metaFile = new MetaFile
    try {
      var i = 0
      for (w <- trainedWords) {
        metaFile.seekRecord(w.metaRecordIndex)
        if (answers(i)) {
          metaFile.inc(w.reverse)
        } else {
          metaFile.resetRecord()
        }
        i+=1
      }
    } finally {
      metaFile.close()
    }
  }
}
