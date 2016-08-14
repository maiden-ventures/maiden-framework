package maiden.traits

import java.time.LocalDateTime
import com.twitter.util.Future

trait MaidenEncoder extends Product

trait MaidenModel extends MaidenEncoder {
  val id: Option[Long]
  val createdAt: Option[LocalDateTime]
  val updatedAt: Option[LocalDateTime]

}

trait MaidenModelWithoutTimestamps {
  val id: Option[Long]
}

trait MaidenFullResponse extends MaidenModel

trait MaidenWithoutTimestampsFullResponse extends MaidenModelWithoutTimestamps

trait MaidenModelObject[M <: MaidenModel, MF <: MaidenFullResponse] {

  def exists(id: Long): Boolean
  def get(id: Long): MF
  def findBy[T](col: String, value: T) : List[MF]
  def deleteBy[T](col: String, value: T): Long
  //def getRangeBy[T](col: String, start: T, end: T): List[MF]
  def create(data: M): Long
  def update(data: M): MF
}

trait WithApi
trait WithAdmin
trait WithAuthorization

//some helper traits
//these are not actually used right now
trait ApiModel extends MaidenModel with WithApi
trait AdminableModel extends MaidenModel with WithApi with WithAdmin
trait AuthorizedModelApi extends MaidenModel with WithApi with WithAuthorization
