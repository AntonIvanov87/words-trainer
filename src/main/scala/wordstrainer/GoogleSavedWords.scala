package wordstrainer

import java.net.URI
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}
import java.time.Duration
import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

private object GoogleSavedWords {

  def getNew(
      lastLocalPair: Option[(String, String)],
      secrets: GoogleSecrets
  ): collection.Seq[(String, String)] = {

    val savedWordsURI = URI.create("https://translate.google.com/saved")
    val savedWordsReq = HttpRequest
      .newBuilder(savedWordsURI)
      .header("pragma", "no-cache")
      .header("cache-control", "no-cache")
      .header(
        "cookie",
        "__Secure-3PSID=" + secrets._Secure3PSID
      )
      .GET()
      .build()

    val httpClient = HttpClient
      .newBuilder()
      .connectTimeout(Duration.ofSeconds(3))
      .build()

    val resp = httpClient.send(savedWordsReq, BodyHandlers.ofLines())
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

  def parseSavedWords(
      respLines: java.util.stream.Stream[String],
      lastLocalPair: Option[(String, String)]
  ): collection.Seq[(String, String)] = {

    val pairRegex: Regex =
      // ["aphPo2NBCps","en","ru","heath","пустошь",1607184558359797,
      """.*\["[^"]++","[a-z][a-z]","[a-z][a-z]","([^"]++)","([^"]++)",[0-9]+[],].*""".r
    val res = new ArrayBuffer[(String, String)]()
    respLines
      .takeWhile({
        case pairRegex(word, trans) =>
          if (lastLocalPair.isEmpty || lastLocalPair.get != (word, trans)) {
            res += ((word, trans))
            true
          } else {
            // All other pairs are already known, stop iteration
            false
          }
        case _: String => true
      })
      .forEach(_ => {})
    res.reverse
  }

}
