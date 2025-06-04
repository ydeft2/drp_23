package backend.database

import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.Json
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

  def addSlot(slotReq: SlotRequest): IO[Either[String, Unit]] = {

    val payload = slotReq.asJson

    val slotUri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/slots")

    val slotRequest = Request[IO](
      method = Method.POST,
      uri = slotUri,
      headers = Headers(
        Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
        Header.Raw(ci"apikey", supabaseKey),
        Header.Raw(ci"Content-Type", "application/json")
      )
    ).withEntity(payload)

    EmberClientBuilder.default[IO].build.use { httpClient =>
      httpClient.fetch(slotRequest) { response =>
        response.status match {
          case Status.Ok | Status.Created =>
            IO.println("Slot added successfully") *> IO.pure(Right(()))
          case _ =>
            IO.pure(Left(s"Failed to add slot: ${response.status}"))
        }
      }
    }
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

    /**
     * 1) Fetch all slots, but return only the fields:
     *    - slot_time
     *    - slot_length
     *    - clinic_info
     *    - is_taken
     *
     * This is what patients will see.
     * We do a Supabase GET on /slots?select=slot_time,slot_length,clinic_info,is_taken
     */
    def getAllSlotsForPatients: IO[Either[String, List[PatientSlotResponse]]] = {

      val uri = Uri.unsafeFromString(
        s"$supabaseUrl/rest/v1/slots?select=slot_time,slot_length,clinic_info,is_taken"
      )

      val req = Request[IO](
        method = Method.GET,
        uri = uri,
        headers = Headers(
          Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
          Header.Raw(ci"apikey", supabaseKey),
          Header.Raw(ci"Content-Type", "application/json")
        )
      )

      EmberClientBuilder.default[IO].build.use { client =>
        client.fetch(req) { resp =>
          resp.status match {
            case Status.Ok =>
              resp.as[List[PatientSlotResponse]].attempt.flatMap {
                case Right(slotList) => IO.pure(Right(slotList))
                case Left(decodeErr) => IO.pure(Left(s"Decoding error: ${decodeErr.getMessage}"))
              }
            case _ =>
              resp.as[String].map(body => Left(s"Supabase error: ${resp.status.code}"))
          }

        }
      }
    }

  /**
   * 2) Fetch exactly one slot by its slot_id (UUID).
   * Returns all fields (select=*), including booking_id, created_at, etc.
   * We do GET /slots?select=*&slot_id=eq.<UUID>
   * Supabase returns a JSON array (either [ {...} ] or []), so we pick the first element.
   */
  def getSlotById(slotId: UUID): IO[Either[String, Slot]] = {

    val uri = Uri.unsafeFromString(s"$supabaseUrl/rest/v1/slots?select=*&slot_id=eq.$slotId")
    val req = Request[IO](
      method = Method.GET,
      uri = uri,
      headers = Headers(
        Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
        Header.Raw(ci"apikey", supabaseKey),
        Header.Raw(ci"Content-Type", "application/json")
      )      
    )

    EmberClientBuilder.default[IO].build.use { client =>
      client.fetch(req) { resp =>
        resp.status match {
          case Status.Ok =>
            resp.as[List[Slot]].flatMap {
              case slot :: _ => IO.pure(Right(slot))
              case Nil => IO.pure(Left("Slot not found"))
              case _ => IO.pure(Left("Multiple slots with same ID"))
            }
          case other =>
            resp.as[String].map(body => Left(s"Supabase error: ${other.code} – $body"))
        }
      }
    }
  }

  /**
   * Delete a slot by its slot_id (UUID).
   * Sends DELETE /rest/v1/slots?slot_id=eq.<UUID> to Supabase.
   * Returns IO[Either[String, Unit]]:
   *   - Right(()) if Supabase replies 204 No Content
   *   - Left(errorMessage) otherwise
   */
  def deleteSlot(slotId: UUID): IO[Either[String, Unit]] = {

    val uri = Uri.unsafeFromString(
      s"$supabaseUrl/rest/v1/slots?slot_id=eq.$slotId"
    )
    val req = Request[IO](
      method = Method.DELETE,
      uri = uri,
      headers = Headers(
        Header.Raw(ci"Authorization", s"Bearer $supabaseKey"),
        Header.Raw(ci"apikey", supabaseKey),
        Header.Raw(ci"Content-Type", "application/json")
      )
    )

    EmberClientBuilder.default[IO].build.use { client =>
      client.fetch(req) { resp =>
        resp.status match {
          case Status.NoContent =>
            // 204 means exactly one row was deleted, hopefully..
            IO.pure(Right(()))
          case Status.NotFound =>
            IO.pure(Left("Slot not found"))
          case other =>
            resp.as[String].flatMap(body =>
              IO.pure(Left(s"Supabase DELETE error: ${other.code} – $body"))
            )
        }
      }
    }
  }


}