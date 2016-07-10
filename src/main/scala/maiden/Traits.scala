package maiden.traits

import java.time.LocalDateTime

trait MaidenEncoder extends Product

trait MaidenModel extends MaidenEncoder {
  val id: Long
  val createdAt: LocalDateTime
  var updatedAt: LocalDateTime
}

trait WithApi
trait WithAdmin
trait WithAuthorization
trait MaidenFullResponse extends MaidenModel

//some helper traits
//these are not actually used right now
trait ApiModel extends MaidenModel with WithApi
trait AdminableModel extends MaidenModel with WithApi with WithAdmin
trait AuthorizedModelApi extends MaidenModel with WithApi with WithAuthorization
