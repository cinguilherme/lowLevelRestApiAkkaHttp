package part2_lowlevel

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import part2_lowlevel.GuitarDB.{CreateGuitar}
import spray.json._

import scala.concurrent.Future

object LowLevelREST extends App with GuitarStoreJsonProtocol {

  implicit val system = ActorSystem("lowLevelAPI")
  implicit val materializer = ActorMaterializer()


  /**
    * server code now
    */
  import scala.concurrent.duration._
  implicit val timeout = Timeout(3 seconds)
  val guitarDbActor = system.actorOf(Props[GuitarDB], "low_level_guitar_db")

  val listGuits = List(Guitar("guibson", "LX1", 0),
  Guitar("fender", "startocaster", 0),
  Guitar("yamaha", "GX1023", 0))

  listGuits.foreach(guitar => guitarDbActor ! CreateGuitar(guitar))

  implicit val executionContext = system.dispatchers.lookup("my-dispatcher")

  val actions = new GuitarActions

  val requestHandler: HttpRequest => Future[HttpResponse] = {

    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar"), _, _, _) => {
      val query: Query = uri.query()
      if(query.isEmpty) {
        actions.allGuitars()
      } else {
        actions.queryGuitars(query)
      }
    }

    case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitar"), _, entity, _) => actions.createNewGuitar(entity)

    case request: HttpRequest => {
      request.discardEntityBytes()
      Future(HttpResponse(StatusCodes.NotFound))
    }
  }




  Http().bindAndHandleAsync(requestHandler, "localhost", 8083)
}
