package com.ofenbeck

import scala.io.Source

import com.github.tototoshi.csv._
import java.io.File

object Hello extends App {

  def msg = "Hello World!"

  println(msg)

  val csvFilePath = "/Users/taaofge1/code/PoCs/namefinder/ts-x-01.04.00.10.csv"

  /*
  //read the file into a string
  val source = scala.io.Source.fromFile(csvFilePath)
  val it = source.getLines()
  it.next() // skip the header
  val lines = it.toList.mkString("\n")
  source.close()
  val csvLines = ru.johnspade.csv3s.parser.parseComplete(lines)
  if (csvLines.isLeft) {
    println(s"Error parsing CSV file: ${csvLines.left.get}")
    System.exit(1)
  }
  val decodet = csvLines.map(parsed => {
     val decoded = parsed.rows.rows.map( rows => FemaleName.decoder.decode(rows))
     val working = decoded.flatMap( x => x.toOption)
     working
  })
  decodet.foreach(println)
   */
  val reader = CSVReader.open(new File(csvFilePath))

  val withHeader = reader.allWithHeaders()
  withHeader.head.keySet.foreach(println)
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
      println("Error: CSV file does not have the expected header")
      println(row.keySet)
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
  //println( classed.length)
  val uniqueNames = classed.map(_.name).toSet 
  println( uniqueNames.size)

 case class NameStats(name: String, total: Int, last10years: Int, syllables: Int, lastYear: Int){
  override def toString() = s"$name: $total, $last10years, $syllables, $lastYear"
 } 
  val mapped =classed
  .groupBy(_.name)
  .map( (name, femname) => 
      val total = femname.map(_.value).sum
      val last10years = femname.filter( _.yearOfBirth > 2010).map(_.value).sum
      val lastYear = femname.filter( _.yearOfBirth == 2022).map(_.value).sum
      val syllables = countSyllables(name)
      NameStats(name, total, last10years, syllables, lastYear)
  )
  .toVector
  .filter( _.syllables <= 3)
  .filter( _.name.length() < 8)
  .filter(_.name.forall(ch => ch.isLetter && ch <= 127))
  .filter(_.total > 50)
  .sortBy(_.total){Ordering[Int].reverse}
  /*.foldLeft(Map.empty[String, NameStats]) { (result, current) =>
    if (result.exists(x => isOneEditAway(x.str, current.str))) result
    else current :: result
  }*/
  .filter(x =>  !x.name.toLowerCase().contains("t"))
  .sortBy(_.lastYear)(Ordering[Int].reverse)
  // .drop(1000)
   .take(1000)

  println(s"Down to ${mapped.length}")
  mapped.foreach(x => println(x))
  
  


  // classed.foreach(println)
// decodet.foreach(println)

  // Process the CSV lines here

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