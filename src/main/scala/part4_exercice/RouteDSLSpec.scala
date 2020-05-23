package part4_exercice

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import spray.json._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

case class Book(id: Int, author: String, title: String)

trait BookJsonProtocol extends DefaultJsonProtocol {
  implicit val bookJsonFormatter = jsonFormat3(Book)
}

class RouteDSLSpec extends WordSpec with Matchers with ScalatestRouteTest with BookJsonProtocol {

  import RouteDSLSpec._

  "a digital library backend" should {

    "return all books in the lib" in {
      // send an http request
      // inspec responses
      Get("/api/book") ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK
        entityAs[List[Book]] shouldBe books
      }
    }

    "return a book if query parameter provided" in {
      Get("/api/book?id=1") ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK
        entityAs[Book] shouldBe Book(1, "JRR", "LOTR")
      }
    }

    "return the same book if the other option is used" in {
      Get("/api/book/1") ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK
        entityAs[Book] shouldBe Book(1, "JRR", "LOTR")
      }
    }

    "register a new book via the POST path" in {
      val newBook = Book(5, "Jane", "Pride")
      Post("/api/book", newBook) ~> libraryRoute ~> check {
        status shouldBe StatusCodes.Created
        books should contain(newBook)
      }
    }

    "should not accept POST without a book in it" in {
      Post("/api/book") ~> libraryRoute ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "should fetch all books from the provided author" in {
      Get("/api/book/author/JRR") ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK
        entityAs[List[Book]] shouldBe List(Book(1, "JRR", "LOTR"))
      }
    }
    "should accept the query notation" in {
      Get("/api/book?author=JRR") ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK
        entityAs[List[Book]] shouldBe List(Book(1, "JRR", "LOTR"))
      }
    }

    "should not accept other verbs than GET and POST" in {
      Delete("/api/book") ~> libraryRoute ~> check {
        rejections should not be empty
      }
      Patch("/api/book") ~> libraryRoute ~> check {
        rejections should not be empty
      }
      Put("/api/book") ~> libraryRoute ~> check {
        rejections should not be empty
      }
    }
  }

}

object RouteDSLSpec extends BookJsonProtocol with SprayJsonSupport{

  var books = List(Book(1, "JRR", "LOTR"),
    Book(2, "Martin", "Game of thrones"))

  val libraryRoute =
    pathPrefix("api" / "book") {
      (path("author" / Segment) | parameter('author.as[String]) & get) { author =>
          complete(books.filter(_.author == author))
      } ~
      get {
        (path(IntNumber) | parameter('id.as[Int])) { id =>
          complete(books.find(_.id == id))
        } ~
        pathEndOrSingleSlash {
          complete(books)
        }
      } ~
      post {
        entity(as[Book]) { bk:Book =>
          books = books :+ bk
          complete(StatusCodes.Created)
        } ~
        complete(StatusCodes.BadRequest)
      }
    }
}
