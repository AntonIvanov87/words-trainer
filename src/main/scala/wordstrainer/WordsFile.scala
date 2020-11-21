package wordstrainer

import java.io.{File, RandomAccessFile}
import java.nio.charset.Charset
import wordstrainer.WordsFile.fileName

private object WordsFile {

  private val fileName = "words.dat"

  def apply(dataDir: String) = new WordsFile(dataDir)

}

private class WordsFile private (dataDir: String) extends AutoCloseable {

  private val raf = new RandomAccessFile(new File(dataDir, fileName), "rw")

  def seek(offset: Long): Unit = raf.seek(offset)

  def seekEnd(): Unit = raf.seek(raf.length())

  def getFilePointer(): Long = raf.getFilePointer

  def read(): String = {
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

  def write(word: String): Unit = {
    val bytes = word.getBytes(Charset.forName("UTF-8"))
    if (bytes.length > Byte.MaxValue) {
      throw new IllegalArgumentException(
        "Too long word " + word + ": " + bytes.length + " bytes > " + Byte.MaxValue
      )
    }
    raf.writeByte(bytes.length)
    raf.write(bytes)
  }

  override def close(): Unit = raf.close()
}
