package ru.ololo.db.storage

import java.nio.ByteBuffer

import org.rocksdb.{ReadOptions, WriteOptions}

package object rocksdb {

  val INCREMENT = LongSerializer.serialize(1)
  val DEFAULT_WRITE_OPTIONS = new WriteOptions()
  val DEFAULT_READ_OPTIONS = new ReadOptions()

  object LongSerializer {

    def serialize(long: Long): Array[Byte] = {
      val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
      buffer.putLong(long)
      buffer.array()
    }

    def deserialize(bytes: Array[Byte]): Long = {
      val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
      buffer.put(bytes)
      buffer.flip()
      buffer.getLong
    }

  }

}
