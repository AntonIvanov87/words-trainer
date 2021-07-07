package wordstrainer

import wordstrainer.local.LocalWords

private object RemoveDuplicatesMain {

  def main(args: Array[String]): Unit = {
    val settings = Settings()
    val localWords = new LocalWords(settings.dataDir)
    try {
      localWords.removeDuplicates()
    } finally {
      localWords.close()
    }
  }

}
