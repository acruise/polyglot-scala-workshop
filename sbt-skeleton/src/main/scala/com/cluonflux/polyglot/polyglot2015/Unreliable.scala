package com.cluonflux.polyglot.polyglot2015

import akka.actor.SupervisorStrategy.Escalate
import akka.actor._

import scala.concurrent.duration._

object Unreliable {
  def main(args: Array[String]): Unit = {
    val sys = ActorSystem("unreliable")

    sys.actorOf(Props(new RootActor()))
  }
}

final class RootActor extends Actor with ActorLogging {
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(10, 10.minute) {
    case e: RuntimeException =>
      Escalate
  }

  override def preStart(): Unit = {
    self ! 'init
  }

  var child: Option[ActorRef] = None

  override def receive: Receive = {
    case 'init =>
      log.info("Parent is initializing!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
      val ref = context.actorOf(Props(new ChildActor()))

      child = Some(ref)
      context.watch(ref)

    case 'panic =>
      child.foreach(context.stop)

    case Terminated(victim) =>
      log.info("O NOESS!!! {} died!", victim)
  }
}

final class ChildActor extends Actor with ActorLogging {
  private val rnd = new scala.util.Random()

  override def preStart(): Unit = {
    self ! 'init
  }

  override def receive: Actor.Receive = {
    case 'init =>
      import scala.concurrent.ExecutionContext.Implicits.global

      context.system.scheduler.schedule(1.second, 1.second, self, 'tick)

    case 'tick =>
      log.info("Tick!")

      if (rnd.nextInt() % 10 == 0) sys.error("Whoops!")
  }
}
