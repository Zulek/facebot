package co.datamonsters.facebot.api

import scala.language.higherKinds

trait SendApi[+F[_]] {

  def sendMessage(botName: String, recipient: Id, message: Message,
    notificationType: NotificationType = NotificationType.Regular): F[Response]

  def typingOn(botName: String, recipient: Id): F[Response]

  def typingOff(botName: String, recipient: Id): F[Response]

  def markSeen(botName: String, recipient: Id): F[Response]

  def profile(botName: String, userId: Id): F[UserInfo]
}


