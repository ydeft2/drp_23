package backend.domain

import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._
import java.util.UUID

object auth {

  case class RoleResponse(
    uid: UUID,
    is_patient: Boolean
  )

  object RoleResponse {

    given decoder: Decoder[RoleResponse] = Decoder.instance { c =>
      for {
        uid <- c.downField("uid").as[UUID]
        isPatient <- c.downField("is_patient").as[Boolean]
      } yield RoleResponse.create(uid, isPatient)
    }

    def create(
      uid: UUID,
      isPatient: Boolean
    ): RoleResponse = RoleResponse(uid, isPatient)
  }

  case class AccountDetailsResponse(
    first_name: String,
    last_name: String,
    dob: String
  )

  object AccountDetailsResponse {

    given decoder: Decoder[AccountDetailsResponse] = Decoder.instance { c =>
      for {
        firstName <- c.downField("first_name").as[String]
        lastName <- c.downField("last_name").as[String]
        dob <- c.downField("dob").as[String]
      } yield AccountDetailsResponse.create(firstName, lastName, dob)
    }

    def create(
      firstName: String,
      lastName: String,
      dob: String
    ): AccountDetailsResponse = AccountDetailsResponse(firstName, lastName, dob)
  }
}