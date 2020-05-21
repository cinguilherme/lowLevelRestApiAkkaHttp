package part2_lowlevel


import akka.actor.{Actor, ActorLogging}

case class Guitar(make: String, model: String, stock: Int)

object GuitarDB {
  case class CreateGuitar(guitar: Guitar)
  case class GuitarCreated(id: Int)
  case class FindGuitar(id: Int)
  case object FindAllGuitars

  case class GetInventory(stock: Boolean)
}


import spray.json._

trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {
  implicit val guitarFormat = jsonFormat3(Guitar)
}

class GuitarDB extends Actor with ActorLogging {
  import GuitarDB._

  var guitars: Map[Int, Guitar] = Map()
  var currentGuitarId = 0

  override def receive: Receive = {

    case FindAllGuitars => {
      log.info("searching for all guitars")
      sender() ! guitars.values.toList
    }
    case FindGuitar(id) => {
      log.info(s"searching for guitars with id ${id}")
      sender() ! guitars.get(id)
    }
    case CreateGuitar(guitar: Guitar) => {
      log.info(s"creating guitar with $currentGuitarId, this guitar $guitar")
      guitars = guitars + (currentGuitarId -> guitar)
      sender() ! GuitarCreated
      currentGuitarId += 1
    }
    case GetInventory(stock) => {
      val pred:(Int => Boolean) = if(stock == false) ((z:Int) => z == 0) else ((x:Int) => x > 0)

      val res = guitars.values.toList.filter(gt => pred(gt.stock))
      sender() ! res
    }
  }
}
