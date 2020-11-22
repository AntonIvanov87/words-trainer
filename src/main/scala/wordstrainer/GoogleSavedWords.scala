package wordstrainer

import com.fasterxml.jackson.core.{JsonFactory, JsonToken}
import java.io.InputStream
import java.net.URI
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}
import java.time.Duration
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks.{break, breakable}

private object GoogleSavedWords {

  def getNew(
      lastLocalPair: Option[(String, String)],
      secrets: GoogleSecrets
  ): collection.Seq[(String, String)] = {

    val savedWordsURI = URI.create(
      "https://translate.google.com/translate_a/sg?cm=g&xt=" + secrets.xt
    )
    val savedWordsReq = HttpRequest
      .newBuilder(savedWordsURI)
      .header("cache-control", "no-cache")
      .header(
        "user-agent",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.183 Safari/537.36"
      )
      .header(
        "cookie",
        "SID=" + secrets.sid + "; HSID=" + secrets.hsid + "; SSID=" + secrets.ssid
      )
      .GET()
      .build()

    val httpClient = HttpClient
      .newBuilder()
      .connectTimeout(Duration.ofSeconds(3))
      .build()
    val bodyHandler = BodyHandlers.ofInputStream()

    val resp = httpClient.send(savedWordsReq, bodyHandler)
    try {
      if (resp.statusCode() != 200) {
        throw new RuntimeException(
          "Failed to get saved words, Google replied with code " + resp
            .statusCode()
        )
      }

      parseSavedWords(resp.body(), lastLocalPair)
    } finally {
      resp.body().close()
    }
  }

  // e.g.: [11,null,[["1dOhqSa4FX0","en","ru","halcyon","безмятежный",1604304344669106],["rWyW5SveDOo","en","ru","commensurate","соразмерный",1604429522984572]],null,1604489275415896,null,10000]
  def parseSavedWords(
      respBodyStream: InputStream,
      lastLocalPair: Option[(String, String)]
  ): collection.Seq[(String, String)] = {
    val parser = new JsonFactory().createParser(respBodyStream)
    try {
      if (parser.nextToken() != JsonToken.START_ARRAY) {
        throw new IllegalStateException(
          "Unknown Google response format: expected array, but got " + parser
            .currentToken()
        )
      }

      if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
        throw new IllegalStateException(
          "Unknown Google response format: expected int number, but got " + parser
            .currentToken()
        )
      }
      val numPairs = parser.getIntValue

      while (parser.nextToken() != JsonToken.START_ARRAY) {
        if (parser.currentToken() == null) {
          throw new IllegalStateException(
            "Unknown Google response format: can not find an inner array of words"
          )
        }
      }

      val res = new ArrayBuffer[(String, String)]()
      breakable {
        for (i <- 0 until numPairs) {
          if (parser.nextToken() != JsonToken.START_ARRAY) {
            throw new IllegalStateException(
              "Unknown Google response format: expected " + numPairs + " words, but got " + i
            )
          }
          parser.nextToken()
          parser.nextToken()
          parser.nextToken()

          if (parser.nextToken() != JsonToken.VALUE_STRING) {
            throw new IllegalStateException(
              "Unknown Google response format: expected a word, but got " + parser
                .currentToken()
            )
          }
          val word = parser.getValueAsString

          if (parser.nextToken() != JsonToken.VALUE_STRING) {
            throw new IllegalStateException(
              "Unknown Google response format: expected a translation, but got " + parser
                .currentToken()
            )
          }
          val translation = parser.getValueAsString

          if (
            lastLocalPair.isDefined
            && lastLocalPair.get._1 == word && lastLocalPair.get._2 == translation
          ) {
            break()
          }

          res += ((word, translation))

          while (parser.nextToken() != JsonToken.END_ARRAY) {}
        }
      }

      res.reverse
    } finally {
      parser.close()
    }

  }

}
