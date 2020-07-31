package ru.ololo.db.transport

trait Encoder[From, To] {
  def encode(from: From): To
}

object Encoder {
  def apply[From, To](implicit F: Encoder[From, To]): Encoder[From, To] = F
}
