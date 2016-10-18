import java.io.{FileWriter, IOException}
import java.nio.file.{Paths, Files}

import org.jsoup.select.Elements
import org.jsoup.{HttpStatusException, Jsoup}
import scala.text.Document
import scala.util.matching.Regex
import play.api.libs.json._
import scala.io.Source


/**
 * Resolves codeshare and non-codeshare flightcodes using data scraped from flightradar24.com
 * No access tokens needed.
 * You must provide input file with flight codes to resolve.
 */
object FR24CodeshareScraper {

  // Input file containing flightcodes to resolve
  val flightcodesInputFile = "flightcodes.txt"

  // This file will contain operating carriers' flightcodes
  val nonCodeshareFlightcodesOutputFile = "non_codeshare_flightcodes.txt"

  // This file will contain resolution table (in csv format)
  // || marketing carrier flightcode | operating carrier flightcode ||
  val codeshareFlightcodesOutputFile = "codeshare_flightcodes.csv"


  def main (args: Array[String]) {

    val ncffw = new FileWriter(nonCodeshareFlightcodesOutputFile)

    val fw = new FileWriter(codeshareFlightcodesOutputFile)
    fw.write("code,operating_carrier_code\n")

    var i = 1

    Source
      .fromFile(flightcodesInputFile)
      .getLines()
      .toList
      .distinct
      .foreach(
        line => {

          if(i%100 == 0) println(i)

          val boundary = if(line(2).isLetter) 3 else 2

          val number =
            if(line.last.isLetter)
              line.substring(boundary, line.length-1).toInt.toString + line.last
            else
              line.substring(boundary, line.length).toInt.toString

          val carrier = line.substring(0, boundary)

          val code = carrier + number

          val json =
            fetchPage(
              String.format(
                "https://www.flightradar24.com/v1/search/web/find?query=%s&limit=18&type=schedule",
                code.toLowerCase
              )
            )
            .text()

          val parsed = Json.parse(json)

          val actualCode = (parsed \ "results" \ 0 \ "id").getOrElse(JsNull).toString().replace("\"", "")
          val codeshare = (parsed \ "results" \ 0 \ "match").getOrElse(JsNull).toString().replace("\"", "") == "codeshare"


          if(codeshare)
            fw.write(code+","+actualCode+"\n")
          else if (actualCode != "null")
            ncffw.write(actualCode+"\n")

          i += 1
        }
      )

    fw.close()
    ncffw.close()


  }


  private def fetchPage(url: String): org.jsoup.nodes.Document = {
    try {

      Jsoup
        .connect(url)
        .ignoreContentType(true)
        .timeout(100000)
        .userAgent(
          "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
        .get()

    }
    catch {

      case e: Exception => {
        println(e)
        println("pause and retry..")
        Thread.sleep(5000)

        fetchPage(url)
      }

    }


  }


}
