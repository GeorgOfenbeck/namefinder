package com.ofenbeck

import scalatags.Text.all._
object NameFinderWS extends cask.MainRoutes {


  case class Message(name: String, msg: String)
  import io.getquill._
  import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

  import com.mysql.cj.jdbc.MysqlDataSource
  val mysqlDataSource = new MysqlDataSource()
  mysqlDataSource.setServerName("192.168.1.51")
  mysqlDataSource.setUser("name")
  mysqlDataSource.setPassword("finder")
  mysqlDataSource.setDatabaseName("nameit")
  val hikariConfig = new HikariConfig()
  hikariConfig.setDataSource(mysqlDataSource)
  val ctx = new MysqlJdbcContext(LowerCase, new HikariDataSource(hikariConfig))
  ctx.executeAction(
    "CREATE TABLE IF NOT EXISTS namestats (name text, msg text);"
  )
  import ctx._

  def messages = ctx.run(query[Message].map(m => (m.name, m.msg)))

  var openConnections = Set.empty[cask.WsChannelActor]
  val bootstrap =
    "https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/css/bootstrap.css"

  @cask.staticResources("/static")
  def staticResourceRoutes() = "static"

  @cask.get("/init")
  def init() = {

    //ctx.executeAction("DROP TABLE IF EXISTS namestats;")
   val rawSQL = quote{
      """
      |CREATE TABLE IF NOT EXISTS namestats (
      |  name VARCHAR(255),
      |  total INT,
      |  last10years INT,
      |  syllables INT,
      |  lastYear INT,
      |  ratingM INT,
      |  ratingML INT,
      |  clashProb DOUBLE,
      |  prob DOUBLE,
      |  INDEX idx_name (name),
      |  INDEX idx_total (total),
      |  INDEX idx_last10years (last10years),
      |  INDEX idx_syllables (syllables),
      |  INDEX idx_lastYear (lastYear),
      |  INDEX idx_ratingM (ratingM),
      |  INDEX idx_ratingML (ratingML),
      |  INDEX idx_clashProb (clashProb),
      |  INDEX idx_prob (prob)
      |);
      |""".stripMargin
   }

    doctype("html")(
      html(
      head(
        link(rel := "stylesheet", href := bootstrap),
        script(src := "/static/app.js")
      ),
      body(
        h1("?"),
        
      )
    )
    )
  }

  @cask.get("/")
  def hello() = doctype("html")(
    html(
      head(
        link(rel := "stylesheet", href := bootstrap),
        script(src := "/static/app.js")
      ),
      body(
        div(cls := "container")(
          h1("Scala Chat!"),
          div(id := "messageList")(messageList()),
          div(id := "errorDiv", color.red),
          form(onsubmit := "submitForm(); return false")(
            input(
              `type` := "text",
              id := "nameInput",
              placeholder := "User name"
            ),
            input(
              `type` := "text",
              id := "msgInput",
              placeholder := "Write a message!"
            ),
            input(`type` := "submit")
          )
        )
      )
    )
  )

  def messageList() = frag(
    for ((name, msg) <- messages) yield p(b(name), " ", msg)
  )

  @cask.postJson("/")
  def postChatMsg(name: String, msg: String) = {
    if (name == "")
      ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "")
      ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else {
      import ctx._
      val msgval = Message(name, msg)
      val a = quote {
        query[Message].insert(
          _.name -> lift(msgval.name),
          _.msg -> lift(msgval.msg)
        )
      }
      // ctx.run(query[Message].insert(lift(msgval)))
      ctx.run(a)
      for (conn <- openConnections)
        conn.send(cask.Ws.Text(messageList().render))
      ujson.Obj("success" -> true, "err" -> "")
    }
  }

  @cask.websocket("/subscribe")
  def subscribe() = cask.WsHandler { connection =>
    connection.send(cask.Ws.Text(messageList().render))
    openConnections += connection
    cask.WsActor { case cask.Ws.Close(_, _) => openConnections -= connection }
  }

  initialize()
}
