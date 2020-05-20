package part1_recap

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.Success

object AkkaStreamRecap extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val source = Source(1 to 10)

  val sink = Sink.fold[Int, Int](0)(_ + _)

  val flow = Flow[Int].map(_ + 1)

  val simpleMat = source.via(flow).toMat(sink)(Keep.right).run()
  import system.dispatcher
  simpleMat.onComplete {
    case Success(value) => println(s"sompleted sum is $value")
  }

}
