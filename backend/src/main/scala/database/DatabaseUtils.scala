package backend.database

import cats.effect.IO
import org.http4s._
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.ci.CIStringSyntax
import io.circe.Json
import org.http4s.dsl.io._
import java.util.UUID

case class AuthRequest(
    uid: String,
    accessToken: String
)


val supabaseUrl: String = sys.env("SUPABASE_URL")
val supabaseKey: String = sys.env("SUPABASE_API_KEY")
