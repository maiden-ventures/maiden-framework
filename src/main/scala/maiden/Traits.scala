package maiden.traits

import java.time.LocalDateTime

trait MaidenEncoder extends Product

trait MaidenModel extends MaidenEncoder {
  val id: Option[Long]
  val createdAt: LocalDateTime
  val updatedAt: LocalDateTime

}

trait MaidenFullResponse extends MaidenModel

trait MaidenModelObject[M <: MaidenModel, MF <: MaidenFullResponse] {

  def exists(id: Long): Boolean
  def get(id: Long): MF
  def findById(id: Long): List[MF]
  def deleteById(id: Long): Long
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
