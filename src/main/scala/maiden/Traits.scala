package maiden.traits

import java.time.LocalDateTime
import com.twitter.util.Future

trait MaidenEncoder extends Product

trait MaidenModel extends MaidenEncoder {
  val id: Long
  val createdAt: Option[LocalDateTime]
  val updatedAt: Option[LocalDateTime]
}

trait MaidenModelWithoutTimestamps {
  val id: Long
}

trait MaidenFullResponse extends MaidenModel

trait MaidenWithoutTimestampsFullResponse extends MaidenModelWithoutTimestamps

trait MaidenModelObject[M <: MaidenModel] {

  def exists(id: Long): Boolean
  def get(id: Long): M
  def delete(id: Long): Long
  def create(data: M): M
  def update(data: M): M
}

trait WithApi
trait WithAdmin
trait WithAuthorization

//some helper traits
//these are not actually used right now
trait ApiModel extends MaidenModel with WithApi
trait AdminableModel extends MaidenModel with WithApi with WithAdmin
trait AuthorizedModelApi extends MaidenModel with WithApi with WithAuthorization
