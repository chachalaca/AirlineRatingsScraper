import java.io.{FileWriter, IOException}
import java.nio.file.{Paths, Files}

import org.jsoup.select.Elements
import org.jsoup.{HttpStatusException, Jsoup}
import scala.text.Document
import scala.util.matching.Regex
import play.api.libs.json._
import scala.io.Source



object FR24FlightsHistoryScraper {


  def main (args: Array[String]) {

    Source
      .fromFile("non_codeshare_flightcodes")
      .getLines()
      .foreach(
        line => {

          val number = line.substring(2, line.length).toInt
          val carrier = line.substring(0,2)

          val code = carrier + number.toString

          val json =
            fetchPage(
              String.format(
                "https://api.flightradar24.com/common/v1/flight/list.json?query=%s&fetchBy=flight&page=1&limit=100&token=MHOGTypsr3LUh7xVFTsLTscE-ji67nOujfckEBCa25o&timestamp=1467755400",
                code.toLowerCase
              )
            )
            .text()

          val parsed = Json.parse(json)

          val fw = new FileWriter("flights_history/" + code)
          fw.write("scheduled_departure,actual_departure\n")

          (parsed \ "result" \ "response" \ "data" \\ "time")
            .foreach(
              instance => {
                val scheduledDeparture = (instance \ "scheduled" \ "departure").get
                val actualDeparture = (instance \ "real" \ "departure").get

                fw.write(scheduledDeparture.toString().replace("\"", "") + "," + actualDeparture.toString().replace("\"", "") + "\n")
              }
            )

          fw.close()

        }
      )






    /*

    iataCodes
      .foreach(
        iata => {

          println(i + " " + iata)

          try {

            val json =
              fetchPage(
                String.format(
                  "https://api.flightradar24.com/common/v1/airport.json?code=%s&plugin[]=&plugin-setting[schedule][mode]=&plugin-setting[schedule][timestamp]=%s&page=1&limit=25&token=",
                  iata.toLowerCase,
                  (System.currentTimeMillis / 1000).toString
                )
              )
                .text()

            val parsed = Json.parse(json)

            val icao = (parsed \ "result" \ "response" \ "airport" \ "pluginData" \ "details" \ "code" \ "icao").get
            val name = (parsed \ "result" \ "response" \ "airport" \ "pluginData" \ "details" \ "name").get
            val latitude = (parsed \ "result" \ "response" \ "airport" \ "pluginData" \ "details" \ "position" \ "latitude").getOrElse(JsNull)
            val longitude = (parsed \ "result" \ "response" \ "airport" \ "pluginData" \ "details" \ "position" \ "longitude").getOrElse(JsNull)
            val altitude = (parsed \ "result" \ "response" \ "airport" \ "pluginData" \ "weather" \ "elevation" \ "m").getOrElse(JsNull)
            val country = (parsed \ "result" \ "response" \ "airport" \ "pluginData" \ "details" \ "position" \ "country" \ "name").getOrElse(JsNull)
            val city = (parsed \ "result" \ "response" \ "airport" \ "pluginData" \ "details" \ "position" \ "region" \ "city").getOrElse(JsNull)
            val ratingsAvg = (parsed \ "result" \ "response" \ "airport" \ "pluginData" \ "flightdiary" \ "ratings" \ "avg").getOrElse(JsNull)
            val ratingsTotal = (parsed \ "result" \ "response" \ "airport" \ "pluginData" \ "flightdiary" \ "ratings" \ "total").getOrElse(JsNull)
            val reviewsCount = (parsed \ "result" \ "response" \ "airport" \ "pluginData" \ "flightdiary" \ "reviews").getOrElse(JsNull)
            val reviewsEvaluation = (parsed \ "result" \ "response" \ "airport" \ "pluginData" \ "flightdiary" \ "evaluation").getOrElse(JsNull)

            fw.write(iata + "," + icao + "," + name + "," + latitude + "," + longitude + "," + altitude + "," + country + "," + city + "," + ratingsAvg + "," + ratingsTotal + "," + reviewsCount + "," + reviewsEvaluation + "\n")

          } catch {

            case e: HttpStatusException => println(e)
          }


          i += 1

        }
      )

    fw.close()

    */

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
