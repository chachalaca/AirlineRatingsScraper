import java.io.{FileWriter, IOException}
import java.net.{InetSocketAddress, Proxy}
import java.nio.file.{Paths, Files}
import java.sql.{DriverManager, Connection}
import java.text.SimpleDateFormat
import java.util.Date

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
 * Fetches flight history data (scheduled dpt, actual dpt..) from flightradar24.com
 * In order to access historic data older than 7 days you need API access tokens for premium subscription plan.
 * Uses MySQL database - see db_structure_flights.sql
 * Takes flightcodes from table `flight`
 */
object FR24FlightsHistoryScraper {

  // Number of threads to be used
  val numThreads = 1

  // MYSQL
  val url = "jdbc:mysql://localhost:3306/flights"
  val driver = "com.mysql.cj.jdbc.Driver"
  val username = ""
  val password = ""


  // List of proxy servers with flightradar24 access tokens
  // (ip, port, token)
  // Don't forget to set proxy type correctly. Choose SOCKS if using SSH tunnels as proxy.
  val pwt =
    List(
      ("127.0.0.1", 8080, ""),
      ("127.0.0.1", 8081, ""),
      ("127.0.0.1", 8082, ""),
      ("127.0.0.1", 8083, ""),
      ("127.0.0.1", 8084, ""),
      ("127.0.0.1", 8085, ""),
      ("127.0.0.1", 8086, ""),
      ("127.0.0.1", 8087, ""),
      ("127.0.0.1", 8088, "")
    )
      .map (t => (new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(t._1, t._2)), t._3))


  class FR24FlightsHistoryThread(

    proxiesWithTokens: List[(Proxy, String)],
    flightCodes: List[String],
    connection: Connection

  ) extends Runnable {

    val proxies = {val proxies = new mutable.Queue[(Proxy, String)]; proxiesWithTokens.foreach(t => proxies += t); proxies}

    def run(): Unit = {
      var i = 1

      flightCodes
        .foreach(
          code => {

            println(code)

            val json =
              fetchFlightHistory(code)
                .text()

            val parsed = Json.parse(json)


            val items = (parsed \ "result" \ "response" \ "data").as[List[JsValue]]


              val query = "INSERT INTO history (departure_scheduled,departure_actual,airport_iata,carrier_iata,flightcode) VALUES"
              val values = new ArrayBuffer[String]()

              items
                .foreach(
                  instance => {

                    val airport = (instance \ "airport" \ "origin" \ "code" \ "iata").getOrElse(JsNull)
                    val carrier = (instance \ "airline" \ "code" \ "iata").getOrElse(JsNull)
                    val scheduledDeparture = (instance \ "time" \ "scheduled" \ "departure").getOrElse(JsNull)
                    val actualDeparture = (instance \ "time" \ "real" \ "departure").getOrElse(JsNull)

                    if(scheduledDeparture != JsNull && actualDeparture != JsNull) {

                      values +=
                        "(\"%s\",\"%s\",%s,%s,\"%s\")"
                          .format(
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(scheduledDeparture.toString().replace("\"", "").toLong*1000)),
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(actualDeparture.toString().replace("\"", "").toLong*1000)),
                            airport,
                            carrier,
                            code
                          )

                    }

                  }
                )

              if(values.nonEmpty)
                connection.createStatement.executeUpdate(query + values.mkString(","))

            i += 1

          }
        )
    }


    private def fetchFlightHistory(fltCode: String): org.jsoup.nodes.Document = {
      try {

        val (proxy, token) = proxies.dequeue()
        proxies.enqueue((proxy, token))

        print(proxy.address()+" ")

        Jsoup
          .connect(
            String.format(
              "https://api.flightradar24.com/common/v1/flight/list.json?query=%s&fetchBy=flight&page=1&limit=100&token=%s&timestamp=%s",
              fltCode.toLowerCase,
              token, // token
              (System.currentTimeMillis / 1000).toString
            )
          )
          .proxy(proxy)
          .ignoreContentType(true)
          .timeout(100000)
          .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
          .get()

      }
      catch {

        case e: Exception => {
          println(e)

          proxies.enqueue(proxies.dequeue())

          fetchFlightHistory(fltCode)
        }
      }
    }

  }


  def main (args: Array[String]) {

    Class.forName(driver)
    val connection = DriverManager.getConnection(url, username, password)

    val pwtGroups =
      pwt
        .grouped(pwt.size/numThreads)
        .toArray


    val rs = connection.createStatement.executeQuery("SELECT code FROM flight WHERE code NOT IN (SELECT DISTINCT flightcode FROM history)")
    val flightCodes = ArrayBuffer[String]()
    while (rs.next) {
      flightCodes += rs.getString("code")
    }


    flightCodes
      .toList
      .grouped(flightCodes.length/numThreads)
      .grouped(numThreads)
      .foreach {
        case (warp: List[List[String]]) => {

          val pwtQueue = {
            val pwtQueue = new mutable.Queue[List[(Proxy, String)]]; pwtGroups.foreach(t => pwtQueue += t); pwtQueue
          }

          warp
            .foreach {
              case (flightCodes: List[String]) =>

                new Thread(new FR24FlightsHistoryThread(
                  proxiesWithTokens = pwtQueue.dequeue(),
                  flightCodes = flightCodes,
                  connection = connection
                )).start()

            }
        }
      }
  }

}
