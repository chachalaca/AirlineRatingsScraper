import java.io.IOException

import org.jsoup.Jsoup
import org.jsoup.select.Elements
import scala.util.matching.Regex

object AirlineRatingScraper {

  var i = 1

  def main(args: Array[String]) {

    var container = fetchAirlineRatingPage()

    while (!container.isEmpty) {

      val carrierCode = container.select("div p").first().text().takeRight(2)

      val carrierType = container.select("div p span").first().text()

      val carrierName = container.select("h2").first().text()

      val ratingPattern = new Regex("[0-9]*\\/[0-9]*")

      val carrierSafetyRating = ratingPattern.findFirstIn(container.select("div").eq(2).text()).getOrElse("").split("\\/", 2)

      val carrierProductRating = ratingPattern.findFirstIn(container.select("div").eq(4).text()).getOrElse("").split("\\/", 2)

      println(
        i + "," +
        (if (carrierCode != "na") carrierCode else "") + "," +
        carrierName + "," +
        carrierType + "," +
        (if (carrierSafetyRating.size > 1) (carrierSafetyRating(0).toFloat / carrierSafetyRating(1).toFloat) else "") + "," +
        (if (carrierProductRating.size > 1) (carrierProductRating(0).toFloat / carrierProductRating(1).toFloat) else "")
      )

      i += 1

      container = fetchAirlineRatingPage()

    }

  }

  private def fetchAirlineRatingPage(): Elements = {
    try {

      val doc = Jsoup.connect("http://www.airlineratings.com/ratings/" + i.toString).get()
      doc.select("div#maincontent.container div.row div.col-md-8 div.hero-unit")

    }
    catch {

      case ioe: IOException => {
        // in case of infinite redirect
        i = i+1
        fetchAirlineRatingPage()
      }

    }

  }


}

