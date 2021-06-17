package org.sublimat.utils

import zio.test.Assertion._
import zio.test._

object CryptSpec extends DefaultRunnableSpec {

  override def spec =
    suite("CryptSpec")(
      test("should hash password with salt") {
        val hash = CryptoUtils.encryptWithGlobSalt("PW4WS900")
        val hash2 = CryptoUtils.encryptWithGlobSalt("PW4WS900")
        println(s"passwordHash = ${hash}")
        assert(hash)(equalTo(hash2))
      }
    )

}
