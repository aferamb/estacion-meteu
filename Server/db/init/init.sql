-- Initialization script for MariaDB
CREATE DATABASE IF NOT EXISTS UBICOMP CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE UBICOMP;

-- Create measurement table expected by the Java code
-- Create measurement table with richer schema to store sensor metadata and JSON payloads
CREATE TABLE IF NOT EXISTS MEASUREMENT (
  id INT AUTO_INCREMENT PRIMARY KEY,
  sensor_id VARCHAR(128) NOT NULL,
  sensor_type VARCHAR(64) NOT NULL,
  street_id VARCHAR(64),
  timestamp DATETIME NOT NULL,
  location JSON,
  data JSON,
  extra JSON,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX (sensor_id),
  INDEX (sensor_type),
  INDEX (timestamp)
);

-- Insert sample rows using JSON for `location` and `data`
INSERT INTO MEASUREMENT (sensor_id, sensor_type, street_id, timestamp, location, data) VALUES (
  'LABJAV08-G1', 'weather', 'ST_0686', '2025-09-18 09:00:00',
  JSON_OBJECT('lat', 40.3971536, 'long', -3.6734246, 'alt', 650.0, 'district', 'Arganzuela', 'neighborhood', 'Imperial'),
  JSON_OBJECT('temp', 21.5, 'humid', 45.2, 'aqi', 42, 'lux', 120.0, 'sound_db', 55.3, 'atmhpa', 1013.2, 'uv_index', 2)
);

INSERT INTO MEASUREMENT (sensor_id, sensor_type, street_id, timestamp, location, data) VALUES (
  'LABJAV08-G1', 'weather', 'ST_0686', '2025-09-19 10:30:00',
  JSON_OBJECT('lat', 40.3971536, 'long', -3.6734246, 'alt', 650.0, 'district', 'Arganzuela', 'neighborhood', 'Imperial'),
  JSON_OBJECT('temp', 23.7, 'humid', 50.1, 'aqi', 100, 'lux', 300.0, 'sound_db', 60.0, 'atmhpa', 1012.8, 'uv_index', 3)
);

-- Create users table for authentication/authorization
CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(512) NOT NULL,
  salt VARCHAR(128) NOT NULL,
  role VARCHAR(64) DEFAULT 'user',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert an example user (password must be hashed in real use; here it's placeholder)
INSERT INTO users (username, password_hash, salt, role) VALUES ('admin', 'changeme_hash', 'changeme_salt', 'admin');
