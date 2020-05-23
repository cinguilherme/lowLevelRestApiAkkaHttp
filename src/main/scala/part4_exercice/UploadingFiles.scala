package part4_exercice

import java.io.File

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.{ByteString, Timeout}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object UploadingFiles extends App {

  implicit val system = ActorSystem("uploadingFiles")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  type filePart = Multipart.FormData.BodyPart

  val route =
    (pathEndOrSingleSlash & get) {
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,
      """
        |<html>
        |<body>
        |<form action="http://localhost:8080/upload" method="post" enctype="multipart/form-data">
        |<input type="file" name="myFile">
        |<button type="submit">Upload</button>
        |</input>
        |</form>
        |</body>
        |</html>
      """.stripMargin))
    } ~
    (path("upload") & extractLog) { log =>
      //handle uploading files
      // multipart/form-data
      entity(as[Multipart.FormData]) { data =>
        // data is the file payload
        val partsSource: Source[filePart, Any] = data.parts
        val fileSink: Sink[filePart, Future[Done]] = Sink.foreach[filePart] { part =>
          if(part.name == "myFile") {
            // create a file to dump the bytes
            val filePath = "src/main/resources/download/" +
              part.filename.getOrElse("tempfile_"+System.currentTimeMillis())
            val file = new File(filePath)

            log.info(s"writing to file $filePath")

            val fileContentsSource: Source[ByteString, _] = part.entity.dataBytes
            val fileContentsSink: Sink[ByteString, _] = FileIO.toPath(file.toPath)
            fileContentsSource.runWith(fileContentsSink)

          }
        }

        import scala.concurrent.duration._
        implicit val timeout = Timeout(5 second)
        val writeOperationFuture = partsSource.runWith(fileSink)

        onComplete(writeOperationFuture) {
          case Success(value) => complete("file uploaded")
          case Failure(exception) => complete("file failed to upload")
        }
      }
    }

  Http().bindAndHandle(route, "localhost", 8080)

}
