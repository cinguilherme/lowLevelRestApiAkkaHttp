package part3_highlevel

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer

object DirectivesBreakDown extends App {

  implicit val system = ActorSystem("Directives")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher
  import akka.http.scaladsl.server.Directives._

  /**
    * Filtering Directives
    */

  val simpleRoute = post { // here are the verbs can be filtered
    complete(HttpResponse(StatusCodes.Forbidden))
  }

  val pathRoute = path("home") { // here can filter the URI /home
    complete(HttpEntity(ContentTypes.`application/json`,
      """
        |{
        |
        |}
      """.stripMargin))
  }

  val complexPathRoute =
    path("api" / "myEndpoint") { ///api/myEndpoint
      complete(StatusCodes.OK)
    }

  val dontConfuse =
    path("api/myEndpoint")

  /**
    * Path extraction directives
    */
  // /api/item/42

  val pathExtractionRoute =
  path("api" / "item" / IntNumber) { (number: Int) =>
    println(s"the item number $number")
    complete(StatusCodes.OK)
  }

  val multiExtract = path("api" / "inventory" / IntNumber / IntNumber) {
    (id, quantity) =>
      println(s"I got id $id and quantity $quantity")
      complete(StatusCodes.OK)
  }

  val queryExtracRoute =
    path("api" / "item") {
      parameter('id.as[Int]) { (id:Int) =>
        println(s"extracted id with $id")
        complete(StatusCodes.OK)
      }
    }

  val extractRequestRoute =
    path("control") {
      extractRequest { request: HttpRequest =>
        println(s"i got the request here $request")
        extractLog { log =>

          complete(StatusCodes.OK)
        }
      }
    }

  Http().bindAndHandle(extractRequestRoute, "localhost", 8080)
}
