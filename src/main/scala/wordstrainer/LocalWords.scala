package wordstrainer

import java.io.FileNotFoundException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ThreadLocalRandom

import wordstrainer.LocalWords._

import scala.collection.mutable.ArrayBuffer

private object LocalWords {

  def apply(dataDir: String) = new LocalWords(dataDir)

  private def saveWordPair(
                            wordsFile: WordsFile,
                            metaFile: MetaFile,
                            wordPair: (String, String)
                          ): Unit = {
    val wordPairOffset = wordsFile.getFilePointer()
    wordsFile.write(wordPair._1)
    wordsFile.write(wordPair._2)

    metaFile.write(new MetaFile.Record(wordPairOffset))
  }

  private def getNextTrainTime(successes: Byte, lastSuccessTime: Long): Long =
    // TODO: store lastSuccessTime in days instead of millis, that will require short instead of long
    Instant
      .ofEpochMilli(lastSuccessTime)
      .plus(Math.pow(successes, 1.4).round, ChronoUnit.DAYS)
      .truncatedTo(ChronoUnit.DAYS)
      .toEpochMilli()

  private def newWordToTrain(
      metaRecord: MetaFile.Record,
      metaRecordIndex: Int,
      wordsFile: WordsFile,
      trainingType: TrainingType.TrainingType
  ): WordToTrain = {
    wordsFile.seek(metaRecord.wordsFileOffset)
    val word = wordsFile.read()
    val trans = wordsFile.read()
    if (trainingType == TrainingType.WordTrans) {
      WordToTrain(word, trans, metaRecordIndex, reverse = false)
    } else {
      WordToTrain(trans, word, metaRecordIndex, reverse = true)
    }
  }

  case class TrainingData(
      totalToTrain: Int,
      totalTrained: Int,
      wordsToTrain: collection.Seq[WordToTrain]
  )

  case class WordToTrain(
      question: String,
      answer: String,
      metaRecordIndex: Int,
      reverse: Boolean
  )

  private object TrainingType extends Enumeration {
    type TrainingType = Value
    val DoNotTrain, WordTrans, TransWord = Value
  }

}

private class LocalWords private (dataDir: String) {

  def getLastPair(): Option[(String, String)] = {
    val lastWordPairOffset = MetaFile.getLastWordPairOffset(dataDir)
    if (lastWordPairOffset.isEmpty) {
      return Option.empty
    }

    val wordsFile = WordsFile(dataDir)
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

    val wordsFile = WordsFile(dataDir)
    try {
      wordsFile.seekEnd()

      val metaFile = MetaFile(dataDir)
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

  def getTrainingData(): TrainingData = {
    var totalToTrain = 0
    var totalTrained = 0
    val wordsToTrain = new ArrayBuffer[WordToTrain](10)
    val metaFile =
      try {
        MetaFile(dataDir)
      } catch {
        case _: FileNotFoundException =>
          return TrainingData(totalToTrain, totalTrained, wordsToTrain)
      }
    try {
      val wordsFile = WordsFile(dataDir)
      try {
        var metaRecordIdx = 0
        val curTime = System.currentTimeMillis()
        for (metaRecord <- metaFile) {

          val nextWordTransTime = getNextTrainTime(
            metaRecord.wordTransSuccesses,
            metaRecord.wordTransLastTime
          )
          val trainWordTrans =
            if (nextWordTransTime <= curTime) {
              totalToTrain += 1
              true
            } else {
              totalTrained += 1
              false
            }

          val nextTransWordTime = getNextTrainTime(
            metaRecord.transWordSuccesses,
            metaRecord.transWordLastTime
          )
          val trainTransWord =
            if (nextTransWordTime <= curTime) {
              totalToTrain += 1
              true
            } else {
              totalTrained += 1
              false
            }

          val trainingType =
            if (trainWordTrans && trainTransWord) {
              if (ThreadLocalRandom.current().nextBoolean()) {
                TrainingType.WordTrans
              } else {
                TrainingType.TransWord
              }
            } else if (trainWordTrans) {
              TrainingType.WordTrans
            } else if (trainTransWord) {
              TrainingType.TransWord
            } else {
              TrainingType.DoNotTrain
            }

          if (
            trainingType != TrainingType.DoNotTrain && wordsToTrain.length < 10
          ) {
            wordsToTrain += newWordToTrain(
              metaRecord,
              metaRecordIdx,
              wordsFile,
              trainingType
            )
          }

          metaRecordIdx += 1
        }
      } finally {
        wordsFile.close()
      }
    } finally {
      metaFile.close()
    }
    TrainingData(totalToTrain, totalTrained, wordsToTrain)
  }

  def saveAnswers(
      trainedWords: collection.Seq[LocalWords.WordToTrain],
      answers: Array[Boolean]
  ): Unit = {
    val metaFile = MetaFile(dataDir)
    try {
      var i = 0
      for (w <- trainedWords) {
        metaFile.seekRecord(w.metaRecordIndex)
        if (answers(i)) {
          metaFile.inc(w.reverse)
        } else {
          metaFile.resetRecord()
        }
        i += 1
      }
    } finally {
      metaFile.close()
    }
  }

}
