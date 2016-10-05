import java.io.IOException

import org.jsoup.Jsoup
import org.jsoup.select.Elements
import scala.util.matching.Regex
import play.api.libs.json._



object AirportRatingScraper {


  def main (args: Array[String]) {

    val countries =
      Jsoup
        .connect("https://www.flightradar24.com/data/airports")
        .timeout(100000)
        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
        .get()
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
            val airports = Jsoup
              .connect(url.toString)
              .timeout(100000)
              .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
              .get()
              .select("#tbl-datatable tbody tr td a")

            new Regex("data-iata=\"[A-Z0-9]*\"")
              .findAllIn(airports.toString)
              .map {
                case (s: String) => s.substring(11, s.length-1)
              }

          }
        )
        .distinct


    iataCodes
      .foreach(
        iata => {
          val json =
            Jsoup
              .connect("https://api.flightradar24.com/common/v1/airport.json?code="+iata.toLowerCase+"&plugin[]=&plugin-setting[schedule][mode]=&plugin-setting[schedule][timestamp]=1475679982&page=1&limit=25&token=")
              .ignoreContentType(true)
              .timeout(100000)
              .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
              .get()
              .text()



              val parsed = Json.parse(json)

              val icao = (parsed \ "result" \ "response" \ "airport" \ "pluginData" \ "details" \ "code" \ "icao").get
              val name = (parsed \ "result" \ "response" \ "airport" \ "pluginData" \ "details" \ "name").get


        }
      )

  }

}
