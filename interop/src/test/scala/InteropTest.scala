import com.github.ghik.silencer.silent
import log.effect.interop.TestLogCapture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

@silent("local method [a-zA-Z0-9]+ in value <local InteropTest> is never used")
final class InteropTest extends AnyWordSpecLike with Matchers with TestLogCapture {

  "A LogWriter instance can be derived from a log4cats Logger" in {
    import cats.effect.IO
    import cats.effect.unsafe.implicits.global
    import org.typelevel.log4cats.slf4j.Slf4jLogger
    import log.effect.LogWriter
    import log.effect.internal.Show

    final class A()
    object A {
      implicit val showA: Show[A] =
        new Show[A] {
          def show(a: A): String = "an A"
        }
    }

    val logged = capturedLog4sOutOf { logger =>
      import log.effect.interop.log4cats._

      implicit val buildMessageLogger =
        Slf4jLogger.fromSlf4j[IO](logger).unsafeRunSync()

      val lw = implicitly[LogWriter[IO]]

      lw.trace("a message") >>
        lw.trace(new Exception("an exception")) >>
        lw.trace("a message", new Exception("an exception")) >>
        lw.trace(new A()) >>
        lw.debug("a message") >>
        lw.debug(new Exception("an exception")) >>
        lw.debug("a message", new Exception("an exception")) >>
        lw.debug(new A()) >>
        lw.info("a message") >>
        lw.info(new Exception("an exception")) >>
        lw.info("a message", new Exception("an exception")) >>
        lw.info(new A()) >>
        lw.warn("a message") >>
        lw.warn(new Exception("an exception")) >>
        lw.warn("a message", new Exception("an exception")) >>
        lw.warn(new A()) >>
        lw.error("a message") >>
        lw.error(new Exception("an exception")) >>
        lw.error("a message", new Exception("an exception")) >>
        lw.error(new A())
    }

    logged.size shouldBe 20
  }

  "The readme interop example compiles" in {
    import cats.effect.{Resource, Sync}
    import cats.syntax.flatMap._
    import org.typelevel.log4cats.Logger
    import log.effect.LogWriter

    import log.effect.interop.log4cats._

    sealed trait RedisClient[F[_]] {
      def address: String
      def write: F[Unit]
    }
    object RedisClient {
      def apply[F[_]: LogWriter](addr: String)(implicit F: Sync[F]): Resource[F, RedisClient[F]] =
        Resource.make(
          F.pure(
            new RedisClient[F] {
              val address        = addr
              def write: F[Unit] = LogWriter.info(address)
            }
          )
        )(_ => F.unit)
    }

    def buildLog4catsLogger[F[_]]: F[Logger[F]] = ???

    def storeOwnAddress[F[_]: Sync](address: String): F[Unit] =
      buildLog4catsLogger[F] >>= { implicit l =>
        RedisClient[F](address).use { cl =>
          cl.write
        }
      }
  }
}
