package part2_lowlevel

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.model.Uri.Query
import part2_lowlevel.GuitarDB.{CreateGuitar, FindAllGuitars, FindGuitar, GetInventory, SetInventory}
import part2_lowlevel.Guitar
import part3_highlevel.HighLevelExample.{materializer, system, guitarDbActor}

import scala.concurrent.Future
import spray.json._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._


class GuitarActions extends GuitarStoreJsonProtocol  {

  //implicit val executionContext = system.dispatchers.lookup("my-dispatcher")
  import system.dispatcher
  implicit val timeout = Timeout(3 seconds)

  def inventory(stock: Boolean): Future[HttpResponse] = {
    getStock(stock)
  }

  def inventory(uri: Uri): Future[HttpResponse] = {
    val query: Query = uri.query()
    if(query.isEmpty){
      getStock(true)
    } else {
      val st = query.get("inStock").map(_.toBoolean).get
      println(st)
      getStock(st)
    }
  }

  def setInventory(uri: Uri): Future[HttpResponse] = {

    val query: Query = uri.query()

    if(query.isEmpty){
      Future(HttpResponse(StatusCodes.BadRequest))
    } else {

      val idOpt:Option[Int] = query.get("id").map(_.toInt)
      val quantityOpt:Option[Int] = query.get("quantity").map(_.toInt)

      val finalRes = for {
        id <- idOpt
        quantity <- quantityOpt
      } yield {
        setStock(id, quantity)
      }
      finalRes.getOrElse(Future(HttpResponse(StatusCodes.BadRequest)))
    }
  }

  def setStock(id: Int, quantity: Int) = {
    (guitarDbActor ? SetInventory(id, quantity)).map { va =>
      HttpResponse(StatusCodes.OK)
    }
  }

  def getStock(stock: Boolean) = {
    (guitarDbActor ? GetInventory(stock)).mapTo[List[Guitar]].map( list =>
      HttpResponse(StatusCodes.OK,
        entity = HttpEntity(ContentTypes.`application/json`,
          list.toJson.prettyPrint)))
  }

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

  def guitarById(id: Int) = {
    println("guitar by id evoked")
    val gui: Future[Option[Guitar]] = (guitarDbActor ? FindGuitar(id)).mapTo[Option[Guitar]]
    gui.map {
      case None => HttpResponse(StatusCodes.NotFound)
      case Some(guitar) => HttpResponse(StatusCodes.OK,
        entity = HttpEntity(ContentTypes.`application/json`, guitar.toJson.prettyPrint))
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
