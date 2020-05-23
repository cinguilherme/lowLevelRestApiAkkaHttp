package part4_exercice

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import part4_exercice.PersonDB.NewPerson

object HighLevelExercice extends App {

  implicit val system = ActorSystem("govermentRegistration")
  implicit val materializer = ActorMaterializer()

  // citizends
  val personDb = system.actorOf(Props[PersonDB], "peopleDB")
  val personActions = new PersonActions(personDb, system, materializer)

  personDb ! NewPerson("Amadeus", 1)
  personDb ! NewPerson("Zelda", 2)
  // Citizen number 1

  import akka.http.scaladsl.server.Directives._

  val personRoutes = (pathPrefix("api" / "people") | pathPrefix("api" / "people" / "v1")) {
    (parameter('pin.as[Int]) | path(IntNumber)) { pin =>
      get {
        complete(personActions.getPersonByPin(pin))
      }
    } ~
    get {
      complete(personActions.getAllPeople())
    } ~
    (post & extractRequest) { request =>
      complete(personActions.createPerson(request.entity))
    }
  }

  Http().bindAndHandle(personRoutes, "localhost", 8088)
}
