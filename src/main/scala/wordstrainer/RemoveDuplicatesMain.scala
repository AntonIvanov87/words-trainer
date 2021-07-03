package wordstrainer

import wordstrainer.local.{LocalWords, MetaFile, WordsFile}

private object RemoveDuplicatesMain {

  def main(args: Array[String]): Unit = {
    val settings = Settings()
    val metaFile = MetaFile(settings.dataDir)
    try {
      val wordsFile = WordsFile(settings.dataDir)
      try {
        var removed = false
        do {
          removed = false
          val baseMeta = metaFile.last
          if (wordsFile.isLastPair(baseMeta.wordsFileOffset)) {
            val (baseWord, baseTrans) = wordsFile.readAt(baseMeta.wordsFileOffset)
            //println("Base: " + Pairs.toString(baseMeta, baseWord, baseTrans))

            var metaIndex = metaFile.length - 2
            var dupFound = false
            while (metaIndex >= 0 && !dupFound) {
              val meta = metaFile(metaIndex)
              val (word, trans) = wordsFile.readAt(meta.wordsFileOffset)
              if (word == baseWord && trans == baseTrans) {
                dupFound = true
                //println("Dup:  " + Pairs.toString(meta, word, trans))
                println("Removing " + LocalWords.toString(baseMeta, baseWord, baseTrans))
                metaFile.removeLast()
                wordsFile.removeFrom(baseMeta.wordsFileOffset)
                removed = true
              } else {
                metaIndex -= 1
              }
            }
          }
        } while (removed)
      } finally {
        wordsFile.close()
      }
    } finally {
      metaFile.close()
    }

  }

}
