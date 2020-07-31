package ru.ololo.db.storage

trait DataStorage[F[_]] {
  def get(key: String): F[Option[Row]]
  def put(key: String, value: String): F[Row]
  def cas(key: String, oldValue: String, newValue: String): F[(Row, Boolean)]
}

object DataStorage {
  def apply[F[_]](implicit F: DataStorage[F]): DataStorage[F] = F
}
