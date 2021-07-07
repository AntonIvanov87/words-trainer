package wordstrainer.local

import wordstrainer.local.LocalWords._

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

private[wordstrainer] object LocalWords {

  case class PairWithTrainingData private (
      word: String,
      trans: String,
      wordTransSuccesses: Byte,
      wordTransLastTime: Long,
      transWordSuccesses: Byte,
      transWordLastTime: Long,
      private val wordsFileOffset: Long
  ) {
    override def toString: String = {
      s"$word $trans " +
        s"$wordTransSuccesses ${Instant.ofEpochMilli(wordTransLastTime)} " +
        s"$transWordSuccesses ${Instant.ofEpochMilli(transWordLastTime)} " +
        s"$wordsFileOffset"
    }
  }

  private object PairWithTrainingData {
    def apply(
        word: String,
        trans: String,
        metaRecord: MetaFile.Record
    ): PairWithTrainingData = {
      PairWithTrainingData(
        word,
        trans,
        metaRecord.wordTransSuccesses,
        metaRecord.wordTransLastTime,
        metaRecord.transWordSuccesses,
        metaRecord.transWordLastTime,
        metaRecord.wordsFileOffset
      )
    }
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

private[wordstrainer] class LocalWords(dataDir: String)
    extends Iterable[PairWithTrainingData]
    with AutoCloseable {

  private[this] val metaFile = new MetaFile(dataDir)
  private[this] val wordsFile =
    try {
      new WordsFile(dataDir)
    } catch {
      case th: Throwable =>
        try {
          metaFile.close()
        } catch {
          case th2: Throwable =>
            th.addSuppressed(th2)
        }
        throw th
    }

  def saveNewPairs(externalPairs: collection.Seq[(String, String)]): Unit = {
    val externalWordToTrans =
      externalPairs.foldLeft(
        new mutable.HashMap[String, mutable.Set[String]](
          externalPairs.length,
          mutable.HashMap.defaultLoadFactor
        )
      ) { (map, pair) =>
        val translations = map.getOrElseUpdate(pair._1, mutable.Set(pair._2))
        translations += pair._2
        map
      }

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
  }

  def getTrainingData: TrainingData = {
    var totalToTrain = 0
    var totalTrained = 0
    val wordsToTrain = new ArrayBuffer[WordToTrain](10)
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

      if (trainingType != TrainingType.DoNotTrain && wordsToTrain.length < 10) {
        wordsToTrain += newWordToTrain(
          metaRecord,
          metaRecordIdx,
          wordsFile,
          trainingType
        )
      }

      metaRecordIdx += 1
    }
    TrainingData(totalToTrain, totalTrained, wordsToTrain)
  }

  def saveAnswers(
      trainedWords: collection.Seq[LocalWords.WordToTrain],
      answers: Array[Boolean]
  ): Unit = {
    var i = 0
    for (w <- trainedWords) {
      if (answers(i)) {
        metaFile.inc(w.metaRecordIndex, w.reverse)
      } else {
        metaFile.resetRecord(w.metaRecordIndex)
      }
      i += 1
    }
  }

  override def iterator: Iterator[PairWithTrainingData] = {
    metaFile.iterator.map { metaRecord =>
      val (word, trans) = wordsFile.readAt(metaRecord.wordsFileOffset)
      PairWithTrainingData(word, trans, metaRecord)
    }
  }

  def removeDuplicates(): Unit = {
    var removed = false
    do {
      removed = false
      val baseMeta = metaFile.last
      if (wordsFile.isLastPair(baseMeta.wordsFileOffset)) {
        val (baseWord, baseTrans) = wordsFile.readAt(baseMeta.wordsFileOffset)

        var metaIndex = metaFile.length - 2
        var dupFound = false
        while (metaIndex >= 0 && !dupFound) {
          val meta = metaFile(metaIndex)
          val (word, trans) = wordsFile.readAt(meta.wordsFileOffset)
          if (word == baseWord && trans == baseTrans) {
            dupFound = true
            println(
              "Removing " + PairWithTrainingData(baseWord, baseTrans, baseMeta)
            )
            metaFile.removeLast()
            wordsFile.removeFrom(baseMeta.wordsFileOffset)
            removed = true
          } else {
            metaIndex -= 1
          }
        }
      }
    } while (removed)
  }

  override def close(): Unit = {
    try {
      wordsFile.close()
    } finally {
      metaFile.close()
    }
  }

}
