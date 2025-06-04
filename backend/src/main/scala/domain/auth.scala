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
}