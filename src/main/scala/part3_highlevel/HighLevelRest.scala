package part3_highlevel

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer

import scala.concurrent.Future

object HighLevelRest extends App {

  implicit val system = ActorSystem("highlevelrest")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  //directives
  import akka.http.scaladsl.server.Directives._

  val myPath =
    path("home") {
      get {
        complete(
          Future(HttpResponse(StatusCodes.OK)))
      } ~
      post {
        complete(
          Future(HttpResponse(StatusCodes.Forbidden)))
      }
    } ~
  path("about") {
    get {
      complete(
        Future(
          HttpResponse(StatusCodes.OK, entity = HttpEntity(
            """
              |<html>
              |<body>about</body>
              |</html>
            """.stripMargin)))
      )
    }
  }

  Http().bindAndHandle(myPath, "localhost", 8083)

}
