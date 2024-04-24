import mill._
import mill.scalalib._

object namefinder extends ScalaModule {

  //def scalaVersion = "2.13.13"
  def scalaVersion = "3.3.3"
  def ammoniteVersion = "3.0.0-M1"
  def ivyDeps = Agg(
    ivy"com.github.tototoshi::scala-csv:1.3.10",
    ivy"com.lihaoyi::os-lib:0.10.0",
    ivy"com.lihaoyi::cask:0.9.2",
    ivy"com.lihaoyi::scalatags:0.13.1",
    ivy"io.getquill::quill-jdbc:4.8.3",
    ivy"com.mysql:mysql-connector-j:8.3.0",
    //ivy"org.postgresql:postgresql:42.7.3",
    ivy"com.opentable.components:otj-pg-embedded:0.13.1"
  )
  object test extends ScalaTests with TestModule.Munit {
    def ivyDeps = Agg(
      ivy"org.scalameta::munit::0.7.29",
      ivy"com.lihaoyi::utest:0.8.3",
      ivy"com.lihaoyi::requests:0.8.2"
    )
  }
}
