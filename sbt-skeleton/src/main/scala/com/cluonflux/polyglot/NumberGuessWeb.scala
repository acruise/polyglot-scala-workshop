import spray.http._
import akka.actor._
import akka.pattern.ask
import spray.routing.SimpleRoutingApp
import scala.xml.NodeSeq
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Await

object NumberGuessWeb extends App
  with SimpleRoutingApp
  with spray.httpx.SprayJsonSupport
{
  implicit val system = ActorSystem("my-system")

  val game = system.actorOf(Props(new NumberGameActor))

  import GuessNumber._

  implicit val timeout = Timeout(5.seconds)
  
  def form(action: String): NodeSeq = {
   <form action={action} method="post">
     <input name="guessed"/>
     <input type="submit" name="submit"/>
   </form>
  }

  import spray.json._
  import spray.json.DefaultJsonProtocol._
  import scala.reflect.ClassTag
  import spray.httpx.SprayJsonSupport._

  implicit val remainingGuessesF = jsonFormat1(RemainingGuesses)
  implicit val guessF = jsonFormat1(Guess)
  val highF = jsonFormat1(High) 
  implicit val taggedHighF = new JsonWriter[High] {
    def write(high: High): JsValue = {
      highF.write(high) match {
	      case JsObject(fields) =>
          JsObject(fields + ("status" -> JsString("High")))
      }
    }
  }
  val lowF = jsonFormat1(Low)
  implicit val taggedLowF = new JsonWriter[Low] {
    def write(low: Low): JsValue = {
      lowF.write(low) match {
        case JsObject(fields) =>
          JsObject(fields + ("status" -> JsString("Low")))
      }
    }
  }

  implicit val wonF = new JsonWriter[Won.type] {
    def write(t: Won.type): JsValue = JsObject("status" -> JsString("won"))
  }

  implicit val lostF = new JsonWriter[Lost.type] {
    def write(t: Lost.type): JsValue = JsObject("status" -> JsString("lost"))
  }

  startServer(interface = "localhost", port = 8080) {
    path("restgame") {
      get {
	      complete(
	        game.ask(QueryGuesses).mapTo[RemainingGuesses]
	      )
      } ~ post {
	      entity(as[Guess]) { guess =>
          val jsonFuture = game.ask(guess).map {
            case w: Won.type => JsObject(Map("status" -> JsString("Won")))
            case l: Lost.type => JsObject(Map("status" -> JsString("Lost")))

            case h: High => h.toJson
            case l: Low => l.toJson
    	  }
          
    	  complete(jsonFuture.map(_.prettyPrint))
	    }
    }
  } ~ path("game") {
    get {
      complete {
          <xml:group>
            <h1>Welcome to the Number Game</h1>
	    {form("/game")}
          </xml:group>
        }
      } ~ post {
	formFields('guessed.as[Int]) { (guessed) =>
    val htmlFuture = game.ask(Guess(guessed)) map {
	    case Won => <h1>You won!</h1>
	    case Lost => <h1>You lost! :(</h1>

	    case High(remaining) =>
	      <xml:group>
  	        <h1>Your guess was too HIGH ({remaining} guesses left)</h1>
	        {form("/game")}
              </xml:group>
	      
	    case Low(remaining) => 
              <xml:group>
                 <h1>Your guess was too LOW ({remaining} guesses left)</h1>
                 {form("/game")}
              </xml:group>
	  }

	  complete(htmlFuture)
	}
      }

    }
  }
}

