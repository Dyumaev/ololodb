package ru.ololo.db

package object storage {

  case class Row(key: String, value: String, version: Long)

}
