package part4_exercice

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.util.Timeout
import part4_exercice.PaymentSystemDomain.{PaymentAccept, PaymentReject, PaymentRequest}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class CreditCard(serial: String, secCode: String, senderAcc: String)

object PaymentSystemDomain {
  case class PaymentRequest(creditCard: CreditCard, receiverAcc: String, amount: BigDecimal)
  case object PaymentAccept
  case object PaymentReject
}
import spray.json._
trait PaymentsJsonProtocol extends DefaultJsonProtocol {
  implicit val creditCardFormat = jsonFormat3(CreditCard)
  implicit val paymentRequestFormat = jsonFormat3(PaymentRequest)
}

class PaymentValidator extends Actor with ActorLogging {
  override def receive: Receive = {
    case PaymentRequest(creditCard: CreditCard, reciverAcc, amount) => {
      log.info(s"the sender acc is trying to send $amount to $reciverAcc")
      if(creditCard.serial == "1234-1234-1234-1234") sender() ! PaymentReject
      else sender() ! PaymentAccept
    }
  }
}


object PaymentSystem extends App with PaymentsJsonProtocol with SprayJsonSupport{

  implicit val system = ActorSystem("paymentSystem")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  import scala.concurrent.duration._
  import akka.http.scaladsl.server.Directives._
  import akka.pattern.ask
  import PaymentSystemDomain._

  implicit val timeout = Timeout(1 second)

  val paymentValidatorActor = system.actorOf(Props[PaymentValidator], "paymenactor")

  val paymentRoute = path("api" / "payments") {
    post {
      entity(as[PaymentRequest]) { paymentRequest =>
        val validationResponse = (paymentValidatorActor ? paymentRequest).map {
          case PaymentReject => StatusCodes.Forbidden
          case PaymentAccept => StatusCodes.OK
          case _ => StatusCodes.BadRequest
        }

        complete(validationResponse)
      }
    }
  }

  Http().bindAndHandle(paymentRoute, "localhost", 8080)

}
