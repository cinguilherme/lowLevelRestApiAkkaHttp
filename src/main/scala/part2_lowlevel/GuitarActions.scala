package part2_lowlevel

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.model.Uri.Query
import part2_lowlevel.GuitarDB.{CreateGuitar, FindAllGuitars, FindGuitar}
import part2_lowlevel.LowLevelREST.{guitarDbActor, system, materializer}

import scala.concurrent.Future
import spray.json._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._


class GuitarActions extends GuitarStoreJsonProtocol  {

  implicit val executionContext = system.dispatchers.lookup("my-dispatcher")
  implicit val timeout = Timeout(3 seconds)

  def queryGuitars(query: Query): Future[HttpResponse] = {

    val guitarId = query.get("id").map(_.toInt)
    guitarId match {
      case None => Future(HttpResponse(StatusCodes.BadRequest))
      case Some(id) =>
        val gui: Future[Option[Guitar]] = (guitarDbActor ? FindGuitar(id)).mapTo[Option[Guitar]]
        gui.map {
          case None => HttpResponse(StatusCodes.NotFound)
          case Some(guitar) => HttpResponse(StatusCodes.OK,
            entity = HttpEntity(ContentTypes.`application/json`, guitar.toJson.prettyPrint))
        }
    }

  }

  def createNewGuitar(entity: HttpEntity): Future[HttpResponse] = {
    val strict = entity.toStrict(3 seconds)
    strict.flatMap { strictEntity =>
      val guitar = strictEntity.data.utf8String.parseJson.convertTo[Guitar]
      (guitarDbActor ? CreateGuitar(guitar)).map { any =>
        HttpResponse(StatusCodes.OK)
      }
    }
  }

  def allGuitars() = {
    val guitarsFuture: Future[List[Guitar]] = (guitarDbActor ? FindAllGuitars).mapTo[List[Guitar]]
    guitarsFuture.map { guitars =>
      HttpResponse(
        entity = HttpEntity(
          ContentTypes.`application/json`,
          guitars.toJson.prettyPrint
        )
      )
    }
  }

}
