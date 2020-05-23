package part4_exercice

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import part4_exercice.PaymentSystemDomain.PaymentRequest
import spray.json._

object ConnectionLevel extends App with PaymentsJsonProtocol with SprayJsonSupport{

  implicit val system = ActorSystem("paymentSystem")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  import scala.concurrent.duration._
  import akka.http.scaladsl.server.Directives._
  import akka.pattern.ask


  val cards = List(
    CreditCard("1234-1234-1234-1234", "543", "acc1"),
    CreditCard("1234-1234-1234-1234", "549", "acc2"),
    CreditCard("1234-4242-4242-4242", "513", "acc3"),
  )
  val paymentsBatch = cards.map(c => PaymentRequest(c, "mystoreacc", 100))

  val requests = paymentsBatch
    .map(p => HttpRequest(
      HttpMethods.POST,
      uri = Uri("/api/payments"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        p.toJson.prettyPrint)))

  Source(requests)
    .via(Http().outgoingConnection("localhost", 8080))
    .to(Sink.foreach[HttpResponse](println)).run()

}
