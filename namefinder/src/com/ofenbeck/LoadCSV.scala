package com.ofenbeck

import com.github.tototoshi.csv._

object LoadCSV {
  def apply(): Vector[NameStats] = {

    val csvFilePath = os.read.inputStream(os.resource / "ts-x-01.04.00.10.csv")
    val reader = CSVReader.open(new java.io.InputStreamReader(csvFilePath))

    val classed = csvToClass(reader)

    val allnames = classed
    .groupBy(_.name)
    .map{case (name, femname) => {
      val total = femname.map(_.value).sum
      val last10years = femname.filter(_.yearOfBirth > 2010).map(_.value).sum
      val lastYear = femname.filter(_.yearOfBirth == 2022).map(_.value).sum
      val syllables = countSyllables(name)
      NameStats(name, total, last10years, syllables, lastYear, 0, 0, 0.0, 0.0)
    }
        }.toVector

    .sortBy(_.lastYear)(Ordering[Int].reverse)

    val numberGirlsLastYear = allnames.filter(_.lastYear > 0).map(_.lastYear).sum

    val withstats: Vector[NameStats] = allnames.filter(_.lastYear > 1).zipWithIndex.map( ele =>
    ele match {
      case (nameStats, index) =>
         val prob: Double = 1.0*nameStats.lastYear/numberGirlsLastYear
         nameStats.copy( prob = prob, clashProb = 1.0-(scala.math.pow(1.0-prob,30 ) ))
        }
    )
    withstats
  }

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

}
