package wordstrainer

import wordstrainer.local.{LocalWords, MetaFile, WordsFile}

private object PrintLocalWordsMain {
  def main(args: Array[String]): Unit = {
    val settings = Settings()
    val metaFile = MetaFile(settings.dataDir)
    try {
      val wordsFile = WordsFile(settings.dataDir)
      try {
        println(
          "'Offset' 'Word' 'Translation' 'Word->Trans Successes' 'Word->Trans Last Time' 'Trans->Word Successes' 'Trans->Word Last Time"
        )
        for (m <- metaFile) {
          val (word, trans) = wordsFile.readAt(m.wordsFileOffset)
          println(LocalWords.toString(m, word, trans))
        }
      } finally {
        wordsFile.close()
      }
    } finally {
      metaFile.close()
    }
  }
}
