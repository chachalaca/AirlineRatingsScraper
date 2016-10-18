Flights, airports and carriers data scrapers
============================================

This repository contains collection of scripts for fetching data related to air traffic. Perfect for data science & other applications.

- **Airlines** (low cost/regional/full service, safety rating, product rating)
- **Airports** (latitude, longitude, altitude, country, city, ratings, reviews)
- **Codeshare flights** (marketing carrier code vs. operating carrier code resolution)
- **Flight history** (up to last 100 departures - scheduled vs. actual time)
- **Weather history** (summary, precipitation intensity, precipitation probability, precipitation type, temperature, dew point, humidity, wind speed, wind bearing, cloud cover, atmospheric pressure, ozone - with 1 hour granularity)

Airlines (`ARAirlineRatingScraper`)
-----------------------------------

Fetches data from [airlinertings.com](http://airlinertings.com) and outputs it in CSV file.

Airports (`FR24AirportDataScraper`)
-----------------------------------

Fetches data from [flightradar24.com](http://flightradar24.com) API and outputs it in CSV file. Their API provides more interesting data related to airports than this script saves. Just take a look.

Codeshare flights (`FR24CodeshareScraper`)
------------------------------------------

Many physical flights use multiple flight codes, each for one cooperating carrier [(Wikipedia)](https://en.wikipedia.org/wiki/Codeshare_agreement). This script resolves marketing carrier code vs. operating carrier code. Data is scraped from flightradar24.com search API.

Flight history (`FR24FlightsHistoryScraper`)
--------------------------------------------

Fetches history of flight departures delays (actual vs. scheduled departure time) for up to last 100 departures. Data is taken from [flightradar24.com](http://flightradar24.com) API. It provides more interesting data related to flights history than this script saves (like arrivals etc.). Just take a look.

In order to access data older than 7 days you need to activate premium membership (or free trial) and extract API access token associated with this account. After login and navigating to flights history page the token can be obtained from cookie `frPl` value or from API requests URL. (Use Google Chrome developer tools, tab Network)

Unfortunately FR24 API limits requests from one IP address so that you can do 1 request every ~ 4 seconds. For this reason solution using proxy servers is introduced. Here we use SOCKS proxy using SSH tunnels set by `ssh -D [port] [remote host]`. In this setting proxy server is `localhost` or `127.0.0.1` and ssh tunnels are bind to port numbers `808*`. Remember that you must first extract API tokens for every proxy server. FR24 allows you to get up to 3 valid access tokens for one account. If using multiple FR24 accounts and signed up using Google or Facebook you may experience some troubles obtaining tokens for your additional accounts so you should rather choose classic registration using email and password.

This script loads flightcodes from and saves history items to MySQL database. See `db_structure_flights.sql`.

Weather history (`DSWeatherScraper`)
------------------------------------

Extracts weather history data for every airport with one hour granularity from [darksky.net](http://darksky.net). Their server use certain anti-scraper measures so that you cannot fire requests too often. For this reason we again use array of proxies to overcome this issue and speedup the data acquisition.

This script also uses database whose structure is saved in `db_structure_flights.sql`.