package idel.domain

import arrow.core.Either
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class Errors : DescribeSpec( {
  context("Either.entityOrNotFound") {
      describe("either contains value") {
          val e = Either.Right(1)
          e.isEntityOrNotFound() shouldBe true
      }

      describe("either contains EntityNotFound") {
          val e = Either.Left(EntityNotFound("",1))

          e.isEntityOrNotFound() shouldBe true
      }

      describe("either contains EntityAlreadyExists") {
          val e = Either.Left(EntityAlreadyExists(""))

          e.isEntityOrNotFound() shouldBe false
      }
  }
})