package part3_highlevel

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.get
import akka.stream.ActorMaterializer
import akka.util.Timeout
import part2_lowlevel.{Guitar, GuitarActions, GuitarDB, GuitarStoreJsonProtocol}
import part2_lowlevel.GuitarDB.CreateGuitar
import spray.json._

object HighLevelExample extends App with GuitarStoreJsonProtocol{
  import scala.concurrent.duration._
  implicit val timeout = Timeout(3 seconds)

  implicit val system = ActorSystem("highlevelexmple")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  // Initial setup
  val guitarDbActor = system.actorOf(Props[GuitarDB], "low_level_guitar_db")

  val listGuits = List(Guitar("guibson", "LX1", 0),
    Guitar("fender", "startocaster", 0),
    Guitar("yamaha", "GX1023", 0))

  listGuits.foreach(guitar => guitarDbActor ! CreateGuitar(guitar))
  val actions = new GuitarActions

  //END

  import akka.http.scaladsl.server.Directives._

  val guitarServerRoute =
    path("api" / "guitar") {
      parameter('id.as[Int]) { id =>
        get {
          complete(actions.guitarById(id))
        }
      } ~
        get {
        complete(actions.allGuitars())
      } ~
        (post & extractRequest) { request =>
        val entity = request.entity
        complete(actions.createNewGuitar(entity))
      }
    } ~
    path("api" / "guitar" / IntNumber) { id =>
      get {
        complete(actions.guitarById(id))
      }
    } ~
    path("api" / "guitar" / "inventory") {
      get {
        parameter('stock.as[Boolean]) { stock =>
          complete(actions.inventory(stock))
        }
      }
    }

  val guitarServerNeat =
    pathPrefix("api" / "guitar") {

      path("inventory") {

        parameter('stock.as[Boolean]) { stock =>
          get {
            complete(actions.inventory(stock))
          }
        }
      } ~
        (parameter('id.as[Int]) | path(IntNumber)) { id =>

          get {
            complete(actions.guitarById(id))
          }
      } ~
      (post & extractRequest) { request =>
        val entity = request.entity
        complete(actions.createNewGuitar(entity))
      } ~
        pathEndOrSingleSlash {
          get {
            complete(actions.allGuitars())
          }
        }
    }



  Http().bindAndHandle(guitarServerNeat, "localhost", 8083)

}
