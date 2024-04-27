package com.ofenbeck

import scalatags.Text.all._
object NameFinderWS extends cask.MainRoutes {

  case class Message(name: String, msg: String)
  import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

  import com.mysql.cj.jdbc.MysqlDataSource
  val mysqlDataSource = new MysqlDataSource()
  mysqlDataSource.setServerName("192.168.1.51")
  mysqlDataSource.setUser("name")
  mysqlDataSource.setPassword("finder")
  mysqlDataSource.setDatabaseName("nameit")
  val hikariConfig = new HikariConfig()
  hikariConfig.setDataSource(mysqlDataSource)
  val ctx = new io.getquill.MysqlJdbcContext(
    io.getquill.LowerCase,
    new HikariDataSource(hikariConfig)
  )

  def messages() = {
    import io.getquill._
    import ctx._
    val allnames = ctx.run(
      query[NameStats].map(m =>
        NameStats(
          name = m.name,
          total = m.total,
          last10years = m.last10years,
          syllables = m.syllables,
          lastYear = m.lastYear,
          ratingM = m.ratingM,
          ratingML = m.ratingML,
          clashProb = m.clashProb,
          prob = m.prob,
          considered = m.considered
        )
      )
    )
    allnames
  }
  def nextMuchkinName(): (String, Int) = {
    import io.getquill._
    import ctx._
    val name = ctx.run(
      query[NameStats]
        .filter(_.ratingM == 0)
        .filter(_.considered == true)
        .filter(n => n.ratingML > 50 || n.ratingML == 0)
        .take(1)
    )
    (name(0).name, name(0).ratingM)
  }

  def nextMuchkinLordName(): (String, Int) = {
    import io.getquill._
    import ctx._
    val name = ctx.run(
      query[NameStats]
        .filter(_.ratingML == 0)
        .filter(_.considered == true)
        //.sortBy(_.ratingM)(io.getquill.Ord.desc[Int])
        .filter(n => n.ratingM > 50 || n.ratingM == 0)
        .take(1)
    )
    (name(0).name, name(0).ratingML)
  }
  var openConnections = Set.empty[cask.WsChannelActor]
  val bootstrap =
    "https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/css/bootstrap.css"

  @cask.staticResources("/static")
  def staticResourceRoutes() = "static"

  @cask.get("/munchkinLord")
  def munchkinLord() = {
    val (name, rating) = nextMuchkinLordName()
    doctype("html")(
      html(
        head(
          link(rel := "stylesheet", href := bootstrap),
          link(rel := "stylesheet", href := "/static/style.css"),
          script(src := "/static/app.js")
        ),
        body(
          div(cls := "container")(
            h1("Mini Munch Name Picker (MunchkinLord)"),
            div(id := "NextName")(nextName(name)),
            div(id := "errorDiv", color.red),
            form(onsubmit := "submitMunchkinLord(); return false")(
              input(
                `type` := "hidden",
                id := "nameInput",
                value := name
              ),
              input(
                `type` := "range",
                min := 1,
                max := 100,
                value := "50",
                id := "nameRatingInput",
                `class` := "slider"
              ),
              input(`type` := "submit")
            )
          )
        )
      )
    )
  }

  @cask.get("/munchkin")
  def munchkin() = {
    val (name, rating) = nextMuchkinName()
    doctype("html")(
      html(
        head(
          link(rel := "stylesheet", href := bootstrap),
          link(rel := "stylesheet", href := "/static/style.css"),
          script(src := "/static/app.js")
        ),
        body(
          div(cls := "container")(
            h1("Mini Munch Name Picker (Munchkin)"),
            div(id := "NextName")(nextName(name)),
            div(id := "errorDiv", color.red),
            form(onsubmit := "submitMunchkin(); return false")(
              input(
                `type` := "hidden",
                id := "nameInput",
                value := name
              ),
              input(
                `type` := "range",
                min := 1,
                max := 100,
                value := "50",
                id := "nameRatingInput",
                `class` := "slider"
              ),
              input(`type` := "submit")
            )
          )
        )
      )
    )
  }

  @cask.postJson("/submitMunchkin")
  def postMuchkinRating(name: String, nameRating: String) = {
    val intRating = nameRating.toIntOption
    intRating match {
      case None =>
        ujson.Obj("success" -> false, "err" -> "Rating is not an Integer")
      case Some(rating) => {
        if (rating < 1 || rating > 100)
          ujson.Obj(
            "success" -> false,
            "err" -> "Rating is not between 1 and 100"
          )
        else {
          import io.getquill._
          import ctx._
          val q = quote {
            query[NameStats]
              .filter(_.name == lift(name))
              .update(_.ratingM -> lift(rating))
          }
          ctx.run(q)
          val (nName, nexthRating) = nextMuchkinName()
          for (conn <- openConnections)
            conn.send(cask.Ws.Text(messageList().render))
          ujson.Obj(
            "success" -> true,
            "err" -> "",
            "nameRender" -> nextName(nName).render,
            "name" -> nName
          )
        }
      }
    }
  }

  @cask.postJson("/submitMunchkinLord")
  def postMuchkinLordRating(name: String, nameRating: String) = {
    val intRating = nameRating.toIntOption
    intRating match {
      case None =>
        ujson.Obj("success" -> false, "err" -> "Rating is not an Integer")
      case Some(rating) => {
        if (rating < 1 || rating > 100)
          ujson.Obj(
            "success" -> false,
            "err" -> "Rating is not between 1 and 100"
          )
        else {
          import io.getquill._
          import ctx._
          val q = quote {
            query[NameStats]
              .filter(_.name == lift(name))
              .update(_.ratingML -> lift(rating))
          }
          ctx.run(q)
          val (nName, nexthRating) = nextMuchkinLordName()
          for (conn <- openConnections)
            conn.send(cask.Ws.Text(messageList().render))
          ujson.Obj(
            "success" -> true,
            "err" -> "",
            "nameRender" -> nextName(nName).render,
            "name" -> nName
          )
        }
      }
    }
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
          h1("Mini-Munch !"),
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

  def nextName(name: String) = frag(
    p(b(name), " Ofenbeck")
  )

  def messageList() = {

    val allnames = messages()

    val munchRank: Map[String, Int] = allnames
      .sortBy(_.ratingM)(Ordering[Int].reverse)
      .zipWithIndex
      .map { case (nameStats, index) =>
        nameStats.name -> index
      }
      .toMap

    val munchLordRank = allnames
      .sortBy(_.ratingML)(Ordering[Int].reverse)
      .zipWithIndex
      .map { case (nameStats, index) =>
        nameStats.name -> index
      }
      .toMap

    val xx = allnames.map(x => {
      val mRank = munchRank.getOrElse(x.name, -1)
      val mlRank = munchLordRank.getOrElse(x.name, -1)
      (x, mRank + mlRank)
    })
    val yy = xx.sortBy(_._2).map { case (nameStats, rank) =>
      if (nameStats.considered == false)
        tr(backgroundColor := "#D6EEEE")(
          td(nameStats.name),
          td(nameStats.lastYear),
          td(nameStats.ratingM),
          td(nameStats.ratingML),
          td(rank)
        )
      else
        tr(
          td(nameStats.name),
          td(nameStats.lastYear),
          td(nameStats.ratingM),
          td(nameStats.ratingML),
          td(rank)
        )
    }

    frag(
      table(
        thead(
          tr(
            th("Name"),
            th("Last Year"),
            th("Munchkin"),
            th("MunchkinLord"),
            th("Rank")
          )
        ),
        tbody(
          for (name <- yy) yield name
        )
      )
    )
  }

  @cask.postJson("/")
  def postChatMsg(name: String, msg: String) = {
    if (name == "")
      ujson.Obj("success" -> false, "err" -> "Name cannot be empty")
    else if (msg == "")
      ujson.Obj("success" -> false, "err" -> "Message cannot be empty")
    else {
      import io.getquill._
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
    connection.send(cask.Ws.Text(nextName(nextMuchkinName()._1).render))
    openConnections += connection
    cask.WsActor { case cask.Ws.Close(_, _) => openConnections -= connection }
  }

  @cask.get("/update")
  def update() = {
    val all: Vector[NameStats] = LoadCSV()
    import io.getquill._
    import ctx._
    val insertAll = quote {
      liftQuery(all).foreach(e =>
        query[NameStats]
          .filter(_.name == e.name)
          .filter(_.total == e.total)
          .filter(_.last10years == e.last10years)
          .filter(_.syllables == e.syllables)
          .filter(_.lastYear == e.lastYear)
          .update(_.considered -> e.considered)
      )
    }
    ctx.run(insertAll)
    doctype("html")(
      html(
        head(
          link(rel := "stylesheet", href := bootstrap),
          script(src := "/static/app.js")
        ),
        body(
          h1("!")
        )
      )
    )
  }

  @cask.get("/initDontUse")
  def init() = {
    val all: Vector[NameStats] = LoadCSV()
    import io.getquill._
    import ctx._
    val insertAll = quote {
      liftQuery(all).foreach(e =>
        query[NameStats].insertValue(
          NameStats(
            e.name,
            e.total,
            e.last10years,
            e.syllables,
            e.lastYear,
            e.ratingM,
            e.ratingML,
            e.clashProb,
            e.prob,
            e.considered
          )
        )
      )
    }
    ctx.run(insertAll)
    // ctx.executeAction("DROP TABLE IF EXISTS namestats;")
    val rawSQL =
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
      |  considered BOOLEAN,
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

    /*
DROP TABLE IF EXISTS namestats;
CREATE TABLE IF NOT EXISTS namestats (
  name VARCHAR(255),
  total INT,
  last10years INT,
  syllables INT,
  lastYear INT,
  ratingM INT,
  ratingML INT,
  clashProb DOUBLE,
  prob DOUBLE,
  considered BOOLEAN,
  INDEX idx_name (name),
  INDEX idx_total (total),
  INDEX idx_last10years (last10years),
  INDEX idx_syllables (syllables),
  INDEX idx_lastYear (lastYear),
  INDEX idx_ratingM (ratingM),
  INDEX idx_ratingML (ratingML),
  INDEX idx_clashProb (clashProb),
  INDEX idx_prob (prob),
  INDEX idx_considered (considered)
  );
     */

    doctype("html")(
      html(
        head(
          link(rel := "stylesheet", href := bootstrap),
          script(src := "/static/app.js")
        ),
        body(
          h1("?")
        )
      )
    )
  }
  initialize()

  override def port: Int = 80
  override def host: String = "0.0.0.0"
}
