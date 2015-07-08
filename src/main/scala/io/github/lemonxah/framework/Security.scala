package io.github.lemonxah.framework

import java.security.MessageDigest

import com.novus.salat._
import com.novus.salat.global._

import scala.concurrent.duration._
import scala.language.postfixOps
import scalaz.Scalaz._
import scalaz._


/**
 * Project: dbabspro
 * Created on 2015/07/08.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */

case class Token(hash: Array[Byte], duration: Duration)
case class Tokenized[A](value: A, token: Token = Security.getToken(3 seconds))

object Security {
  private[Security] val secretKey = "b2657a08-d52d-4de9-914c-8716f6bca0d6"

  def getToken(dur: Duration): Token = {
    val factor = Math.pow(10, dur.toMillis.toString.length - 1).toInt
    val time = System.currentTimeMillis() / factor
    val key = secretKey + time
    val hash = MessageDigest.getInstance("SHA-1").digest(key.getBytes)
    Token(hash, dur)
  }

  def isValidToken(token: Token): Boolean = {
    val factor = Math.pow(10, token.duration.toMillis.toString.length - 1).toInt
    val time = System.currentTimeMillis
    val md = MessageDigest.getInstance("SHA-1")
    (0 to (token.duration.toMillis / factor).toInt)
      .exists(i => {
      val t = (time / factor).toInt - i
      val key = secretKey + t
      if (md.digest(key.getBytes).deep == token.hash.deep) true
      else false
    })
  }

  def validate[A](tokenized: Tokenized[A]): Validation[String, A] = try {
    if (isValidToken(tokenized.token)) tokenized.value.success
    else "Token on payload not valid".failure
  } catch {
    case ex: Exception => ex.getMessage.failure
  }

  def validateBSON[A](bytes: Array[Byte])(implicit m: Manifest[A]): Validation[String, A] = validate(grater[Tokenized[A]].fromBSON(bytes))

}
