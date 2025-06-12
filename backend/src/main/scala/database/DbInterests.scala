package backend.database

import cats.effect.IO
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.ci.CIStringSyntax
import backend.domain.interests.{InterestRecord, InterestRequest, PatientInterestRecord}

import java.util.UUID

object DbInterests {

  /** POST a new interest; returns Right(()) or Left(DbError) */
  def addInterest(reqBody: InterestRequest): IO[Either[DbError, Unit]] = {
    val uri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/interests")
    val req = Request[IO](Method.POST, uri, headers = commonHeaders)
      .withEntity(reqBody.asJson)

    // TODO can we use executeNoContent instead
    EmberClientBuilder
      .default[IO]
      .build
      .use { client =>
        client.fetch(req) { resp =>
            resp.as[String].flatMap { body =>
                IO.pure(
                  resp.status match {
                    case Status.Created | Status.Ok => Right(())
                    case Status.NotFound            => Left(DbError.NotFound("interest",""))
                    case other                      => Left(DbError.SqlError(other.code, body))
                  }
                )
            }
        }
      }
  }

  def getInterestsForPatient(
    patientId: UUID
  ): IO[Either[DbError, List[InterestRecord]]] = {
    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/interests?select=clinic_id&patient_id=eq.$patientId"
    )
    val req = Request[IO](Method.GET, uri, headers = commonHeaders)
    // fetchAndDecode lifts into IO[Either[DbError, ...]]
    fetchAndDecode[List[InterestRecord]](req, "interests")
  }

  def removeInterest(reqBody: InterestRequest): IO[Either[DbError, Unit]] = {
    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/interests" +
      s"?patient_id=eq.${reqBody.patient_id}&clinic_id=eq.${reqBody.clinic_id}"
    )

    val req = Request[IO](Method.DELETE, uri, headers = commonHeaders)

    EmberClientBuilder
      .default[IO]
      .build
      .use { client =>
        client.fetch(req) { resp =>
          resp.as[String].flatMap { body =>
            IO.pure(
              resp.status match {
                case Status.NoContent => Right(())
                case Status.NotFound  => Left(DbError.NotFound("interest", ""))
                case other            => Left(DbError.SqlError(other.code, body))
              }
            )
          }
        }
      }
  }


  /** List all patients watching a given clinic */
  def getInterestsForClinic(
                             clinicId: UUID
                           ): IO[Either[DbError, List[PatientInterestRecord]]] = {
    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/interests?select=patient_id&clinic_id=eq.$clinicId"
    )
    val req = Request[IO](Method.GET, uri, headers = commonHeaders)
    // This decodes List[PatientInterestRecord]
    fetchAndDecode[List[PatientInterestRecord]](req, "interests")
  }
  
  
}
