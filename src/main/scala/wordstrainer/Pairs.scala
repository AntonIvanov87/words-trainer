package wordstrainer

import java.time.Instant

private object Pairs {
  def toString(metaRecord: MetaFile.Record, word: String, trans: String): String = {
    metaRecord.wordsFileOffset + " " + word + " " + trans + " " +
      metaRecord.wordTransSuccesses + " " + Instant.ofEpochMilli(metaRecord.wordTransLastTime) + " " +
      metaRecord.transWordSuccesses + " " + Instant.ofEpochMilli(metaRecord.transWordLastTime)
  }
}
