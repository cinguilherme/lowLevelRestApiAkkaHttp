package part4_exercice

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object RequestLevel extends App {

  implicit val system = ActorSystem("requestLevel")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

}
