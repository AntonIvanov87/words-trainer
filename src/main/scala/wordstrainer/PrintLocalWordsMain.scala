package wordstrainer

import wordstrainer.local.LocalWords

private object PrintLocalWordsMain {
  def main(args: Array[String]): Unit = {
    val settings = Settings()
    val localWords = new LocalWords(settings.dataDir)
    try {
      println(
        "'Offset' 'Word' 'Translation' 'Word->Trans Successes' 'Word->Trans Last Time' 'Trans->Word Successes' 'Trans->Word Last Time"
      )
      localWords.foreach { pair =>
        println(pair.toString)
      }
    } finally {
      localWords.close()
    }
  }
}
