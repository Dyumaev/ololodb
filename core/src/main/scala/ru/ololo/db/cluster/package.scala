package ru.ololo.db

package object cluster {

  case class Heartbeat(from: String, to: String)

  sealed trait Result

  object Result {
    case class Success() extends Result
    case class Failure(message: String) extends Result
  }

}
