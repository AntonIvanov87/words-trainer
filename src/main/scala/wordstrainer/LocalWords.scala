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

  def saveNewPairs(newPairs: Seq[(String, String)]): Unit = {
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

  def getWordsToTrain(): collection.Seq[WordToTrain] = {
    val res = new ArrayBuffer[WordToTrain](10)
    val metaFile = try {
      new MetaFile()
    } catch {
      case _: FileNotFoundException => return res
    }
    try {
      val wordsFile = new WordsFile()
      try {
        var i = 0
        for (metaRecord <- metaFile) {
          maybeTrain(metaRecord, i, wordsFile, res)
          if (res.length == 10) {
            return res
          }
          i += 1
        }
        res
      } finally {
        wordsFile.close()
      }
    } finally {
      metaFile.close()
    }
  }

  case class WordToTrain(question: String, answer: String, metaRecordIndex: Int, reverse: Boolean)

  private def maybeTrain(metaRecord: MetaFile.Record,
                         metaRecordIndex: Int,
                         wordsFile: WordsFile,
                         wordsToTrain: ArrayBuffer[WordToTrain]): Unit = {
    val nextWordTransTime = getNextTrainTime(metaRecord.wordTransSuccesses, metaRecord.wordTransLastTime)
    val nextTransWordTime = getNextTrainTime(metaRecord.transWordSuccesses, metaRecord.transWordLastTime)

    var nextTime = nextWordTransTime
    var reverse = false
    if (nextTransWordTime < nextWordTransTime) {
      nextTime = nextTransWordTime
      reverse = true
    }

    if (nextTime <= System.currentTimeMillis()) {
      wordsFile.seek(metaRecord.wordsFileOffset)
      val word = wordsFile.read()
      val trans = wordsFile.read()
      val wordToTrain =
        if (!reverse) {
          WordToTrain(word, trans, metaRecordIndex, reverse)
        } else {
          WordToTrain(trans, word, metaRecordIndex, reverse)
        }
      wordsToTrain.addOne(wordToTrain)
    }
  }

  private def getNextTrainTime(successes: Byte, lastSuccessTime: Long): Long =
    lastSuccessTime + TimeUnit.DAYS.toMillis(successes)
}
