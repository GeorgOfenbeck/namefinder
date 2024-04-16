import mill._
import mill.scalalib._

object namefinder extends ScalaModule {

  def scalaVersion = "2.13.13"
  def ammoniteVersion = "3.0.0-M1"
  def ivyDeps = Agg(
    ivy"com.github.tototoshi::scala-csv:1.3.10",
    ivy"com.lihaoyi::os-lib:0.10.0",
    ivy"io.github.pityka::nspl-awt:0.10.0",
    ivy"org.plotly-scala::plotly-almond:0.8.4",
    )
  object test extends ScalaTests with TestModule.Munit {
    def ivyDeps = Agg(
      ivy"org.scalameta::munit::0.7.29",
      
    )
  }
}

