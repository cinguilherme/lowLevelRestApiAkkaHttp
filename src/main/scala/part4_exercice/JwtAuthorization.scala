package part4_exercice

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson}
import spray.json._

import scala.util.{Failure, Success}

object SecurityDomain extends DefaultJsonProtocol {
  case class LoginRequest(username: String, password: String)
  implicit val loginRequestFormat = jsonFormat2(LoginRequest)
}

object JwtAuthorization extends App with SprayJsonSupport{

  implicit val system = ActorSystem("jwt")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  import SecurityDomain._

  val superPassWord = Map(
    "admin" -> "123",
    "daniel" -> "123"
  )

  val currentTimeContext = System.currentTimeMillis() / 1000
  val alg = JwtAlgorithm.HS256

  val secretKey = "cinguilherme"

  def checkPassword(user: String, pass: String): Boolean = {
    superPassWord
      .filter((m:(String, String)) => m._1 == user && m._2 == pass)
      .nonEmpty
  }

  def createToken(str: String, expirationPeriod: Int): String = {
    val claims = JwtClaim(
      expiration = Some(currentTimeContext + TimeUnit.DAYS.toMillis(expirationPeriod)),
      issuedAt = Some(currentTimeContext),
      issuer = Some("cinguilherme")
    )
    JwtSprayJson.encode(claims, secretKey, alg) // JWT String
  }

  def tokenExpired(token: String): Boolean = {
    JwtSprayJson.decode(token, secretKey, Seq(alg)) match {
      case Success(claims) => claims.expiration.getOrElse(0L) < currentTimeContext
      case Failure(_) => true
    }
  }

  def tokenValid(token: String): Boolean = {
    JwtSprayJson.isValid(token, secretKey, Seq(alg))
  }


  val login =
    post {
      entity(as[LoginRequest]) {
        case LoginRequest(user, pass) if(checkPassword(user, pass)) => {
          val token = createToken(user, 1)
          respondWithHeader(RawHeader("Access-Token", token)) {
            complete(StatusCodes.OK)
          }
        }
        case _ => complete(StatusCodes.Unauthorized)

      }
    }

  val authenticatedRoute = (path("secure") & get ) {
    optionalHeaderValueByName("Authorization") {
      case Some(token) if(tokenValid(token)) => {
        if(tokenExpired(token)) {
          complete(HttpResponse(StatusCodes.Unauthorized, entity = "Token expired"))
        } else {
          complete("user access authorized endpoint")
        }
      }
      case _ => complete(HttpResponse(StatusCodes.Unauthorized, entity = "token is invalid"))

    }
  }

  val route = login ~ authenticatedRoute

  Http().bindAndHandle(route, "localhost", 8080)

}
