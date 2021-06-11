package org.sublimat.utils

import java.security.MessageDigest

object CryptoUtils {

  private val GLOB_SALT = "$2a$12$jEgg"

  def encryptWithGlobSalt(password: String): String = {
    MessageDigest.getInstance("SHA-256")
      .digest((password + GLOB_SALT).getBytes("UTF-8"))
      .map("%02x".format(_)).mkString
  }

  def sha512(data: String, salt: String): String = {
    MessageDigest.getInstance("SHA-512")
      .digest(s"$data$salt".getBytes("UTF-8"))
      .map("%02x".format(_)).mkString
  }

}
