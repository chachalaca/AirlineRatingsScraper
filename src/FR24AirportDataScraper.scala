import java.io.{FileWriter, IOException}

import org.jsoup.select.Elements
import org.jsoup.{HttpStatusException, Jsoup}
import scala.text.Document
import scala.util.matching.Regex
import play.api.libs.json._



object FR24AirportDataScraper {


  def main (args: Array[String]) {

    val countries =
      fetchPage("https://www.flightradar24.com/data/airports")
        .select("#tbl-datatable tbody tr td a")


    val airportUrls = new Regex("href=\"[a-zA-Z0-9.\\/:\\-]*\"")
      .findAllIn(countries.toString)
      .map {
        case (s: String) => s.subSequence(6, s.length-1)
      }
      .toStream
      .distinct


    val iataCodes =
      airportUrls
        .flatMap(
          url => {
            val airports =
              fetchPage(url.toString)
                .select("#tbl-datatable tbody tr td a")

            new Regex("data-iata=\"[A-Z0-9]*\"")
              .findAllIn(airports.toString)
              .map {
                case (s: String) => s.substring(11, s.length-1)
              }

          }
        )
        .distinct


    val fw = new FileWriter("airports.csv")
    fw.write("iata,icao,name,latitude,longitude,altitude,country,city,ratings_avg,ratings_total,reviews_count,reviews_evaluation\n")

    var i = 1

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
        Thread.sleep(10000)

        fetchPage(url)
      }

    }


  }


}
