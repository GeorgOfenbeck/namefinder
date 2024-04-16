package com.ofenbeck

import scala.io.Source

import com.github.tototoshi.csv._
import java.io.File

import os._
import com.ofenbeck.Namefinder.NameStats
import plotly._, plotly.element._, plotly.layout._, plotly.Almond._

object Namefinder extends App {

  val csvFilePath = os.read.inputStream(os.resource / "ts-x-01.04.00.10.csv")
  val reader = CSVReader.open(new java.io.InputStreamReader(csvFilePath))

  val classed = csvToClass(reader)
  case class NameStats(
      name: String,
      total: Int,
      last10years: Int,
      syllables: Int,
      lastYear: Int
  ) {
    override def toString() =
      s"$name: $syllables, $total, ${last10years / 10}, $lastYear"
  }
  val mapped = classed
    .groupBy(_.name)
    .map { case (name, femname) =>
      val total = femname.map(_.value).sum
      val last10years = femname.filter(_.yearOfBirth > 2010).map(_.value).sum
      val lastYear = femname.filter(_.yearOfBirth == 2022).map(_.value).sum
      val syllables = countSyllables(name)
      NameStats(name, total, last10years, syllables, lastYear)
    }
    .toVector
    .view
    .filter(_.syllables <= 3)
    .filter(_.name.length() < 8)
    .filter(_.name.forall(ch => ch.isLetter && ch <= 127))
    .filter(x => !x.name.toLowerCase().contains("t"))
    .toVector
    // .filter(_.total > 50)
    // sortBy(_.total) { Ordering[Int].reverse }
    .sortBy(_.lastYear)(Ordering[Int].reverse)
  // .drop(200)
  // .take(10)
  val vecs = mapped.zipWithIndex.foldLeft((Vector.empty[Int],Vector.empty[Int])) { case (acc, ele) =>
    ele match {
      case (nameStats, index) =>
      (acc._1 :+ index, acc._2 :+ nameStats.lastYear)
    }
  }
  val (xv,yv) = vecs
  //println(s"Down to ${mapped.length}")
  //mapped.foreach(x => println(x))

  // classed.foreach(println)
// decodet.foreach(println)

  // Process the CSV lines here

  def csvToClass(reader: CSVReader): List[FemaleName] = {
    val withHeader = reader.allWithHeaders()
    val classed = withHeader.map { row =>
      // print(row.keySet)
      if (
        row.keySet != Set(
          "TIME_PERIOD",
          "firstname",
          "YEAROFBIRTH",
          "VALUE",
          "OBS_STATUS"
        )
      ) {
        // println("Error: CSV file does not have the expected header")
        // println(row.keySet)
        FemaleName("", "", 0, 0, "")
      } else
        FemaleName(
          row("TIME_PERIOD"),
          row("firstname"),
          row("YEAROFBIRTH").toInt,
          row("VALUE").toInt,
          row("OBS_STATUS")
        )
    }
    classed
  }

  def countSyllables(word: String): Int = {
    val vowels = "aeiouyAEIOUY"
    val regex = "[aeiouyAEIOUY]+".r
    val syllables = regex.findAllIn(word).length
    // if (word.endsWith("e")) syllables - 1 else syllables
    syllables
  }

  def isOneEditAway(s1: String, s2: String): Boolean = {
    val diff = s1.zip(s2).count { case (c1, c2) => c1 != c2 }
    diff == 1 && s1.length == s2.length || Math.abs(s1.length - s2.length) == 1
  }
}
