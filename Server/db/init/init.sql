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
  recorded_at TIMESTAMP NOT NULL,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  altitude DOUBLE NOT NULL,
  district VARCHAR(128) NOT NULL,
  neighborhood VARCHAR(128) NOT NULL,

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

