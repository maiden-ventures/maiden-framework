package maiden.helpers

import java.security.SecureRandom
import com.roundeights.hasher.Algo
import com.roundeights.hasher.Implicits._
import scala.language.postfixOps

object Hasher {

  private[this] def hashUsing(algo: Algo, string: String) = algo(string)

  /* with salt */
  def md5(salt: String, s: String) = hashUsing(Algo.hmac(salt).md5, s).hex
  def sha256(salt: String, s: String) = hashUsing(Algo.hmac(salt).sha256, s).hex
  def sha512(salt: String, s: String) = hashUsing(Algo.hmac(salt).sha512, s).hex

  /* without salt */
  def md5(s: String) = hashUsing(Algo.md5, s).hex
  def sha256(s: String) = hashUsing(Algo.sha256, s).hex
  def sha512(s: String) = hashUsing(Algo.sha512, s).hex
}


object TokenGenerator {

  private[this] val TOKEN_LENGTH = 128
  private[this] val TOKEN_CHARS =
     "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._"
  private[this] val secureRandom = new SecureRandom()

  def generate:String =
    generate(TOKEN_LENGTH)

  def generate(tokenLength: Int): String =
    if(tokenLength == 0) "" else TOKEN_CHARS(secureRandom.nextInt(TOKEN_CHARS.length())) +
     generate(tokenLength - 1)
}

object RandomStringGenerator {

  private[this] val secureRandom = new SecureRandom()
  private[this] val TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

  def generate(tokenLength: Int = 12): String =
    if(tokenLength == 0) "" else TOKEN_CHARS(secureRandom.nextInt(TOKEN_CHARS.length())) +
     generate(tokenLength - 1)
}
