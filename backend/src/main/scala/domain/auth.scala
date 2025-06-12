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
    dob: String,
    address: String
  )

  object AccountDetailsResponse {

    given decoder: Decoder[AccountDetailsResponse] = Decoder.instance { c =>
      for {
        firstName <- c.downField("first_name").as[String]
        lastName <- c.downField("last_name").as[String]
        dob <- c.downField("dob").as[String]
        address <- c.downField("address").as[String]
      } yield AccountDetailsResponse.create(firstName, lastName, dob, address)
    }

    def create(
      firstName: String,
      lastName: String,
      dob: String,
      address: String
    ): AccountDetailsResponse = AccountDetailsResponse(firstName, lastName, dob, address)
  }

  case class PatientInsert(
    uid: String,
    first_name: String,
    last_name: String,
    dob: String,
    address: String,
    latitude: Float,
    longitude: Float
  )
  
  case class RegisterRequest(
    firstName: String,
    lastName: String,
    dob: String,
    email: String,
    password: String,
    address: String,
    latitude: Float,
    longitude: Float
  )

  object RegisterRequest {

    given encoder: Encoder[RegisterRequest] = Encoder.instance { r =>
      Json.obj(
        "first_name" -> r.firstName.asJson,
        "last_name" -> r.lastName.asJson,
        "dob" -> r.dob.asJson,
        "email" -> r.email.asJson,
        "password" -> r.password.asJson,
        "address" -> r.address.asJson,
        "latitude" -> r.latitude.asJson,
        "longitude" -> r.longitude.asJson
      )
    }

    given decoder: Decoder[RegisterRequest] = Decoder.instance { c =>
      for {
        firstName <- c.downField("first_name").as[String]
        lastName  <- c.downField("last_name").as[String]
        dob       <- c.downField("dob").as[String]
        email     <- c.downField("email").as[String]
        password  <- c.downField("password").as[String]
        address   <- c.downField("address").as[String]
        latitude  <- c.downField("latitude").as[Float]
        longitude <- c.downField("longitude").as[Float]
      } yield RegisterRequest(firstName, lastName, dob, email, password, 
        address, latitude, longitude)
    }

    def create(
      firstName: String,
      lastName: String,
      dob: String,
      email: String,
      password: String,
      address: String,
      latitude: Float,
      longitude: Float
    ): RegisterRequest = RegisterRequest(firstName, lastName, dob, email, password, 
      address, latitude, longitude)
  }

}