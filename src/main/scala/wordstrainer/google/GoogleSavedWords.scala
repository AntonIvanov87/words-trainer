package wordstrainer.google

import java.net.URI
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}
import java.time.Duration
import java.util.function.{BiConsumer, Supplier}
import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

private[wordstrainer] object GoogleSavedWords {

  def get(secrets: GoogleSecrets): collection.Seq[(String, String)] = {

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

      parseSavedWords(resp.body())
    } finally {
      resp.body().close()
    }
  }

  def parseSavedWords(
      respLines: java.util.stream.Stream[String]
  ): collection.Seq[(String, String)] = {
    val pairs = ArrayBuffer[(String, String)]()
    val pairRegex: Regex =
      // ["aphPo2NBCps","en","ru","heath","пустошь",1607184558359797
      """\["[^"]++","[a-z][a-z]","[a-z][a-z]","([^"]++)","([^"]++)",[0-9]++[],]""".r
    respLines.forEach { line =>
      pairRegex.findAllMatchIn(line).foreach { m =>
        pairs.addOne((m.group(1), m.group(2)))
      }
    }
    pairs
  }

}
