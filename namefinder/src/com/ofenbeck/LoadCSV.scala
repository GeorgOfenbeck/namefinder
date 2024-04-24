package com.ofenbeck

object LoadCSV {
  def apply() = {

import com.github.tototoshi.csv._
 
     val csvFilePath = os.read.inputStream(os.pwd / "work" / "ts-x-01.04.00.10.csv")
    val reader = CSVReader.open(new java.io.InputStreamReader(csvFilePath))

      val classed = csvToClass(reader)


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
