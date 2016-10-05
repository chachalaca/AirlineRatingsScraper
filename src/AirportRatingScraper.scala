import java.io.IOException

import org.jsoup.Jsoup
import org.jsoup.select.Elements
import scala.util.matching.Regex


object AirportRatingScraper {


  def main (args: Array[String]) {

    val countries =
      Jsoup
        .connect("https://www.flightradar24.com/data/airports")
        .timeout(100000)
        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
        .get()
        .select("#tbl-datatable tbody tr td a")


    val iataCodes = new Regex("href=\"[a-zA-Z0-9.\\/:\\-]*\"")
      .findAllIn(countries.toString)
      .map {
        case (s: String) => {val url = s.subSequence(6, s.length-1);println(url);url}
      }
      .flatMap(
        url => {
          val airports = Jsoup
            .connect(url.toString)
            .timeout(100000)
            .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
            .get()
            .select("#tbl-datatable tbody tr td a")

          val code = new Regex("data-iata=\"[A-Z0-9]*\"")
            .findAllIn(airports.toString)
            .map {
              case (s: String) => s.substring(11, s.length-1)
            }

          println(code.toList)

          code

        }
      )
      //.toStream
      //.distinct
      //.size


    println(iataCodes)

  }

}
