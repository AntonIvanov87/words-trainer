package wordstrainer.local

import wordstrainer.local.LocalWords._

import java.io.FileNotFoundException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

private[wordstrainer] object LocalWords {

  def apply(dataDir: String) = new LocalWords(dataDir)

  def toString(
      metaRecord: MetaFile.Record,
      word: String,
      trans: String
  ): String = {
    s"${metaRecord.wordsFileOffset} $word $trans " +
      s"${metaRecord.wordTransSuccesses} ${Instant.ofEpochMilli(metaRecord.wordTransLastTime)} " +
      s"${metaRecord.transWordSuccesses} ${Instant.ofEpochMilli(metaRecord.transWordLastTime)}"
  }

  private def saveWordPair(
      wordsFile: WordsFile,
      metaFile: MetaFile,
      wordPair: (String, String)
  ): Unit = {
    val wordPairOffset = wordsFile.getFilePointer
    wordsFile += wordPair

    metaFile.addOne(new MetaFile.Record(wordPairOffset))
  }

  private def getNextTrainTime(successes: Byte, lastSuccessTime: Long): Long =
    // TODO: store lastSuccessTime in days instead of millis, that will require short instead of long
    Instant
      .ofEpochMilli(lastSuccessTime)
      .plus(Math.pow(successes, 1.4).round, ChronoUnit.DAYS)
      .truncatedTo(ChronoUnit.DAYS)
      .toEpochMilli

  private def newWordToTrain(
      metaRecord: MetaFile.Record,
      metaRecordIndex: Int,
      wordsFile: WordsFile,
      trainingType: TrainingType.TrainingType
  ): WordToTrain = {
    val (word, trans) = wordsFile.readAt(metaRecord.wordsFileOffset)
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

  private def remove(
      externalWordToTranslations: mutable.Map[String, mutable.Set[String]],
      word: String,
      trans: String
  ): Unit = {
    externalWordToTranslations.get(word).foreach { translations =>
      if (translations.remove(trans) && translations.isEmpty) {
        externalWordToTranslations.remove(word)
      }
    }
  }

}

private[wordstrainer] class LocalWords private (dataDir: String) {

  def saveNewPairs(externalPairs: collection.Seq[(String, String)]): Unit = {
    val externalWordToTrans =
      externalPairs.foldLeft(mutable.Map[String, mutable.Set[String]]()) {
        (map, pair) =>
          val translations = map.getOrElseUpdate(pair._1, mutable.Set())
          translations += pair._2
          map
      }

    val wordsFile = WordsFile(dataDir)
    try {
      val metaFile = MetaFile(dataDir)
      try {
        for (metaRecord <- metaFile) {
          val localPair = wordsFile.readAt(metaRecord.wordsFileOffset)
          remove(externalWordToTrans, localPair._1, localPair._2)
          remove(externalWordToTrans, localPair._2, localPair._1)
        }

        wordsFile.seekEnd()
        externalWordToTrans.foreachEntry { (word, translations) =>
          translations.foreach { trans =>
            println(s"New: $word $trans")
            saveWordPair(wordsFile, metaFile, (word, trans))
          }
        }

      } finally {
        metaFile.close()
      }

    } finally {
      wordsFile.close()
    }
  }

  def getTrainingData: TrainingData = {
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
        if (answers(i)) {
          metaFile.inc(w.metaRecordIndex, w.reverse)
        } else {
          metaFile.resetRecord(w.metaRecordIndex)
        }
        i += 1
      }
    } finally {
      metaFile.close()
    }
  }

}
