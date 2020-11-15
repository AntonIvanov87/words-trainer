package wordstrainer

import java.io.{FileInputStream, InputStream}
import java.net.URI
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}
import java.time.Duration
import java.util.Properties

import com.fasterxml.jackson.core.{JsonFactory, JsonToken}

import scala.collection.mutable
import scala.jdk.CollectionConverters.PropertiesHasAsScala


private object GoogleSavedWords {

  def get(): Array[(String, String)] = {
    val secrets = loadSecrets()

    val savedWordsURI = URI.create("https://translate.google.com/translate_a/sg?cm=g&xt=" + secrets("xt"))
    val savedWordsReq = HttpRequest.newBuilder(savedWordsURI)
      .header("cache-control", "no-cache")
      .header("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.183 Safari/537.36")
      .header("cookie", "SID=" + secrets("SID") + "; HSID=" + secrets("HSID") + "; SSID=" + secrets("SSID"))
      .GET().build()

    val httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(3))
      .build()
    val bodyHandler = BodyHandlers.ofInputStream()

    val resp = httpClient.send(savedWordsReq, bodyHandler)
    try {
      if (resp.statusCode() != 200) {
        throw new RuntimeException("Failed to get saved words, Google replied with code " + resp.statusCode())
      }

      parseSavedWords(resp.body())
    } finally {
      resp.body().close()
    }
  }

  private def loadSecrets(): mutable.Map[String, String] = {
    val fis = new FileInputStream("secrets.properties")
    (try {
      val secrets = new Properties()
      secrets.load(fis)
      secrets
    } finally {
      fis.close()
    }).asScala
  }

  // e.g.: [11,null,[["1dOhqSa4FX0","en","ru","halcyon","безмятежный",1604304344669106],["rWyW5SveDOo","en","ru","commensurate","соразмерный",1604429522984572]],null,1604489275415896,null,10000]
  def parseSavedWords(respBodyStream: InputStream): Array[(String, String)] = {
    val parser = new JsonFactory().createParser(respBodyStream)
    try {
      if (parser.nextToken() != JsonToken.START_ARRAY) {
        throw new IllegalStateException("Unknown Google response format: expected array, but got " + parser.currentToken())
      }

      if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
        throw new IllegalStateException("Unknown Google response format: expected int number, but got " + parser.currentToken())
      }
      val numPairs = parser.getIntValue

      while (parser.nextToken() != JsonToken.START_ARRAY) {
        if (parser.currentToken() == null) {
          throw new IllegalStateException("Unknown Google response format: can not find an inner array of words")
        }
      }

      val res = new Array[(String, String)](numPairs)
      for (i <- 0 until numPairs) {
        if (parser.nextToken() != JsonToken.START_ARRAY) {
          throw new IllegalStateException("Unknown Google response format: expected " + numPairs + " words, but got " + i)
        }
        parser.nextToken()
        parser.nextToken()
        parser.nextToken()

        if (parser.nextToken() != JsonToken.VALUE_STRING) {
          throw new IllegalStateException("Unknown Google response format: expected a word, but got " + parser.currentToken())
        }
        val word = parser.getValueAsString

        if (parser.nextToken() != JsonToken.VALUE_STRING) {
          throw new IllegalStateException("Unknown Google response format: expected a translation, but got " + parser.currentToken())
        }
        val translation = parser.getValueAsString

        res(i) = (word, translation)

        while (parser.nextToken() != JsonToken.END_ARRAY) {}
      }
      reverse(res)
      res
    } finally {
      parser.close()
    }

  }

  private def reverse[T](arr: Array[T]): Unit = {
    var i = 0
    var j = arr.length-1
    while(i < j) {
      val temp = arr(i)
      arr(i) = arr(j)
      arr(j) = temp
      i+=1
      j-=1
    }
  }

}
