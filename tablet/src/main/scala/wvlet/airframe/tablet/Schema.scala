/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.tablet

import java.util.Locale

import wvlet.airframe.tablet.Schema.RecordType
import wvlet.airframe.tablet.msgpack.MessageCodec

object Schema {
  sealed trait DataType {
    def signature: String
    def typeName: String
    def typeArgs: Seq[DataType]
    override def toString: String = signature
  }

  sealed abstract class PrimitiveType(val signature: String) extends DataType {
    def typeName                         = signature
    override def typeArgs: Seq[DataType] = Seq.empty
  }
  sealed trait NamedType extends DataType {
    def name: String
  }

  case object NIL       extends PrimitiveType("nil")
  case object INTEGER   extends PrimitiveType("int")
  case object FLOAT     extends PrimitiveType("float")
  case object BOOLEAN   extends PrimitiveType("boolean")
  case object STRING    extends PrimitiveType("string")
  case object TIMESTAMP extends PrimitiveType("timestamp")
  case object BINARY    extends PrimitiveType("binary")
  case object JSON      extends PrimitiveType("json")

  // Arbitrary types
  case object ANY extends DataType {
    override def signature: String       = "any"
    override def typeName: String        = "any"
    override def typeArgs: Seq[DataType] = Seq.empty
  }

  // Structure types
  case class ARRAY(elementType: DataType) extends DataType {
    override def signature               = s"array[${elementType.signature}]"
    override def typeName: String        = "array"
    override def typeArgs: Seq[DataType] = Seq(elementType)
  }
  case class MAP(keyType: DataType, valueType: DataType) extends DataType {
    override def typeName: String        = "map"
    override def signature               = s"map[${keyType.signature},${valueType.signature}]"
    override def typeArgs: Seq[DataType] = Seq(keyType, valueType)
  }

  /**
    * Union type represents a type whose data can be one of the specified types.
    * The members of a union type need to be record types.
    * @param types
    */
  case class UNION(types: Seq[RecordType]) extends DataType {
    override def typeName: String        = "union"
    override def signature: String       = s"union[${types.map(_.signature).mkString("|")}]"
    override def typeArgs: Seq[DataType] = types
  }

  /**
    *
    *
    *
    * @param column
    */
  case class RecordType(typeName: String, column: Seq[Column]) extends DataType {
    // Person(id:int, name:string, address:Address)
    // Address(address:string, phone:array[string])
    override def signature               = s"${typeName}(${column.map(_.signature).mkString(",")})"
    override def typeArgs: Seq[DataType] = Seq.empty

    def size: Int = column.length
    // 0-origin index
    @transient private lazy val columnIdx: Map[Column, Int] = column.zipWithIndex.toMap[Column, Int]

    /**
      * @param index 0-origin index
      * @return
      */
    def columnType(index: Int) = column(index)

    /**
      * Return the column index
      *
      * @param column
      * @return
      */
    def columnIndex(column: Column) = columnIdx(column)
  }

  case class Column(name: String, columnType: Schema.DataType) {
    def signature = s"${name}:${columnType}"
  }

  case object DataType {
    def unapply(s: String): Option[DataType] = {
      val tpe = s.toLowerCase(Locale.US) match {
        case "nil" | "null"                => NIL
        case "varchar" | "string" | "text" => STRING
        case "bigint" | "integer"          => INTEGER
        case "boolean"                     => BOOLEAN
        case "float" | "double"            => FLOAT
        case "timestamp"                   => TIMESTAMP
        case "json"                        => JSON // TODO use JSON type
        case _                             => STRING // TODO support more type
      }
      Some(tpe)
    }
  }

  object SchemaCodec extends MessageCodec[Schema] {}
  o
}

case class Schema(recordTypes: Seq[RecordType]) {
  override def toString = s"Schema(${recordTypes.mkString(", ")})"
}
