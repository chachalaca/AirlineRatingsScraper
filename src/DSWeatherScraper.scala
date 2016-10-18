import java.io.{FileWriter, IOException}
import java.net.{InetSocketAddress, Proxy}
import java.nio.file.{Paths, Files}
import java.sql.{DriverManager, Connection}
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import org.jsoup.select.Elements
import org.jsoup.{HttpStatusException, Jsoup}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.text.Document
import scala.util.Random
import scala.util.matching.Regex
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.io.Source


/**
 * Fetches airports weather history from darksky.net
 * uses MySQL database - see db_structure_flights.sql
 * takes airport coordinates from table `airport`
 */
object DSWeatherScraper {

  // Number of threads to be used
  val numThreads = 10

  // How many days into the past the weather data should be collected?
  val daysCount = 100

  // MYSQL
  val url = "jdbc:mysql://localhost:3306/flights"
  val driver = "com.mysql.cj.jdbc.Driver"
  val username = ""
  val password = ""

  class DSWeatherScraperThread(

    airports: List[(String,Double,Double)],
    connection: Connection

  ) extends Runnable {


    // Proxy server list. SSH tunnels are used as SOCKS proxies in this example.
    val proxies = {
      val proxies = new mutable.Queue[Proxy]()
      8080.to(8089)
        .foreach (p => proxies += new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved("127.0.0.1", p)))
      proxies
    }


    def run(): Unit = {

      airports
        .foreach {
        case ((iata: String, lat: Double, lon: Double)) => {

          println(iata)

          val cal = Calendar.getInstance()

          val query = "INSERT INTO weather (airport_iata, time, summary, precip_intensity, precip_probability, precip_type, temperature, dew_point, humidity, wind_speed, wind_bearing, cloud_cover, pressure, ozone) VALUES"
          val values = new ArrayBuffer[String]()

          1.to(daysCount).foreach(
            i => {

              cal.add(Calendar.DATE, -1)

              val page =
                fetchWeather(lat, lon, cal.getTime)
                  .html()

              val json = new Regex("\\[.+\\]")
                .findFirstIn(page)
                .get

              val parsedItems = Json.parse(json).as[List[JsValue]]

              parsedItems
                .foreach(
                  instance => {

                    values +=
                      "(\"%s\", \"%s\", \"%s\", %s, %s, \"%s\", %s, %s, %s, %s, %s, %s, %s, %s)"
                        .format(
                          iata,
                          new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date((instance \ "time").get.toString().replace("\"", "").toLong * 1000)),
                          (instance \ "summary").getOrElse(JsNull).toString().replace("\"", ""),
                          (instance \ "precipIntensity").getOrElse(JsNull),
                          (instance \ "precipProbability").getOrElse(JsNull),
                          (instance \ "precipType").getOrElse(JsNull).toString().replace("\"", ""),
                          (instance \ "temperature").getOrElse(JsNull),
                          (instance \ "dewPoint").getOrElse(JsNull),
                          (instance \ "humidity").getOrElse(JsNull),
                          (instance \ "windSpeed").getOrElse(JsNull),
                          (instance \ "windBearing").getOrElse(JsNull),
                          (instance \ "cloudCover").getOrElse(JsNull),
                          (instance \ "pressure").getOrElse(JsNull),
                          (instance \ "ozone").getOrElse(JsNull)
                        )

                  }
                )


            }
          )

          if (values.nonEmpty)
            try {
              connection.createStatement.executeUpdate(query + values.mkString(","))
            } catch {

              case e: Exception => {
                println(e)
                values.foreach(println(_))
              }
            }



          }
        }
    }


    private def fetchWeather(lat: Double, lon: Double, date: Date): org.jsoup.nodes.Document = {
      try {

        Jsoup
          .connect(
            "https://darksky.net/%s,%s/%s"
              .format(
                lat.toString,
                lon.toString,
                new SimpleDateFormat("yyyy-MM-dd").format(date)
              )
          )
          .proxy(proxies.front)
          .ignoreContentType(true)
          .timeout(100000)
          .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
          .get()

      }
      catch {

        case e: Exception => {
          println(e)
          println(proxies.front.address())

          proxies.enqueue(proxies.dequeue())

          fetchWeather(lat, lon, date)
        }
      }
    }

  }




  def main (args: Array[String]) {


    Class.forName(driver)
    val connection = DriverManager.getConnection(url, username, password)



    val rs = connection.createStatement.executeQuery("SELECT iata, latitude, longitude FROM airport WHERE iata IS NOT NULL AND latitude IS NOT NULL AND longitude IS NOT NULL AND iata NOT IN (SELECT DISTINCT airport_iata FROM weather) AND iata IN (SELECT DISTINCT airport_iata FROM history) ORDER BY departures_count DESC")
    val airports = ArrayBuffer[(String,Double,Double)]()
    while (rs.next) {
      airports += ((rs.getString("iata"), rs.getDouble("latitude"), rs.getDouble("longitude")))
    }

    println(airports.size)


    airports
      .toList
      .grouped(airports.length/numThreads)
      .foreach {
      case (aptsPart: List[(String,Double,Double)]) => {

          new Thread(new DSWeatherScraperThread(
            aptsPart,
            connection
          )).start()

        }
      }

  }


}
