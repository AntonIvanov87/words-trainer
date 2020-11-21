package wordstrainer

import java.time.Instant

private object PrintLocalWordsMain {
  def main(args: Array[String]): Unit = {
    val settings = Settings()
    val metaFile = MetaFile(settings.dataDir)
    try {
      val wordsFile = WordsFile(settings.dataDir)
      try {
        println(
          "'Word' 'Translation' 'Word->Trans Successes' 'Word->Trans Last Time' 'Trans->Word Successes' 'Trans->Word Last Time"
        )
        for (m <- metaFile) {
          wordsFile.seek(m.wordsFileOffset)
          val word = wordsFile.read()
          val trans = wordsFile.read()
          println(
            word + " " + trans + " " + m.wordTransSuccesses
              + " " + Instant.ofEpochMilli(m.wordTransLastTime)
              + " " + m.transWordSuccesses + " " + Instant.ofEpochMilli(
              m.transWordLastTime
            )
          )
        }
      } finally {
        wordsFile.close()
      }
    } finally {
      metaFile.close()
    }
  }
}
