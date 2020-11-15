package wordstrainer

private object Main {

  def main(args: Array[String]): Unit = {
    val allPairs = GoogleSavedWords.get()

    val lastLocalPair = LocalWords.getLastPair()
    val newPairs = getNewPairs(allPairs, lastLocalPair)
    LocalWords.saveNewPairs(newPairs)
    println("Got " + newPairs.length + " new words")

    val wordsToTrain = LocalWords.getWordsToTrain()

    val answers = Trainer.train(wordsToTrain)

    saveAnswers(wordsToTrain, answers)
  }

  private def getNewPairs(allPairs: Array[(String, String)], lastLocalPair: Option[(String, String)]): Array[(String, String)] = {
    if (lastLocalPair.isDefined) {
      for (i <- allPairs.indices.reverse) {
        if (allPairs(i) == lastLocalPair.get) {
          return allPairs.drop(i+1)
        }
      }
    }
    allPairs
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
