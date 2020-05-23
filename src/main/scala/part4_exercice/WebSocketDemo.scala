package part4_exercice

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.CompactByteString

object WebSocketDemo extends App {

  implicit val system = ActorSystem("webSocketSystem")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val textMessage = TextMessage(Source.single("Hellow"))
  val binaryMessage = BinaryMessage(Source.single(CompactByteString("Hello")))


  val html =
    """
      |<html><head><script>
      |
      |        var exampleSocket = new WebSocket("ws://localhost:8080/greeter");
      |
      |        console.log("starting websocket");
      |
      |        exampleSocket.onmessage = function(event) {
      |            var newc = document.createElement("div");
      |            newc.innerText = event.data
      |            document.getElementById("1").appendChild(newc);
      |        }
      |
      |       exampleSocket.onopen = function(event) {
      |           exampleSocket.send("testing 1");
      |           exampleSocket.send("testing 2");
      |           exampleSocket.send("testing 3");
      |       }
      |
      |
      |    </script></head><body>
      |<div>websocket sample</div>
      |
      |<div id="1">
      |
      |</div></body></html>
    """.stripMargin

  def websocketFlow: Flow[Message, Message, Any] = Flow[Message].map {
    case tm:TextMessage =>
      println("handling text message")
      TextMessage(Source.single("server says back") ++ tm.textStream ++ Source.single("!"))
    case bm:BinaryMessage =>
      println("handling binary message")
      bm.dataStream.runWith(Sink.ignore)
      TextMessage(Source.single("server received a binary message"))
  }

  import akka.http.scaladsl.server.Directives._
  val webSocketRoute =
    (pathEndOrSingleSlash & get) {
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, html))
    } ~
  path("greeter") {
    println("handling websocket connection")
    handleWebSocketMessages(socialFlow)
  }

  Http().bindAndHandle(webSocketRoute, "localhost", 8080)


  case class SocialPost(owner: String, content: String)

  val socialFeed = Source(
    List(
      SocialPost("Daniel", "New course is available"),
      SocialPost("Martin", "I killed Java"),
      SocialPost("Fred", "I like big things"),
      SocialPost("Martin", "I really like small things")
    )
  )

  import scala.concurrent.duration._

  val socialX = socialFeed
    .throttle(1, 2 second)
    .map(socialPost => TextMessage(s"Social post ${socialPost.owner} saying ${socialPost.content}"))

  val socialFlow: Flow[Message, Message, Any] = Flow.fromSinkAndSource(
    Sink.foreach[Message](println),
    socialX
  )

}
