package maiden.common


object Converters {

  def ccToMap(cc: AnyRef) =
    (Map[String, Any]() /: cc.getClass.getDeclaredFields) {
      (a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(cc))
    }


}
