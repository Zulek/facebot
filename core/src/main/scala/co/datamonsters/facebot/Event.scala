package co.datamonsters.facebot

import co.datamonsters.facebot.api.{Response, _}

import scala.language.higherKinds

class Event[+F[_]](val sendApi: SendApi[F],
                   val botName: String,
                   val entryId: String,
                   val entryTime: Long,
                   val messaging: Messaging)
    extends SendApi[F] {

  val reply: (Message, NotificationType) => F[Response] = sendMessage(botName, messaging.sender, _, _)

  def sendMessage(botName: String, recipient: Id,
                  message: Message,
                  notificationType: NotificationType = NotificationType.Regular): F[Response] =
    sendApi.sendMessage(botName, recipient, message, notificationType)

  def typingOn(botName: String, recipient: Id): F[Response] =
    sendApi.typingOn(botName, recipient)

  def markSeen(botName: String, recipient: Id): F[Response] =
    sendApi.markSeen(botName, recipient)

  def typingOff(botName: String, recipient: Id): F[Response] =
    sendApi.typingOff(botName, recipient)

  def profile(botName: String, userId: Id): F[UserInfo] =
    sendApi.profile(botName, userId)
}

object Event {
  def unapply[F[_]](arg: Event[F]): Option[(String, Messaging)] =
    Some((arg.botName, arg.messaging))
}
