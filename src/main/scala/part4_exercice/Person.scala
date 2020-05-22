package part4_exercice

  import akka.actor.{Actor, ActorLogging}
  import part4_exercice.PersonDB.{AllPeople, FindPerson, NewPerson, NewPersonComplete, PersonCreated}
  import spray.json._

case class Person(pin: Int, name: String)

trait PersonJsonProtocol extends DefaultJsonProtocol {
  implicit val personJsonFormat = jsonFormat2(Person)
}

object PersonDB {
  case class NewPerson(name: String, pin: Int)
  case class NewPersonComplete(person: Person)
  object PersonCreated
  object AllPeople
  case class FindPerson(pin: Int)
}

class PersonDB extends Actor with ActorLogging {

  var peopleDb:Map[Int, Person] = Map()

  override def receive: Receive = {

    case NewPerson(name:String, pin:Int) => {
      val person = Person(pin, name)
      peopleDb = peopleDb + (pin -> person)
      sender() ! PersonCreated
    }
    case NewPersonComplete(person) => {
      peopleDb = peopleDb + (person.pin -> person)
      sender() ! PersonCreated
    }

    case AllPeople => sender() ! peopleDb.values.toList

    case FindPerson(pin: Int) => sender() ! peopleDb.get(pin)
  }
}