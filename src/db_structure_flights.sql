SET NAMES utf8;
SET time_zone = '+00:00';
SET foreign_key_checks = 0;
SET sql_mode = 'NO_AUTO_VALUE_ON_ZERO';

DROP TABLE IF EXISTS `airport`;
CREATE TABLE `airport` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `iata` char(3) DEFAULT NULL,
  `icao` char(4) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `altitude` int(11) DEFAULT NULL,
  `ratings_avg` float DEFAULT NULL,
  `ratings_total` int(11) DEFAULT NULL,
  `reviews_count` int(11) DEFAULT NULL,
  `reviews_evaluation` float DEFAULT NULL,
  `timezone` float DEFAULT NULL,
  `timezone_string` varchar(255) DEFAULT NULL,
  `dst` char(2) DEFAULT NULL,
  `departures_count` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `iata_index` (`iata`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `carrier`;
CREATE TABLE `carrier` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `iata` char(2) DEFAULT NULL,
  `icao` char(3) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `type` varchar(255) DEFAULT NULL,
  `safety_rating` double DEFAULT NULL,
  `product_rating` double DEFAULT NULL,
  `passengers_carried` int(11) DEFAULT NULL,
  `fleet` int(11) DEFAULT NULL,
  `departures_count` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `flight`;
CREATE TABLE `flight` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(10) NOT NULL,
  `carrier_iata` char(2) DEFAULT NULL,
  `codeshare` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `codeshare` (`codeshare`),
  CONSTRAINT `flight_ibfk_1` FOREIGN KEY (`codeshare`) REFERENCES `flight` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `history`;
CREATE TABLE `history` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `flight` int(11) DEFAULT NULL,
  `departure_scheduled` datetime NOT NULL,
  `departure_actual` datetime DEFAULT NULL,
  `airport_iata` char(3) DEFAULT NULL,
  `airport_destination_iata` char(3) DEFAULT NULL,
  `carrier_iata` char(2) DEFAULT NULL,
  `flightcode` varchar(10) NOT NULL,
  `departure_gate` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `flight` (`flight`),
  KEY `flightcode_index` (`flightcode`),
  KEY `departure_scheduled_index` (`departure_scheduled`),
  KEY `departure_gate_index` (`departure_gate`),
  KEY `airport_iata_index` (`airport_iata`),
  CONSTRAINT `history_ibfk_1` FOREIGN KEY (`flight`) REFERENCES `flight` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `weather`;
CREATE TABLE `weather` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `airport_iata` char(3) NOT NULL,
  `time` datetime NOT NULL,
  `summary` varchar(255) DEFAULT NULL,
  `precip_intensity` float DEFAULT NULL,
  `precip_probability` float DEFAULT NULL,
  `precip_type` varchar(100) DEFAULT NULL,
  `temperature` float DEFAULT NULL,
  `dew_point` float DEFAULT NULL,
  `humidity` float DEFAULT NULL,
  `wind_speed` float DEFAULT NULL,
  `wind_bearing` float DEFAULT NULL,
  `cloud_cover` float DEFAULT NULL,
  `pressure` float DEFAULT NULL,
  `ozone` float DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;