package ru.ololo.db

package object broadcast {

  case class Message(key: String, value: String, version: Long)

  sealed trait Result

  object Result {
    case class Success() extends Result
    case class Failure(message: String) extends Result
  }

}
