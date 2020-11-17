package wordstrainer

import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit

import scala.collection.mutable.ArrayBuffer

private object LocalWords {

  def getLastPair(): Option[(String, String)] = {
    val lastWordPairOffset = MetaFile.getLastWordPairOffset()
    if (lastWordPairOffset.isEmpty) {
      return Option.empty
    }

    val wordsFile = new WordsFile
    try {
      wordsFile.seek(lastWordPairOffset.get)
      val word = wordsFile.read()
      val translation = wordsFile.read()
      Some(word, translation)

    } finally {
      wordsFile.close()
    }
  }

  def saveNewPairs(newPairs: collection.Seq[(String, String)]): Unit = {
    if (newPairs.isEmpty) {
      return
    }

    val wordsFile = new WordsFile
    try {
      wordsFile.seekEnd()

      val metaFile = new MetaFile
      try {
        metaFile.seekEnd()

        for (wordPair <- newPairs) {
          saveWordPair(wordsFile, metaFile, wordPair)
        }

      } finally {
        metaFile.close()
      }

    } finally {
      wordsFile.close()
    }
  }

  private def saveWordPair(wordsFile: WordsFile, metaFile: MetaFile, wordPair: (String, String)) {
    val wordPairOffset = wordsFile.getFilePointer()
    wordsFile.write(wordPair._1)
    wordsFile.write(wordPair._2)

    metaFile.write(new MetaFile.Record(wordPairOffset))
  }

  def getTrainingData(): TrainingData = {
    var totalToTrain = 0
    var totalTrained = 0
    val wordsToTrain = new ArrayBuffer[WordToTrain](10)
    val metaFile = try {
      new MetaFile()
    } catch {
      case _: FileNotFoundException => return TrainingData(totalToTrain, totalTrained, wordsToTrain)
    }
    try {
      val wordsFile = new WordsFile()
      try {
        var i = 0
        for (metaRecord <- metaFile) {
          val trainingType = getTrainingType(metaRecord)
          if (trainingType != TrainingType.DoNotTrain) {
            if (wordsToTrain.length < 10) {
              wordsToTrain.addOne(newWordToTrain(metaRecord, i, wordsFile, trainingType))
            }
            totalToTrain += 1
          } else {
            totalTrained += 1
          }
          i += 1
        }
      } finally {
        wordsFile.close()
      }
    } finally {
      metaFile.close()
    }
    TrainingData(totalToTrain, totalTrained, wordsToTrain)
  }

  case class TrainingData(totalToTrain: Int, totalTrained: Int, wordsToTrain: collection.Seq[WordToTrain])

  case class WordToTrain(question: String, answer: String, metaRecordIndex: Int, reverse: Boolean)

  private object TrainingType extends Enumeration {
    type TrainingType = Value
    val DoNotTrain, WordTrans, TransWord = Value
  }

  private def getTrainingType(metaRecord: MetaFile.Record): TrainingType.TrainingType = {
    val curTime = System.currentTimeMillis()
    val nextWordTransTime = getNextTrainTime(metaRecord.wordTransSuccesses, metaRecord.wordTransLastTime)
    val nextTransWordTime = getNextTrainTime(metaRecord.transWordSuccesses, metaRecord.transWordLastTime)

    if (nextWordTransTime <= nextTransWordTime && nextWordTransTime <= curTime) {
      TrainingType.WordTrans
    } else if (nextTransWordTime <= nextWordTransTime && nextTransWordTime <= curTime) {
      TrainingType.TransWord
    } else {
      TrainingType.DoNotTrain
    }
  }

  private def newWordToTrain(metaRecord: MetaFile.Record,
                            metaRecordIndex: Int,
                            wordsFile: WordsFile,
                            trainingType: TrainingType.TrainingType): WordToTrain = {
      wordsFile.seek(metaRecord.wordsFileOffset)
      val word = wordsFile.read()
      val trans = wordsFile.read()
      if (trainingType == TrainingType.WordTrans) {
        WordToTrain(word, trans, metaRecordIndex, reverse = false)
      } else {
        WordToTrain(trans, word, metaRecordIndex, reverse = true)
      }
  }

  private def getNextTrainTime(successes: Byte, lastSuccessTime: Long): Long =
    lastSuccessTime + TimeUnit.DAYS.toMillis(successes)
}
