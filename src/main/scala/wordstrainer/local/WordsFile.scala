package wordstrainer.local

import wordstrainer.local.WordsFile.fileName

import java.io.{File, RandomAccessFile}
import java.nio.charset.Charset
import scala.collection.mutable

private object WordsFile {

  private val fileName = "words.dat"

  def apply(dataDir: String) = new WordsFile(dataDir)

}

private class WordsFile(dataDir: String)
    extends mutable.Growable[(String, String)]
    with AutoCloseable {

  private val raf = new RandomAccessFile(new File(dataDir, fileName), "rw")

  override def addOne(pair: (String, String)): WordsFile.this.type = {
    seekEnd()
    write(pair._1)
    write(pair._2)
    this
  }

  def seekEnd(): Unit = raf.seek(raf.length())

  def readAt(offset: Long): (String, String) = {
    seek(offset)
    read()
  }

  def read(): (String, String) = (readOne(), readOne())

  def isLastPair(offset: Long): Boolean = {
    seek(offset)
    read()
    val res = raf.getFilePointer == raf.length()
    seek(offset)
    res
  }

  def getFilePointer: Long = raf.getFilePointer

  def removeFrom(offset: Long): Unit = {
    raf.setLength(offset)
  }

  override def close(): Unit = raf.close()

  override def clear(): Unit = throw new UnsupportedOperationException

  private[this] def write(word: String): Unit = {
    val bytes = word.getBytes(Charset.forName("UTF-8"))
    if (bytes.length > Byte.MaxValue) {
      throw new IllegalArgumentException(
        "Too long word " + word + ": " + bytes.length + " bytes > " + Byte.MaxValue
      )
    }
    raf.writeByte(bytes.length)
    raf.write(bytes)
  }

  private[this] def seek(offset: Long): Unit = raf.seek(offset)

  private[this] def readOne(): String = {
    val wordLen = raf.readByte()
    val wordBytes = new Array[Byte](wordLen)
    val read = raf.read(wordBytes)
    if (read != wordLen) {
      throw new IllegalStateException(
        "Bad word length: expected " + wordLen +
          ", but read " + read + ", end offset " + raf.getFilePointer
      )
    }
    new String(wordBytes)
  }

}
