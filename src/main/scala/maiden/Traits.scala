package maiden.traits


trait MaidenEncoder extends Product
trait MaidenModel extends MaidenEncoder
trait WithApi
trait WithAdmin
trait WithAuthorization
trait MaidenFullResponse extends MaidenEncoder

//some helper traits
//these are not actually used right now
trait ApiModel extends MaidenModel with WithApi
trait AdminableModel extends MaidenModel with WithApi with WithAdmin
trait AuthorizedModelApi extends MaidenModel with WithApi with WithAuthorization
