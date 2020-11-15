package wordstrainer

private object Trainer {

  def train(words: collection.Seq[LocalWords.WordToTrain]): Array[Boolean] = {
    val res = new Array[Boolean](words.length)
    var i = 0
    for (w <- words) {
      // TODO: add color
      // TODO: remove
      print('\n' + w.question)
      Console.in.readLine()

      print(w.answer + " y/n? ")
      val answer = Console.in.readLine()
      res(i) = answer == "y" || answer == "Y"
      i += 1
    }
    res
  }

}
