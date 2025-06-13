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

  def getSlots(
      filter: SlotFilter,
      pagination: Pagination
  ): IO[Either[DbError, List[PatientSlotResponse]]] = {

    val baseSelect = "slot_time,slot_length,clinic_info,is_taken,clinic_id,slot_id"
    val selectQuery = s"?select=$baseSelect"
    val maybeClinicClause: Option[String] = filter.clinicId.map(id => s"clinic_id=eq.$id")

    val filterClauses = List(
      maybeClinicClause,
      filter.isTaken.map(t => s"is_taken=eq.$t"),
      filter.clinicInfo.map(ci => s"clinic_info=ilike.%25$ci%25"),
      filter.slotTimeGte.map(i => s"slot_time=gte.${i}"),
      filter.slotTimeLte.map(i => s"slot_time=lte.${i}")
    ).flatten
     .map("&" + _)
     .mkString("")

    /*
    We might have to use this instead of the .map.mkString call above
      val filterString: String =
        if (filterClauses.isEmpty) ""
        else filterClauses.map("&" + _).mkString("")
     */


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
    getSlots(SlotFilter(None, None, None, None, None), Pagination(None, None))


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