package ru.ololo.db.utils

object Id {

  object Tag {

    def apply[U] = new Tagger[U]

    trait Tagged[U]
    type @@[+T, U] = T with Tagged[U]

    class Tagger[U] {
      def apply[T](t: T): T @@ U = t.asInstanceOf[T @@ U]
    }

  }

  object Id {

    import Tag._

    type Id[T] = String @@ T

    def apply[T](id: String): Id[T] = Tag[T][String](id)

  }

}
