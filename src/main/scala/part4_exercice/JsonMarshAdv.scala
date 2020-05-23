package part4_exercice

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import part4_exercice.GameAreaMap.{AddPlayer, GetAllPlayers, GetPlayerByNickName, GetPlayersByClass, OperationSuccess, RemovePlayer, RemovePlayerByNick}
import spray.json._
import akka.pattern.ask

case class Player(nickName: String, characterClass: String, level: Int)

trait PlayerJsonProtocol extends DefaultJsonProtocol {
  implicit val playerJsn = jsonFormat3(Player)
}

object GameAreaMap {
  case object GetAllPlayers
  case class GetPlayerByNickName(nick: String)
  case class GetPlayersByClass(characterClass: String)
  case class AddPlayer(player: Player)

  case class RemovePlayer(player: Player)
  case class RemovePlayerByNick(nick: String)

  case object OperationSuccess
}


class GameAreaMap extends Actor with ActorLogging {

  var players: Map[String, Player] = Map()

  override def receive: Receive = {

    case GetAllPlayers => {
      log.info("getting all players")
      sender() ! players.values.toList
    }

    case GetPlayerByNickName(nick: String) => {
      log.info(s"getting player by nick $nick")
      sender() ! players.get(nick)
    }

    case GetPlayersByClass(charClass: String) => {
      log.info(s"getting players by class $charClass")
      sender() ! players.values.toList.filter(_.characterClass == charClass)
    }

    case AddPlayer(player: Player) => {
      log.info(s"adding new player $player")
      players = players + (player.nickName -> player)
      sender() ! OperationSuccess
    }

    case RemovePlayer(player: Player) => {
      log.info(s"removing player $player")
      players = players - player.nickName
      sender() ! OperationSuccess
    }

    case RemovePlayerByNick(nick: String) => {
      log.info(s"removing player by nick $nick")
      players = players - nick
      sender() ! OperationSuccess
    }
  }
}



object JsonMarshAdv extends App with PlayerJsonProtocol with SprayJsonSupport {

  implicit val system = ActorSystem("advJson")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  import akka.pattern.ask
  import scala.concurrent.duration._

  implicit val timeout = Timeout(1 second)

  val gameAreaMapActor = system.actorOf(Props[GameAreaMap], "gameAreaMap")



  val listInitialPlayers =
    List(
      Player("cintra", "worrior", 1),
      Player("daniel", "mage", 6),
      Player("Liza", "priest", 78)
    )
  listInitialPlayers.foreach(player => {
    gameAreaMapActor ! AddPlayer(player)
  })


  /**
    * 1 - GET /api/player -> all players
    * 2 - GET /api/player/nickname -> player with nicknamme
    * 3 - GET /api/player?nickname=x -> same
    * 4 - GET /api/player/class/(charClass) => all players with that class
    * 5 - POST /api/player with json payload
    * 5 - DELETE /api/player with json payload
    */

  import akka.http.scaladsl.server.Directives._

  val gameRoute = pathPrefix("api" / "player") {
    get {
      path("class" / Segment) { charClass =>

        val listOfPlayers = (gameAreaMapActor ? GetPlayersByClass(charClass)).mapTo[List[Player]]
        complete(listOfPlayers)
      } ~
      (path(Segment) | parameter('nickName.as[String])) { nickName =>

        val playerByNick = (gameAreaMapActor ? GetPlayerByNickName(nickName)).mapTo[Option[Player]]
        complete(playerByNick)

      } ~
      pathEndOrSingleSlash {

        val players = (gameAreaMapActor ? GetAllPlayers).mapTo[List[Player]]
        complete(players)
      }
    } ~
    post {
        entity(as[Player]) { player =>
          complete((gameAreaMapActor ? AddPlayer(player)).map(_ => StatusCodes.Created))
        }
    } ~
    delete {
      (parameter('nick.as[String]) | path(Segment)) { nick =>
        complete((gameAreaMapActor ? RemovePlayerByNick(nick)).map(_ => StatusCodes.Accepted))
      } ~
      entity(as[Player]) { player =>
          complete((gameAreaMapActor ? RemovePlayer(player)).map(_ => StatusCodes.Accepted))
      }
    }
  }

  Http().bindAndHandle(gameRoute, "localhost", 8082)

}
