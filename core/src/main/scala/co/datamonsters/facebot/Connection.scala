package co.datamonsters.facebot

import co.datamonsters.facebot.api._
import co.datamonsters.facebot.exceptions.TokenValidationException
import pushka.json.{parser, printer}

import scala.language.higherKinds
import scala.util.Try

abstract class Connection[+F[_]](botsCredentials: Map[String,Credentials], eventHandler: EventHandler[F]) extends SendApi[F] {

  val apiVersion = "v2.7"

  /**
    * Send request to platform
    *
    * @return response
    */
  def send[R](botName: String,
              endpoint: String,
              params: Map[String, String],
              bodyOpt: Option[String],
              parseResponse: String => R): F[R]

  def receiveVerify(botName: String, params: Map[String, Seq[String]]): Try[String] = Try {
    if (params.get("hub.mode").contains(Seq("subscribe")) && params.contains("hub.challenge")) {
      val credentials = botsCredentials(botName)
      params.get("hub.verify_token") match {
        case None => throw TokenValidationException("verify_token is not defined")
        case Some(Seq(credentials.verifyToken)) => params("hub.challenge").head
        case Some(xs) =>
          val message = s"verify_token is `${xs.mkString}` but `${credentials.verifyToken}` expected"
          throw TokenValidationException(message)
      }
    } else {
      val message = "Bad request. See https://developers.facebook.com/docs/graph-api/webhooks"
      throw TokenValidationException(message)
    }
  }

  /**
    * Take data from web hook
    * @param body String body
    * @return Response to platform
    */
  def receive(botName: String, body: String): Try[Seq[F[_]]] = Try {
    val webHook = pushka.json.read[Webhook](body)
    val events = webHook.entry flatMap { entry =>
      entry.messaging map { messaging =>
        new Event(this, botName, entry.id, entry.time, messaging)
      }
    }
    def loop(acc: List[F[_]], list: List[Event[F]]): List[F[_]] = list match {
      case Nil => acc
      case x :: xs => eventHandler.lift(x) match {
        case Some(f) => loop(f :: acc, xs)
        case None => loop(acc, xs)
      }
    }
    loop(Nil, events.toList).reverse
  }

  private def messagesRequestResponse(botName: String, req: Request): F[Response] = {
    val json = pushka.json.write(req)
    send(botName,"me/messages", Map.empty, Some(json), json => pushka.json.read[Response](json))
  }

  def sendMessage(botName: String, recipient: Id, message: Message,
      notificationType: NotificationType = NotificationType.Regular): F[Response] = {
    val req = Request(recipient, Some(message), None, Some(notificationType.value))
    messagesRequestResponse(botName, req)
  }

  def typingOn(botName: String, recipient: Id): F[Response] = {
    val req = Request(recipient, None, Some("typing_on"), None)
    messagesRequestResponse(botName, req)
  }

  def typingOff(botName: String, recipient: Id): F[Response] = {
    val req = Request(recipient, None, Some("typing_off"), None)
    messagesRequestResponse(botName, req)
  }

  def markSeen(botName: String, recipient: Id): F[Response] = {
    val req = Request(recipient, None, Some("mark_seen"), None)
    messagesRequestResponse(botName, req)
  }

  def profile(botName: String, userId: Id): F[UserInfo] = {
    val params = Map("fields" -> "first_name,last_name,profile_pic,locale,timezone,gender")
    send(botName, userId.value, params, None, json => pushka.json.read[UserInfo](json))
  }
}
