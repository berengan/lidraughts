package controllers

import java.util.concurrent.{ Semaphore, TimeUnit }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal
import play.api.libs.json.Json
import play.api.mvc._

/** Development-only native Italian-draughts analysis; never invokes a shell. */
object ItalianAnalysis extends LidraughtsController {
  private val slots = new Semaphore(1)
  private implicit val analysisEc: ExecutionContext = scala.concurrent.ExecutionContext.global
  private val allowedFen = "^[WB]:(?:W[^:]*:B[^:]*|B[^:]*:W[^:]*)$".r

  def evaluate = Action.async { implicit req =>
    if (!sys.env.get("LIFIDAMA_ITALIAN_ANALYSIS_ENABLED").contains("true"))
      Future.successful(NotFound)
    else {
      val fen = req.getQueryString("fen").getOrElse("").trim
        .replaceFirst(":H[0-9]+:F[0-9]+$", "")
      val depth = req.getQueryString("depth").flatMap(s =>
        scala.util.Try(s.toInt).toOption).getOrElse(14)
      val squares = "\\d+".r.findAllIn(fen).toList.flatMap(s => scala.util.Try(s.toInt).toOption)
      if (fen.length > 200 || allowedFen.findFirstIn(fen).isEmpty ||
        squares.exists(n => n < 1 || n > 32) || depth < 4 || depth > 20)
        Future.successful(BadRequest(Json.obj("error" -> "invalid Italian position or depth")))
      else if (!slots.tryAcquire())
        Future.successful(ServiceUnavailable(Json.obj("error" -> "analysis busy")))
      else Future {
        var process: Process = null
        try {
          val binary = sys.env.getOrElse(
            "LIFIDAMA_ITALIAN_ANALYSIS_ENGINE",
            "/usr/src/lifidama-engine-v2/dama-linux"
          )
          val config = sys.env.getOrElse(
            "LIFIDAMA_ITALIAN_ANALYSIS_CONFIG",
            "/usr/src/lifidama-engine-v2/engine-levels.ini"
          )
          process = new ProcessBuilder(binary, "--analyse", "--fen", fen,
            "--level", "professional", "--max-depth", depth.toString,
            "--config", config).redirectErrorStream(true).start()
          if (!process.waitFor(8, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            GatewayTimeout(Json.obj("error" -> "native analysis timed out"))
          } else {
            val source = scala.io.Source.fromInputStream(process.getInputStream, "UTF-8")
            val output = try source.mkString finally source.close()
            if (process.exitValue() != 0 || output.length > 8192)
              BadGateway(Json.obj("error" -> "native analysis failed"))
            else Ok(Json.parse(output)).withHeaders("Cache-Control" -> "no-store")
          }
        } catch {
          case NonFatal(_) => BadGateway(Json.obj("error" -> "native analysis unavailable"))
        } finally {
          if (process != null && process.isAlive) process.destroyForcibly()
          slots.release()
        }
      }
    }
  }
}
