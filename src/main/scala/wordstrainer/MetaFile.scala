package wordstrainer

import java.io.{FileNotFoundException, RandomAccessFile}

import wordstrainer.MetaFile.{fileName, recordLen}

private class MetaFile() extends AutoCloseable with Iterable[MetaFile.Record] {
  // TODO: extract write methods into a separate class
  private val raf = new RandomAccessFile(fileName, "rw")

  def write(metaRecord: MetaFile.Record): Unit = {
    raf.writeLong(metaRecord.wordsFileOffset)
    raf.writeByte(metaRecord.wordTransSuccesses)
    raf.writeLong(metaRecord.wordTransLastTime)
    raf.writeByte(metaRecord.transWordSuccesses)
    raf.writeLong(metaRecord.transWordLastTime)
  }

  def resetRecord(): Unit = {
    raf.readLong()
    raf.writeByte(0)
    raf.writeLong(0)
    raf.writeByte(0)
    raf.writeLong(0)
  }

  def inc(reverse: Boolean): Unit = {
    if (!reverse) {
      raf.seek(raf.getFilePointer + 8)
    } else {
      raf.seek(raf.getFilePointer + 8 + 1 + 8)
    }
    val prev = raf.readByte()
    if (prev == Byte.MaxValue) {
      return
    }
    raf.seek(raf.getFilePointer-1)
    raf.writeByte(prev+1)
    raf.writeLong(System.currentTimeMillis())
  }

  def read(): MetaFile.Record = {
    MetaFile.Record(raf.readLong(), raf.readByte(), raf.readLong(), raf.readByte(), raf.readLong())
  }

  def seekRecord(index: Int): Unit = raf.seek(index * recordLen)

  def seekEnd(): Unit = raf.seek(raf.length())

  override def iterator: Iterator[MetaFile.Record] = new Iterator[MetaFile.Record] {

    raf.seek(0)
    private val numRecords = raf.length() / recordLen
    private var curRecordIndex = 0

    override def hasNext: Boolean = curRecordIndex < numRecords

    override def next(): MetaFile.Record = {
      curRecordIndex += 1
      read()
    }

  }

  override def close(): Unit = raf.close()

}

private object MetaFile {
  private val fileName = "meta.dat"

  // wordsOffset: Long
  // + num word->trans successes: Byte + word->trance last timestamp: Long
  // + num trans->word successes: Byte + trans->word last timestamp: Long
  private val recordLen = 8 + 1 + 8 + 1 + 8

  case class Record(wordsFileOffset: Long,
                    wordTransSuccesses: Byte, wordTransLastTime: Long,
                    transWordSuccesses: Byte, transWordLastTime: Long) {

    def this(wordsFileOffset: Long) = this(wordsFileOffset, 0, 0, 0, 0)

  }

  def getLastWordPairOffset(): Option[Long] = {
    val metaRAF = try {
      new RandomAccessFile(fileName, "r")
    } catch {
      case _: FileNotFoundException => return Option.empty
    }
    try {
      val metaFileLen = metaRAF.length()
      if (metaFileLen == 0) {
        return Option.empty
      }

      val numMetaRecords = metaFileLen / recordLen
      val lastRecordOffset = (numMetaRecords - 1) * recordLen
      metaRAF.seek(lastRecordOffset)
      Some(metaRAF.readLong())

    } finally {
      metaRAF.close()
    }
  }

}
