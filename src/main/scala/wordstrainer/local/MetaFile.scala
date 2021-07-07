package wordstrainer.local

import wordstrainer.local.MetaFile.{fileName, recordLen}

import java.io.{File, RandomAccessFile}
import scala.collection.mutable

private object MetaFile {
  private val fileName = "meta.dat"

  // wordsOffset: Long
  // + num word->trans successes: Byte + word->trance last timestamp: Long
  // + num trans->word successes: Byte + trans->word last timestamp: Long
  private val recordLen = 8 + 1 + 8 + 1 + 8

  case class Record(
      wordsFileOffset: Long,
      wordTransSuccesses: Byte,
      wordTransLastTime: Long,
      transWordSuccesses: Byte,
      transWordLastTime: Long
  ) {

    def this(wordsFileOffset: Long) = this(wordsFileOffset, 0, 0, 0, 0)

  }

}

private class MetaFile(dataDir: String)
    extends IndexedSeq[MetaFile.Record]
    with mutable.Growable[MetaFile.Record]
    with AutoCloseable {

  // TODO: extract write methods into a separate class?
  private val raf = new RandomAccessFile(new File(dataDir, fileName), "rw")

  override def addOne(elem: MetaFile.Record): MetaFile.this.type = {
    seekEnd()
    write(elem)
    this
  }

  def inc(i: Int, reverse: Boolean): Unit = {
    seekRecord(i)
    if (!reverse) {
      raf.seek(raf.getFilePointer + 8)
    } else {
      raf.seek(raf.getFilePointer + 8 + 1 + 8)
    }
    val prev = raf.readByte()
    if (prev == Byte.MaxValue) {
      return
    }
    raf.seek(raf.getFilePointer - 1)
    raf.writeByte(prev + 1)
    raf.writeLong(System.currentTimeMillis())
  }

  def resetRecord(i: Int): Unit = {
    seekRecord(i)
    raf.readLong()
    raf.writeByte(0)
    raf.writeLong(0)
    raf.writeByte(0)
    raf.writeLong(0)
  }

  override def apply(i: Int): MetaFile.Record = {
    seekRecord(i)
    read()
  }

  override def length: Int = (raf.length() / recordLen).toInt

  override def knownSize: Int = super[IndexedSeq].knownSize

  def removeLast(): Unit = {
    raf.setLength(raf.length() - recordLen)
  }

  override def close(): Unit = raf.close()

  private[this] def write(metaRecord: MetaFile.Record): Unit = {
    raf.writeLong(metaRecord.wordsFileOffset)
    raf.writeByte(metaRecord.wordTransSuccesses)
    raf.writeLong(metaRecord.wordTransLastTime)
    raf.writeByte(metaRecord.transWordSuccesses)
    raf.writeLong(metaRecord.transWordLastTime)
  }

  private[this] def seekRecord(index: Int): Unit = raf.seek(index * recordLen)

  private[this] def seekEnd(): Unit = raf.seek(raf.length())

  private[this] def read(): MetaFile.Record = {
    MetaFile.Record(
      raf.readLong(),
      raf.readByte(),
      raf.readLong(),
      raf.readByte(),
      raf.readLong()
    )
  }

  override def clear(): Unit = throw new UnsupportedOperationException
}
