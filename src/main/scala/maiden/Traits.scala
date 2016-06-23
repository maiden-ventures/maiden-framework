package maiden.traits


trait MaidenModel
trait WithApi
trait WithAdmin
trait WithAuthorization

//some helper traits

trait ApiModel extends MaidenModel with WithApi
trait AdminableModel extends MaidenModel with WithApi with WithAdmin
trait AuthorizedModelApi extends MaidenModel with WithApi with WithAuthorization
