import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object GuessNumber {
  def main(args: Array[String]) {
    val system = ActorSystem("guessNumber")

    val game = system.actorOf(Props(new NumberGameActor))

    val client = system.actorOf(Props(new NumberGameClientActor(game))) 
  }

  case class Guess(number: Int)
  case class High(remainingGuesses: Int)
  case class Low(remainingGuesses: Int)
  case object Won
  case object Lost
  case object QueryGuesses
  case class RemainingGuesses(remaining: Int)
}

final class NumberGameClientActor(game: ActorRef) extends Actor with ActorLogging {
  import GuessNumber._

  override def preStart() {
    self ! 'init
  }

  var lastGuess = 50
  var lowGuess = 0
  var highGuess = 100

  def receive = {
    case 'init => 
      game ! Guess(lastGuess)

    case Won =>
      log.info("YAY!!!!")
      context.stop(self)

    case Lost =>
      log.info("O NOES")
      context.stop(self)

    case High(remaining) =>
      log.info("{} was too high; {} guesses remaining", lastGuess, remaining)

      highGuess = lastGuess
      val newGuess = (highGuess+lowGuess)/2
      lastGuess = newGuess
      game ! Guess(newGuess)

    case Low(remaining) => 
      log.info("{} was too low; {} guesses remaining", lastGuess, remaining)

      lowGuess = lastGuess
      val newGuess = (highGuess+lowGuess)/2
      lastGuess = newGuess
      game ! Guess(newGuess)
  }
}

final class NumberGameActor extends Actor with ActorLogging {
  import GuessNumber._

  override def preStart() {
    self ! 'init
  }

  def receive = {
    case 'init => 
      val targetNumber = scala.math.abs(scala.util.Random.nextInt() % 100) + 1
      val maxGuesses = 7

      context.become(receiveGame(targetNumber, maxGuesses))
  }

  def receiveGame(targetNumber: Int, remainingGuesses: Int): Actor.Receive = {
    case Guess(guessed) => 
      if (guessed == targetNumber) {
        sender() ! Won
        context.stop(self)
        context.system.shutdown()
      } else if (remainingGuesses == 0) {
        sender() ! Lost
        context.stop(self)
        context.system.shutdown()
      } else if (guessed > targetNumber) {
        sender() ! High(remainingGuesses)
        context.become(receiveGame(targetNumber, remainingGuesses - 1))
      } else {
        sender() ! Low(remainingGuesses)
        context.become(receiveGame(targetNumber, remainingGuesses - 1))
      }
    case QueryGuesses =>
      sender() ! RemainingGuesses(remainingGuesses)
  }
}

