package backend.database

import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.{Decoder, Json}
import cats.effect.IO
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.ci.CIStringSyntax
import org.http4s.dsl.io.*
import backend.domain.slots.*
import backend.domain.slots.given

import java.util.UUID


/*

TODO:
These functions need uniformity, in particular
1) Return types
2) Dealing with errors, payloads, json stuff and therefore
3) how they should be used in SlotRoutes

 */



object DbSlots {


  /** A simple ADT for database-level errors */
  sealed trait DbError
  object DbError {
    final case class NotFound(entity: String, id: String) extends DbError
    final case class DecodeError(message: String) extends DbError
    final case class SqlError(status: Int, body: String) extends DbError
    final case class Unknown(message: String) extends DbError
  }

  /** Reusable headers so we don't repeat them in each call */
  private val commonHeaders = Headers(
    Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
    Header.Raw(ci"apikey", supabaseKey),
    Header.Raw(ci"Content-Type", "application/json")
  )

  /**
   * Common “fetch a request and decode it into A” logic
   * so we don’t repeat headers + error‐mapping everywhere.
   */
  private def fetchAndDecode[A: Decoder](
      req: Request[IO],
      notFoundMsg: String
  ): IO[Either[DbError, A]] = {
    EmberClientBuilder
      .default[IO]
      .build
      .use { client =>
        client.fetch(req) { resp =>
          resp.status match {
            case Status.Ok =>
              resp.as[A].attempt.flatMap {
                case Right(a)     => IO.pure(Right(a))
                case Left(decErr) => IO.pure(Left(DbError.DecodeError(decErr.getMessage)))
              }
            case Status.NotFound =>
              IO.pure(Left(DbError.NotFound(notFoundMsg, "")))
            case other =>
              resp.as[String].map(body => Left(DbError.SqlError(other.code, body)))
          }
        }
    }
  }

  /**
   * A helper for write‐operations that expect no JSON body in response, only status codes.
   *  - onSuccess → Right(())
   *  - on 404    → Left(NotFound(entityName, id))
   *  - on other → Left(SqlError)
   */
  private def executeNoContent(
                                req: Request[IO],
                                entityName: String,
                                entityId:   String
                              ): IO[Either[DbError, Unit]] =
    EmberClientBuilder
      .default[IO]
      .build
      .use { client =>
        client.fetch(req) { resp =>
          resp.status match {
            case Status.NoContent      => IO.pure(Right(()))
            case Status.Created | Status.Ok =>
              // for POST which returns 201 or 200
              IO.pure(Right(()))
            case Status.NotFound       => IO.pure(Left(DbError.NotFound(entityName, entityId)))
            case other                 =>
              resp.as[String].map(body => Left(DbError.SqlError(other.code, body)))
          }
        }
      }


  def addSlot(slotReq: SlotRequest): IO[Either[DbError, Unit]] = {

    val payload = slotReq.asJson
    val slotUri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/slots")
    val slotRequest = Request[IO](
      method = Method.POST,
      uri = slotUri,
      headers = commonHeaders
    ).withEntity(payload)

    executeNoContent(slotRequest, "slot", "") // no ID known yet on insert TODO idk about the 3rd parameter tbh
  }

//  def getAllSlots: IO[Either[String, List[SlotResponse]]] = {
//
//
//    val slotsUri = Uri.unsafeFromString("s$supabaseUrl/rest/v1/slots?select=*")
//
//    val getRequest = Request[IO](
//      method = Method.GET,
//      uri = slotsUri,
//      headers = Headers(
//        Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
//        Header.Raw(ci"apikey", supabaseKey),
//        Header.Raw(ci"Content-Type", "application/json")
//      )
//    )
//
//    EmberClientBuilder
//      .default[IO]
//      .build
//      .use { httpClient =>
//        httpClient.fetch(getRequest) { response =>
//          response.status match {
//            case Status.Ok =>
//              // Attempt to decode the response body into List[SlotResponse]
//              response.as[List[SlotResponse]].attempt.flatMap {
//                case Right(slotsList) =>
//                  IO.pure(Right(slotsList))
//                case Left(err) =>
//                  IO.pure(Left(s"Failed to decode slots: ${err.getMessage}"))
//              }
//
//            case _ =>
//              // Read the raw body as a string for debugging
//              response.as[String].flatMap { bodyStr =>
//                IO.pure(Left(s"Supabase returned ${response.status.code}: $bodyStr"))
//              }
//          }
//        }
//      }
//  }

    def getSlots(
        filter: SlotFilter,
        pagination: Pagination
    ): IO[Either[DbError, List[PatientSlotResponse]]] = {

      val baseSelect = "slot_time,slot_length,clinic_info,is_taken"
      val selectQuery = s"?select=$baseSelect"

      val filterClauses = List(
        filter.isTaken.map(t => s"is_taken=eq.$t"),
        filter.clinicInfo.map(ci => s"clinic_info=ilike.%$ci%"),
        filter.slotTimeGte.map(i => s"slot_time=gte.${i}"),
        filter.slotTimeLte.map(i => s"slot_time=lte.${i}")
      ).flatten
       .map("&" + _)
       .mkString("")

      val paginationClauses = s"&limit=${pagination.limit}&offset=${pagination.offset}"

      val fullUri = Uri.unsafeFromString(
        s"$supabaseUrl/rest/v1/slots$selectQuery$filterClauses$paginationClauses"
      )

      val req = Request[IO](
        method = Method.GET,
        uri = fullUri,
        headers = commonHeaders
      )

      fetchAndDecode[List[PatientSlotResponse]](req, "slots")

    }


    /**
     * 1) Fetch all slots, but return only the fields:
     *    - slot_time
     *    - slot_length
     *    - clinic_info
     *    - is_taken
     *
     * This is what patients will see.
     * We do a Supabase GET on /slots?select=slot_time,slot_length,clinic_info,is_taken
     * TODOOOO: Do we need this?
     */
    def getAllSlotsForPatients: IO[Either[DbError, List[PatientSlotResponse]]] =
      getSlots(SlotFilter(None, None, None, None), Pagination(None, None))


  /**
   * 2) Fetch exactly one slot by its slot_id (UUID).
   * Returns all fields (select=*), including booking_id, created_at, etc.
   * We do GET /slots?select=*&slot_id=eq.<UUID>
   * Supabase returns a JSON array (either [ {...} ] or []), so we pick the first element.
   */
  def getSlotById(slotId: UUID): IO[Either[DbError, Slot]] = {

    val uri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/slots?select=*&slot_id=eq.$slotId")

    val req = Request[IO](
      method = Method.GET,
      uri = uri,
      headers = commonHeaders
    )

    fetchAndDecode[List[Slot]](req, "slot").flatMap {
      case Right(slot :: Nil) => IO.pure(Right(slot))
      case Right(Nil)         => IO.pure(Left(DbError.NotFound("slot", slotId.toString)))
      case Right(_ :: _ :: _) => IO.pure(Left(DbError.Unknown("Multiple slots with same ID")))
      case Left(err)          => IO.pure(Left(err))
    }

  }

  /**
   * Delete a slot by its slot_id (UUID).
   * Sends DELETE /rest/v1/slots?slot_id=eq.<UUID> to Supabase.
   * Returns IO[Either[String, Unit]]:
   *   - Right(()) if Supabase replies 204 No Content
   *   - Left(errorMessage) otherwise
   */
  def deleteSlot(slotId: UUID): IO[Either[DbError, Unit]] = {

    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/slots?slot_id=eq.$slotId"
    )
    val req = Request[IO](
      method = Method.DELETE,
      uri = uri,
      headers = commonHeaders
    )

    executeNoContent(req, "slot", slotId.toString)
  }


}