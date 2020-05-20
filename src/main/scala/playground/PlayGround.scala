package playground

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object PlayGround extends App {

  implicit val system = ActorSystem("playGround")
  implicit val materializer = ActorMaterializer()

  println("it builds")


}
