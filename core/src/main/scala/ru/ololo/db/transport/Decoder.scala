package ru.ololo.db.transport

trait Decoder[From, To] {
  def decode(from: From): To
}

object Decoder {
  def apply[From, To](implicit F: Decoder[From, To]): Decoder[From, To] = F
}
