import akka.actor._

object PingPong {
  def main(args: Array[String]) {
    val system = ActorSystem("pingPong")
    val pinger = system.actorOf(Props(new PingPongActor), "pinger")
    val ponger = system.actorOf(Props(new PingPongActor), "ponger")
    
    pinger ! Intro(ponger)
  }

  case class Intro(otherGuy: ActorRef)
  case object Ping
  case object Pong
}

final class PingPongActor extends Actor with ActorLogging {
  import PingPong._
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = {
    case Intro(otherGuy) => 
      otherGuy ! Ping
    case Ping =>
      log.info("Ping")
      sender() ! Pong
    case Pong =>
      log.info("Pong")
      context.system.scheduler.scheduleOnce(1.seconds, sender(), Ping)
  }
}
