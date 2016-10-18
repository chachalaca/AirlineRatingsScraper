import java.sql.{Connection, DriverManager}
import java.util.concurrent.Executors

import scala.io.Source


object DatasetMerger {

  class DatasetMergerThread(

    records: List[String],
    connection: Connection

  ) extends Runnable {

    def run(): Unit = {


      records
        .foreach(
          record => {

            val r = record.split(",")


            if(r(9)=="both") {

              val res = connection.createStatement().executeUpdate(
                "UPDATE history SET departure_gate=\"%s\", airport_destination_iata=\"%s\" WHERE flightcode=\"%s\" AND departure_scheduled=\"%s\""
                  .format(
                    r(6),
                    r(4),
                    if(r(8)=="") r(7) else r(8),
                    r(5)
                  )
              )

              if(res <= 0) {

                connection.createStatement().executeUpdate(
                  "INSERT INTO history (departure_scheduled, airport_iata, airport_destination_iata, carrier_iata, flightcode, departure_gate) VALUES (\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\")"
                    .format(
                      r(5),
                      r(3),
                      r(4),
                      r(1),
                      if(r(8)=="") r(7) else r(8),
                      r(6)
                    )
                )

              }



            }


          }
        )




    }

  }

  def main (args: Array[String]) {

    val numThreads = 100
    val chunkSize = 1000

    // MYSQL
    val url = "jdbc:mysql://104.155.92.147:3306/flights"
    val driver = "com.mysql.cj.jdbc.Driver"
    val username = "dan"
    val password = "dan"
    Class.forName(driver)
    val connection = DriverManager.getConnection(url, username, password)

    var lines =
      Source
        .fromFile("kiwi_merged.csv")
        .getLines()
        .drop(1)
        .toStream


    var chunk = lines take chunkSize
    lines = lines drop chunkSize

    var i = 0

    while (chunk.nonEmpty) {

      println(i*chunkSize)

      val warps =
        chunk
          .toList
          .grouped(chunk.size/numThreads)
          .toArray

      val pool = Executors.newCachedThreadPool()

      warps
        .indices
        .foreach(
          tid =>
            pool.execute(
              new DatasetMergerThread(
                warps(tid),
                connection
              )
            )
        )

      pool.shutdown()

      while (!pool.isTerminated) {}


      chunk = lines take chunkSize
      lines = lines drop chunkSize

      i += 1

    }








  }

}
