-- phpMyAdmin SQL Dump
-- version 4.0.4.1
-- http://www.phpmyadmin.net
--
-- Host: 127.0.0.1
-- Generation Time: Mar 05, 2014 at 12:06 AM
-- Server version: 5.6.11
-- PHP Version: 5.5.3

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `scouting`
--
CREATE DATABASE IF NOT EXISTS `scouting` DEFAULT CHARACTER SET latin1 COLLATE latin1_swedish_ci;
USE `scouting`;

-- --------------------------------------------------------

--
-- Table structure for table `configuration_lu`
--

CREATE TABLE IF NOT EXISTS `configuration_lu` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `configuration_desc` text COLLATE latin1_general_cs NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `invalid` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `timestamp` (`timestamp`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs AUTO_INCREMENT=8 ;

--
-- Dumping data for table `configuration_lu`
--

INSERT INTO `configuration_lu` (`id`, `configuration_desc`, `timestamp`, `invalid`) VALUES
(1, 'Long', '2014-01-25 15:18:38', 0),
(2, 'Wide', '2014-01-25 15:18:38', 0),
(3, 'Square', '2014-01-25 15:18:38', 0),
(4, 'Other', '2014-01-25 15:18:38', 0);

-- --------------------------------------------------------

--
-- Table structure for table `event_lu`
--

CREATE TABLE IF NOT EXISTS `event_lu` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `event_name` varchar(70) COLLATE latin1_general_cs NOT NULL,
  `match_url` text COLLATE latin1_general_cs NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `invalid` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `event_name` (`event_name`),
  KEY `timestamp` (`timestamp`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs AUTO_INCREMENT=103 ;

--
-- Dumping data for table `event_lu`
--

INSERT INTO `event_lu` (`id`, `event_name`, `match_url`, `timestamp`, `invalid`) VALUES
(1, 'Buckeye Regional', 'http://www2.usfirst.org/2014comp/Events/OHCL/ScheduleQual.html', '2014-01-25 15:19:51', 0),
(2, 'Greater Pittsburgh Regional', 'http://www2.usfirst.org/2014comp/Events/PAPI/ScheduleQual.html', '2014-01-25 15:19:51', 0),
(3, 'Championship - Archimedes', 'http://www2.usfirst.org/2014comp/Events/Archimedes/ScheduleQual.html', '2014-01-25 15:19:51', 0),
(4, 'Championship - Curie', 'http://www2.usfirst.org/2014comp/Events/Curie/ScheduleQual.html', '2014-01-25 15:19:51', 0),
(5, 'Championship - Galileo', 'http://www2.usfirst.org/2014comp/Events/Galileo/ScheduleQual.html', '2014-01-25 15:19:51', 0),
(6, 'Championship - Newton', 'http://www2.usfirst.org/2014comp/Events/Newton/ScheduleQual.html', '2014-01-25 15:19:51', 0);

-- --------------------------------------------------------

--
-- Table structure for table `fact_cycle_data`
--

CREATE TABLE IF NOT EXISTS `fact_cycle_data` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `event_id` int(5) unsigned NOT NULL,
  `match_id` int(3) unsigned NOT NULL,
  `team_id` int(5) unsigned NOT NULL,
  `cycle_num` int(3) unsigned NOT NULL,
  `near_poss` tinyint(1) unsigned NOT NULL,
  `white_poss` tinyint(1) unsigned NOT NULL,
  `far_poss` tinyint(1) unsigned NOT NULL,
  `truss` tinyint(1) unsigned NOT NULL,
  `catch` tinyint(1) unsigned NOT NULL,
  `high` tinyint(1) unsigned NOT NULL,
  `low` tinyint(1) unsigned NOT NULL,
  `assists` int(3) unsigned NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `invalid` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `event_id` (`event_id`,`match_id`,`team_id`,`cycle_num`),
  KEY `timestamp` (`timestamp`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs AUTO_INCREMENT=28 ;

-- --------------------------------------------------------

--
-- Table structure for table `fact_match_data`
--

CREATE TABLE IF NOT EXISTS `fact_match_data` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `event_id` int(5) unsigned NOT NULL,
  `match_id` int(3) unsigned NOT NULL,
  `team_id` int(5) unsigned NOT NULL,
  `auto_high` int(1) unsigned NOT NULL,
  `auto_high_hot` int(1) unsigned NOT NULL,
  `auto_low` int(1) unsigned NOT NULL,
  `auto_low_hot` int(1) unsigned NOT NULL,
  `high` int(1) unsigned NOT NULL,
  `low` int(1) unsigned NOT NULL,
  `truss` int(3) unsigned NOT NULL,
  `caught` int(3) unsigned NOT NULL,
  `auto_mobile` tinyint(1) unsigned NOT NULL,
  `auto_goalie` tinyint(1) unsigned NOT NULL,
  `num_cycles` int(3) unsigned NOT NULL,
  `foul` tinyint(1) unsigned NOT NULL,
  `tech_foul` tinyint(1) unsigned NOT NULL,
  `tip_over` tinyint(1) unsigned NOT NULL,
  `yellow_card` tinyint(1) unsigned NOT NULL,
  `red_card` tinyint(1) unsigned NOT NULL,
  `notes` varchar(1024) COLLATE latin1_general_cs NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `invalid` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `event_id` (`event_id`,`match_id`,`team_id`),
  KEY `timestamp` (`timestamp`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs AUTO_INCREMENT=10 ;

-- --------------------------------------------------------

--
-- Table structure for table `notes_options`
--

CREATE TABLE IF NOT EXISTS `notes_options` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `option_text` text COLLATE latin1_general_cs NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `invalid` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `timestamp` (`timestamp`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs AUTO_INCREMENT=5 ;

--
-- Dumping data for table `notes_options`
--

INSERT INTO `notes_options` (`id`, `option_text`, `timestamp`, `invalid`) VALUES
(1, 'No Show', '2014-01-25 15:21:36', 0),
(2, 'Non-functional', '2014-01-25 15:21:36', 0),
(3, 'Defender', '2014-01-25 15:21:36', 0),
(4, 'Catcher', '2014-01-25 15:21:36', 0);

-- --------------------------------------------------------

--
-- Table structure for table `robot_lu`
--

CREATE TABLE IF NOT EXISTS `robot_lu` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `team_id` int(5) unsigned NOT NULL,
  `robot_photo` text COLLATE latin1_general_cs NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `invalid` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `team_id` (`team_id`),
  KEY `timestamp` (`timestamp`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `scout_pit_data`
--

CREATE TABLE IF NOT EXISTS `scout_pit_data` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `team_id` int(10) unsigned NOT NULL,
  `configuration_id` int(10) unsigned NOT NULL,
  `wheel_type_id` int(10) unsigned NOT NULL,
  `wheel_base_id` int(10) unsigned NOT NULL,
  `autonomous_mode` tinyint(1) NOT NULL,
  `auto_high` tinyint(1) unsigned NOT NULL,
  `auto_low` tinyint(1) unsigned NOT NULL,
  `auto_hot` tinyint(1) unsigned NOT NULL,
  `auto_mobile` tinyint(1) unsigned NOT NULL,
  `auto_goalie` tinyint(1) unsigned NOT NULL,
  `truss` tinyint(1) unsigned NOT NULL,
  `catch` tinyint(1) unsigned NOT NULL,
  `active_control` tinyint(1) unsigned NOT NULL,
  `launch_ball` tinyint(1) unsigned NOT NULL,
  `score_high` tinyint(1) NOT NULL,
  `score_low` tinyint(1) NOT NULL,
  `max_height` int(10) unsigned NOT NULL,
  `scout_comments` text COLLATE latin1_general_cs NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `invalid` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `timestamp` (`timestamp`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs AUTO_INCREMENT=2 ;

-- --------------------------------------------------------

--
-- Table structure for table `wheel_base_lu`
--

CREATE TABLE IF NOT EXISTS `wheel_base_lu` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `wheel_base_desc` text COLLATE latin1_general_cs NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `invalid` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `timestamp` (`timestamp`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs AUTO_INCREMENT=9 ;

--
-- Dumping data for table `wheel_base_lu`
--

INSERT INTO `wheel_base_lu` (`id`, `wheel_base_desc`, `timestamp`, `invalid`) VALUES
(1, '4 Wheel Drive', '2014-01-25 15:23:47', 0),
(2, '6 Wheel Drive', '2014-01-25 15:23:47', 0),
(3, '8 Wheel Drive+', '2014-01-25 15:23:47', 0),
(4, 'Omnidirectional', '2014-01-25 15:23:47', 0),
(5, 'Other', '2014-01-25 15:23:47', 0);

-- --------------------------------------------------------

--
-- Table structure for table `wheel_type_lu`
--

CREATE TABLE IF NOT EXISTS `wheel_type_lu` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `wheel_type_desc` text COLLATE latin1_general_cs NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `invalid` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `timestamp` (`timestamp`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs AUTO_INCREMENT=11 ;

--
-- Dumping data for table `wheel_type_lu`
--

INSERT INTO `wheel_type_lu` (`id`, `wheel_type_desc`, `timestamp`, `invalid`) VALUES
(1, 'Kit Wheels', '2014-01-25 15:25:39', 0),
(2, 'Omni', '2014-01-25 15:25:39', 0),
(3, 'Mecanum', '2014-01-25 15:25:39', 0),
(4, 'Tank Tread', '2014-01-25 15:25:39', 0),
(5, 'Swerve', '2014-01-25 15:25:39', 0),
(6, 'Other', '2014-01-25 15:25:39', 0);

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
