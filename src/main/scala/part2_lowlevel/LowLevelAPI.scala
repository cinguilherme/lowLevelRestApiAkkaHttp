package part2_lowlevel

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, HttpMethods, HttpRequest, HttpResponse, StatusCode, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import org.scalatest.path

import scala.concurrent.Future

object LowLevelAPI extends App {

  implicit val system = ActorSystem("lowlevelsystem")
  implicit val materializer = ActorMaterializer()

  val serverSource = Http().bind("localhost", 8001)

  val connectionSink = Sink.foreach[IncomingConnection]{
    connection =>

      println(s"accepted incomming connection from ${connection.remoteAddress}")
  }

  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |<body>Hi</body>
            |</html>
          """.stripMargin
        )
    )
    case request: HttpRequest => {
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
          """
          |<html>
          |<body>Opps</body>
          |</html>
          """.stripMargin)
      )
    }
  }

  val httpSinkConnectionHandler = Sink.foreach[IncomingConnection](connection => {
    connection.handleWithSyncHandler(requestHandler)
  })

//  serverSource.to(httpSinkConnectionHandler).run()

  import system.dispatcher
  val requestHandlerAsync: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
      Future(HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |<body>Hi</body>
            |</html>
          """.stripMargin
        )
      ))
    case request: HttpRequest => {
      request.discardEntityBytes()
      Future(HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |<body>Opps</body>
            |</html>
          """.stripMargin)
      ))
    }
  }

//  Http().bindAndHandleAsync(requestHandlerAsync, "localhost", 8002)

  /**
    * via a flow
    */

  val streamsBasedRequestHandler: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {

    case HttpRequest(HttpMethods.GET, _, _, _, _) => {
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |<body>Hi</body>
            |</html>
          """.stripMargin
        )
      )
    }
    case request: HttpRequest => {
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |<body>Opps</body>
            |</html>
          """.stripMargin)
      )
    }

  }

//  Http().bind("localhost", 8003).runForeach { incommingConnection =>
//    incommingConnection.handleWith(streamsBasedRequestHandler)
//  }
//
//  Http().bindAndHandle(streamsBasedRequestHandler, "localhost", 8004)

  /**
    * Exercice, simple server with a home and a about page and a 404 for the rest
    *
    */

  val home = HttpResponse(StatusCodes.OK,
    entity = HttpEntity(
      ContentTypes.`application/json`,
      """
        |{"status": "ok"}
      """.stripMargin
    ))
  val about = HttpResponse(StatusCodes.OK,
    entity = HttpEntity(
      ContentTypes.`application/json`,
      """
        |{"about": "nothing much yet"}
      """.stripMargin
    ))

  val notFound = HttpResponse(StatusCodes.NotFound,
    entity = HttpEntity(
      """
        |this is messed up
      """.stripMargin
    ))

  val requestHandlerx: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {

    case HttpRequest(HttpMethods.GET, Uri.Path("/"), _, _, _) => home
    case HttpRequest(HttpMethods.GET, Uri.Path("/about"), _, _, _) => about
    case request: HttpRequest => notFound
  }

  val newServer = Http().bindAndHandle(requestHandlerx,"localhost", 3844)


  val sinkExerciceHandler:  HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/"), _, _, _) => home
    case HttpRequest(HttpMethods.GET, Uri.Path("/about"), _, _, _) => about
    case request: HttpRequest => {
      request.discardEntityBytes()
      notFound
    }
  }

  val asyncSuperHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/"), _, _, _) => Future(home)
    case HttpRequest(HttpMethods.GET, Uri.Path("/about"), _, _, _) => Future(about)
    case request: HttpRequest => {
      request.discardEntityBytes()
      Future(notFound)
    }
  }


}
