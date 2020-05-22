package part4_exercice

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, RequestEntity, StatusCodes}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import part4_exercice.PersonDB.{AllPeople, FindPerson, NewPerson, NewPersonComplete, PersonCreated}
import spray.json._

import scala.concurrent.Future

class PersonActions(personDB: ActorRef, system: ActorSystem, materializer: ActorMaterializer)
  extends PersonJsonProtocol {


  import system.dispatcher
  import scala.concurrent.duration._
  import akka.pattern.ask

  implicit val timeout = Timeout(3 second)
  implicit val myMaterializer = materializer

  def createPerson(entity: RequestEntity): Future[HttpResponse] = {
    entity.toStrict(3 second).flatMap { strictEntity =>
      val person: Person = strictEntity.data.utf8String.parseJson.convertTo[Person]
      (personDB ? NewPersonComplete(person)).map(_ => {
        HttpResponse(StatusCodes.Created)
      })
    }

  }

  def getAllPeople(): Future[HttpResponse] = {
    getAllPeopleJson()
      .map(ent => HttpResponse(StatusCodes.OK, entity = ent))
  }

  def getPersonByPin(pin: Int): Future[HttpResponse] = {
    getPersonAsJson(pin)
      .map(HttpEntity(ContentTypes.`application/json`,_))
      .map(entity => HttpResponse(StatusCodes.OK, entity = entity))
  }

  def getPersonAsJson(pin: Int): Future[String] = {
    (personDB ? FindPerson(pin))
      .mapTo[Option[Person]].map {
        _.toJson.prettyPrint
    }
  }

  def getAllPeopleJson() = {
    (personDB ? AllPeople).mapTo[List[Person]].map {
      _.toJson.prettyPrint
    }.map(HttpEntity(ContentTypes.`application/json`, _))
  }


}
