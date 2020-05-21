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

  /**
    * Composite directives
    */

  val simpleNestRoute =
    path("api" / "item") {
      get {
        complete(StatusCodes.OK)
      }
    }

  val compacSimple = (path("api" / "v1") & get & extractLog) { (log) =>
    log.info("well this is really cool")
    complete(StatusCodes.OK)
  }

  val branchDryRoute = (path("about") | path("aboutUs")) {
    complete(StatusCodes.OK)
  }

  val blogByIdRoute = path(IntNumber) { postId =>
    //complex server logic
    complete(StatusCodes.OK)
  }
  val blogByQuyeryParam = parameter('postId.as[Int]) { postId =>
    complete(StatusCodes.OK)
  }

  val combinedBlogByIdRoute = (path(IntNumber) | parameter('postId.as[Int])) {
    postId =>
    complete(StatusCodes.OK)
  }

  /**
    * type 4 - actionable
    */

  val completeOkRoute = complete(StatusCodes.OK)
  val failedRoute = failWith(new RuntimeException) // completes with HTTP 500

  // rejects and pass it on to the next rooute
  val rejectRoute = path("home"){
    reject
  } ~
  path("index" / "endpoint") {
    get {
      complete(StatusCodes.OK)
    } ~
    post {
      complete(StatusCodes.Forbidden)
    }
  }

  Http().bindAndHandle(rejectRoute, "localhost", 8080)
}
