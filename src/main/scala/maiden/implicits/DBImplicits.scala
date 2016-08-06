package maiden.implicits


import java.sql.{PreparedStatement, Types}
import io.getquill.context.BindedStatementBuilder
import io.getquill.JdbcContext

trait DBImplicits {
  this: JdbcContext[_,_] =>

  val SqlNull = quote(infix"null")

  implicit class ForUpdate[T](q: Query[T]) {
    def forUpdate = quote(infix"$q FOR UPDATE".as[Query[T]])
  }

  //this is an override for quill until it supports inserting Option[LocalDateTime]

  private[this] val nullEncoder = encoder[Int](_.setNull)

  override implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    new Encoder[Option[T]] {
      override def apply(idx: Int, value: Option[T], row: BindedStatementBuilder[PreparedStatement]) =
        value match {
          case Some(value) => d(idx, value, row)
          case None => {
            import Types._
            val sqlType =
              d match {
                case `stringEncoder`     => VARCHAR
                case `bigDecimalEncoder` => NUMERIC
                case `booleanEncoder`    => BOOLEAN
                case `byteEncoder`       => TINYINT
                case `shortEncoder`      => SMALLINT
                case `intEncoder`        => INTEGER
                case `longEncoder`       => BIGINT
                case `floatEncoder`      => REAL
                case `doubleEncoder`     => DOUBLE
                case `byteArrayEncoder`  => VARBINARY
                case `dateEncoder`       => TIMESTAMP
                case _                   => TIMESTAMP
              }
            nullEncoder(idx, sqlType, row)
        }}
    }
}
