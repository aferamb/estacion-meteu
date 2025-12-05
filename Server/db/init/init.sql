-- Initialization script for MariaDB
CREATE DATABASE IF NOT EXISTS UBICOMP CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE UBICOMP;

-- Create measurement table expected by the Java code
CREATE TABLE IF NOT EXISTS MEASUREMENT (
  VALUE INT NOT NULL,
  DATE TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert some sample rows
INSERT INTO MEASUREMENT (VALUE, DATE) VALUES (42, '2025-09-18 09:00:00');
INSERT INTO MEASUREMENT (VALUE, DATE) VALUES (100, '2025-09-19 10:30:00');

-- Table to store sensor readings from the weather stations
-- Columns for identification and position are NOT NULL (always provided).
-- Measurement fields are nullable because sensors may omit some values.
CREATE TABLE IF NOT EXISTS sensor_readings (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sensor_id VARCHAR(64) NOT NULL,
  sensor_type VARCHAR(64) NOT NULL,
  street_id VARCHAR(64) NOT NULL,
    -- No automatic timestamps: recorded_at is provided by the sensor
  recorded_at TIMESTAMP NULL,
  latitude DOUBLE NULL,
  longitude DOUBLE NULL,
  altitude DOUBLE NULL,
  district VARCHAR(128) NULL,
  neighborhood VARCHAR(128) NULL,

  -- Common sensor measurements (nullable)
  temp DOUBLE NULL,
  humid DOUBLE NULL,
  aqi INT NULL,
  lux DOUBLE NULL,
  sound_db DOUBLE NULL,
  atmhpa DOUBLE NULL,
  uv_index DOUBLE NULL,

  -- Extra fields broken out into individual nullable columns
  bsec_status INT NULL,
  iaq DOUBLE NULL,
  static_iaq DOUBLE NULL,
  co2_eq DOUBLE NULL,
  breath_voc_eq DOUBLE NULL,
  raw_temperature DOUBLE NULL,
  raw_humidity DOUBLE NULL,
  pressure_hpa DOUBLE NULL,
  gas_resistance_ohm DOUBLE NULL,
  gas_percentage DOUBLE NULL,
  stabilization_status DOUBLE NULL,
  run_in_status DOUBLE NULL,
  sensor_heat_comp_temp DOUBLE NULL,
  sensor_heat_comp_hum DOUBLE NULL,

  -- No automatic timestamps: recorded_at is provided by the sensor

  INDEX idx_sensor_time (sensor_id, recorded_at),
  INDEX idx_lat_long (latitude, longitude)
);

-- Table to store user credentials and roles
CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(512) NOT NULL,
  salt VARCHAR(128) NULL, 
  role VARCHAR(64) DEFAULT 'user',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_login TIMESTAMP NULL,
  failed_attempts INT DEFAULT 0,
  disabled TINYINT(1) DEFAULT 0
);

CREATE TABLE IF NOT EXISTS subscriptions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  topic VARCHAR(255) NOT NULL UNIQUE,
  data_topic VARCHAR(255) NOT NULL,
  alert_topic VARCHAR(255) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  active TINYINT(1) DEFAULT 1,
  INDEX idx_data_topic (data_topic)
);

-- Table that stores configurable allowed ranges for data parameters
CREATE TABLE IF NOT EXISTS parameter_ranges (
  parameter VARCHAR(64) PRIMARY KEY,
  min_value DOUBLE NULL,
  max_value DOUBLE NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert sensible defaults (can be adjusted later)
INSERT IGNORE INTO parameter_ranges (parameter, min_value, max_value) VALUES
('temp', -40.0, 60.0),
('humid', 0.0, 100.0),
('aqi', 0.0, 500.0),
('lux', 0.0, 100000.0),
('sound_db', 0.0, 140.0),
('atmhpa', 300.0, 1100.0),
('uv_index', 0.0, 15.0);

-- Table to record alarm events triggered by stations
CREATE TABLE IF NOT EXISTS sensor_alarms (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sensor_id VARCHAR(64) NULL,
  street_id VARCHAR(64) NULL,
  parameter VARCHAR(64) NOT NULL,
  triggered_value DOUBLE NOT NULL,
  triggered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP NULL,
  resolved_value DOUBLE NULL,
  active TINYINT(1) DEFAULT 1,
  INDEX idx_sensor_param (sensor_id, parameter, active)
);

INSERT INTO users (username,password_hash,salt,role,created_at,failed_attempts,disabled) VALUES ('admin','sVZAr3dXz/euzAJzL00GxsZ3ieowJKmUgYIf2atlpcY=','ZygvP9ubzITBwrXovI2ANQ==','admin', NOW(), 0, 0);