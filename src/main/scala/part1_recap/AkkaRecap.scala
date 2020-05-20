package part1_recap

import akka.actor.{Actor, ActorSystem, Props}
import akka.util.Timeout

import scala.util.Success

object AkkaRecap extends App {

  implicit val system = ActorSystem("akkaRecap")

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "hi" => sender() ! "hello mr"
      case "ih" => println("not gonna respond to that")
      case _ => sender() ! "what?"
    }
  }


  val sim = system.actorOf(Props[SimpleActor], "simpleActor")
  import scala.concurrent.duration._
  implicit val timeout = Timeout(3 second)

  import system.dispatcher
  import akka.pattern.ask
  import akka.pattern.pipe

  val rs = sim ? "hi"
  rs.onComplete {
    case Success(resp) => println(s" got $resp, but what?")
  }

  sim ! "ih"

}
