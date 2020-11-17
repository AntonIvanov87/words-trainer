package wordstrainer

private object Trainer {

  def train(words: collection.Seq[LocalWords.WordToTrain]): Array[Boolean] = {
    val res = new Array[Boolean](words.length)
    var i = 0
    for (w <- words) {
      // TODO: add color
      print('\n' + w.question)
      Console.in.readLine()

      print(w.answer)

      var answer: String = null
      do {
        print( " y/n? ")
        answer = Console.in.readLine()
      } while (answer != "y" && answer != "n")

      res(i) = answer == "y"

      i += 1
    }
    res
  }

}
