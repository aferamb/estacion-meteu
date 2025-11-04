/*
  prueva_pantalla_comp.ino

  Sketch para ESP32 que muestra datos de múltiples sensores en pantalla TFT (TFT_eSPI)
  y los publica en un servidor MQTT en formato JSON. También recibe avisos desde MQTT
  y los muestra en rojo junto a los datos.

  Requisitos:
  - Biblioteca TFT_eSPI (configurada mediante User_Setup.h en la carpeta de la librería)
  - Biblioteca PubSubClient
  - Configurar SSID/Password y MQTT broker abajo

  Nota: Este sketch incluye una función pública `publishAndShowSensorData(...)` que
  puedes llamar desde tu código cuando lleguen datos reales del sensor.
*/

#include <WiFi.h>
#include <PubSubClient.h>
#include <TFT_eSPI.h>
#include <time.h>
// BME680 / BSEC (usar I2C)
#include "bsec.h"
#include <Wire.h>

// ---------------------- CONFIGURACIÓN ----------------------
const char* ssid = "Martin Router King";
const char* password = "reconocer";

const char* mqtt_server = "test.mosquitto.org"; // Broker MQTT
const int mqtt_port = 1883;
const char* mqtt_user = "";
const char* mqtt_pass = "";

// topic donde publicaremos los JSON (puedes cambiarlo)
const char* topic_pub = "sensors/ws/data";
// topic para recibir avisos (alerts) desde el servidor
const char* topic_alerts = "sensors/alerts/#";

// Identificador del sensor (se incluye dentro del JSON)
const char* SENSOR_ID = "LABJAV08-G1";
const char* SENSOR_TYPE = "weather";
const char* STREET_ID = "ST_0686";

// ---------------------- LCD / TFT ----------------------
TFT_eSPI tft = TFT_eSPI();
#define TFT_GREY 0x5AEB
const uint16_t BG = TFT_GREY; // color de fondo (definido en User_Setup.h)

// Layout constants
const int LEFT_COL_X = 8;
const int RIGHT_COL_X = 150;
const int ROW_HEIGHT = 20;
// Alert box (small, top-right corner)
const int ALERT_X = 190;
const int ALERT_Y = 4;
const int ALERT_W = 46;
const int ALERT_H = 14;
// Bottom alert banner (large, red, centered text)
const int ALERT_B_X = 0;
const int ALERT_B_W = 240;
const int ALERT_B_H = 28; // height of banner
const int ALERT_B_Y = 240 - ALERT_B_H - 2; // 2px margin from bottom

// ---------------------- MQTT / WiFi ----------------------
WiFiClient espClient;
PubSubClient mqttClient(espClient);

String lastAlert = "";       // último aviso recibido
unsigned long alertTime = 0;  // cuando llegó

// ---------------------- UTILIDADES y ESTRUCTURAS ----------------------
struct Location {
  double latitude;
  double longitude;
  double altitude_meters;
  String district;
  String neighborhood;
};

struct WeatherData {
  float temperature_celsius;
  float humidity_percent;
  int air_quality_index;
  float luminosity_lux;         // sensor de luminosidad (lux)
  float sound_db;              // contaminación acústica (dB)
  float atmospheric_pressure_hpa;
  int uv_index;
};

// ---------------------- DECLARACIONES ----------------------
void setup_wifi();
void mqttCallback(char* topic, byte* payload, unsigned int length);
void mqttReconnect();
void drawHeader();
void clearDataArea();
void drawDataLine(int row, const String &paramName, const String &value);
String isoTimeNow();
void publishAndShowSensorData(const char* sensor_id, const char* sensor_type, const char* street_id, const Location &loc, const WeatherData &data);
// BSEC / sensor
void checkIaqSensorStatus(void);

// Create an object of the class Bsec
Bsec iaqSensor;

// I2C pins for ESP32
const uint8_t I2C_SDA = 21; // SDA
const uint8_t I2C_SCL = 22; // SCL

// ---------------------- SETUP ----------------------
void setup() {
  Serial.begin(115200);
  delay(500);

  // Inicializar TFT
  tft.init();
  tft.setRotation(0);
  tft.fillScreen(BG);
  tft.setTextSize(1);
  tft.setTextColor(TFT_WHITE, BG);

  drawHeader();

  // Conectar WiFi
  setup_wifi();

  // Inicializar NTP para timestamps (intenta sincronizar la hora)
  configTime(0, 0, "pool.ntp.org", "time.nist.gov");

  // Inicializar MQTT
  mqttClient.setClient(espClient);
  mqttClient.setServer(mqtt_server, mqtt_port);
  mqttClient.setCallback(mqttCallback);

  // --- Inicializar BME680 / BSEC via I2C ---
  Serial.println("Inicializando I2C para BME680...");
  Wire.begin(I2C_SDA, I2C_SCL); // SDA, SCL
  Wire.setClock(400000); // fast mode (opcional)
  iaqSensor.begin(BME68X_I2C_ADDR_LOW, Wire); // usar LOW (0x76) si SDO a GND
  Serial.println("Llamada a iaqSensor.begin() realizada (I2C)");
  checkIaqSensorStatus();

  bsec_virtual_sensor_t sensorList[13] = {
    BSEC_OUTPUT_IAQ,
    BSEC_OUTPUT_STATIC_IAQ,
    BSEC_OUTPUT_CO2_EQUIVALENT,
    BSEC_OUTPUT_BREATH_VOC_EQUIVALENT,
    BSEC_OUTPUT_RAW_TEMPERATURE,
    BSEC_OUTPUT_RAW_PRESSURE,
    BSEC_OUTPUT_RAW_HUMIDITY,
    BSEC_OUTPUT_RAW_GAS,
    BSEC_OUTPUT_STABILIZATION_STATUS,
    BSEC_OUTPUT_RUN_IN_STATUS,
    BSEC_OUTPUT_SENSOR_HEAT_COMPENSATED_TEMPERATURE,
    BSEC_OUTPUT_SENSOR_HEAT_COMPENSATED_HUMIDITY,
    BSEC_OUTPUT_GAS_PERCENTAGE
  };
  iaqSensor.updateSubscription(sensorList, 13, BSEC_SAMPLE_RATE_LP);
  checkIaqSensorStatus();

  // Mostrar nota de inicio
  // Mostrar nota de inicio por encima de la franja de alerta
  tft.drawCentreString("Esperando datos...", 120, ALERT_B_Y - 12, 4);
}

// ---------------------- LOOP ----------------------
unsigned long lastDemo = 0;
void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    setup_wifi();
  }

  if (!mqttClient.connected()) {
    mqttReconnect();
  }
  mqttClient.loop();

  // Leer datos reales del BME680/BSEC y publicarlos cuando haya datos nuevos
  if (iaqSensor.run()) {
    // throttle publishes to at most once por segundo
    if (millis() - lastDemo > 1000) {
      lastDemo = millis();
  Location loc = {40.3971536, -3.6734246, 650.0, "Arganzuela", "Imperial"};
  WeatherData d;
  // Rellenar con lecturas reales cuando estén disponibles
  d.temperature_celsius = iaqSensor.temperature;        // temperatura compensada
  d.humidity_percent = iaqSensor.humidity;
  // BSEC devuelve IAQ (0..500 aprox). Lo usamos como AQI aproximado
  d.air_quality_index = (int)iaqSensor.iaq;
  // Sensores no presentes: dejar a 0
  d.luminosity_lux = 0.0;
  d.sound_db = 0.0;
  d.atmospheric_pressure_hpa = iaqSensor.pressure / 100.0; // convertir Pa -> hPa
  d.uv_index = 0;

      publishAndShowSensorData(SENSOR_ID, SENSOR_TYPE, STREET_ID, loc, d);
    }
  } else {
    // Si no hay nuevos datos, revisar estado ocasionalmente
    checkIaqSensorStatus();
  }

  // Borrar alerta después de 10 s (limpiar la franja inferior)
  if (lastAlert.length() && millis() - alertTime > 10000) {
    lastAlert = "";
    // repintar la franja inferior (banner)
    tft.fillRect(ALERT_B_X, ALERT_B_Y, ALERT_B_W, ALERT_B_H, BG);
    // restore text color for subsequent drawings
    tft.setTextColor(TFT_WHITE, BG);
    tft.setTextSize(1);
  }
}

// ---------------------- FUNCIONES ----------------------
void setup_wifi() {
  if (WiFi.status() == WL_CONNECTED) return;
  Serial.print("Conectando a "); Serial.println(ssid);
  WiFi.begin(ssid, password);
  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED) {
    delay(250);
    Serial.print('.');
    if (millis() - start > 15000) { // 15s timeout
      Serial.println("\nNo se pudo conectar a WiFi, reintentando...");
      start = millis();
    }
  }
  Serial.println();
  Serial.println("WiFi conectado");
  Serial.print("IP: "); Serial.println(WiFi.localIP());
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {
  // Convertir a String
  String msg;
  for (unsigned int i = 0; i < length; i++) msg += (char)payload[i];
  Serial.print("MQTT msg ["); Serial.print(topic); Serial.print("]: "); Serial.println(msg);

  // Si es una alerta, guardamos y la mostramos en rojo
  // Podemos establecer convención: topic contiene "alerts" o bien topic == sensors/alerts/<id>
  if (String(topic).startsWith("sensors/alerts")) {
    // store alert and display a prominent banner at the bottom with red background
    lastAlert = msg;
    alertTime = millis();
    // prepare short message (truncate to fit reasonably in banner)
    String shortMsg = msg;
    if (shortMsg.length() > 36) shortMsg = shortMsg.substring(0, 33) + "...";
    // draw red banner and centered white text
    tft.fillRect(ALERT_B_X, ALERT_B_Y, ALERT_B_W, ALERT_B_H, TFT_RED);
    tft.setTextColor(TFT_WHITE, TFT_RED);
    tft.setTextSize(2); // larger text for emphasis
    // Draw centered; use drawCentreString for font index 4 (bigger) if available
    // fall back to drawCentreString with current text size
    tft.drawCentreString(shortMsg, ALERT_B_X + ALERT_B_W / 2, ALERT_B_Y + 4, 4);
  }
}

void mqttReconnect() {
  if (mqttClient.connected()) return;
  Serial.print("Conectando MQTT...");
  String clientId = String("ESP32-") + String((uint32_t)ESP.getEfuseMac(), HEX);
  if (mqttClient.connect(clientId.c_str(), mqtt_user, mqtt_pass)) {
    Serial.println(" conectado bajo ID: " + clientId);
    mqttClient.subscribe(topic_alerts);
    // también suscribimos a cualquier otro topic que necesitemos
  } else {
    Serial.print(" fallo, rc="); Serial.println(mqttClient.state());
    delay(2000);
  }
}

void drawHeader() {
  tft.fillScreen(BG);
  tft.setTextSize(1);
  tft.setTextColor(TFT_WHITE, BG);
  tft.drawCentreString("Weather Station", 120, 2, 4);
  tft.drawFastHLine(0, 36, 240, TFT_WHITE);
  // Column headers: only Param and Valor (no sensor id)
  tft.drawString("Param", LEFT_COL_X, 40, 2);
  tft.drawString("Valor", RIGHT_COL_X, 40, 2);
}

void clearDataArea() {
  // Fill only the data area and avoid overwriting the bottom alert banner.
  int dataTop = 56;
  int dataBottom = ALERT_B_Y; // reserve area below for alert banner
  int height = dataBottom - dataTop;
  if (height > 0) {
    tft.fillRect(0, dataTop, 240, height, BG);
  } else {
    // fallback: clear whole area if calculation fails
    tft.fillRect(0, 56, 240, 200, BG);
  }
}

// Dibuja una línea de datos: row 0..n
// Draw a single data line: left = parameter name, right = value
void drawDataLine(int row, const String &paramName, const String &value) {
  int y = 56 + row * ROW_HEIGHT;
  // clear the line area
  tft.fillRect(0, y, 240, ROW_HEIGHT, BG);
  tft.setTextSize(1);
  tft.setTextColor(TFT_WHITE, BG);
  tft.drawString(paramName, LEFT_COL_X, y+2, 2);
  tft.drawRightString(value, RIGHT_COL_X + 80, y+2, 2);
}

// Devuelve timestamp ISO8601 UTC con milisegundos si la hora está sincronizada
String isoTimeNow() {
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo)) {
    // Si no hay hora, usar millis como fallback
    unsigned long ms = millis();
    char buf[64];
    snprintf(buf, sizeof(buf), "1970-01-01T00:00:%02lu.%03luZ", (ms/1000)%60, ms%1000);
    return String(buf);
  }
  char buf[64];
  struct timeval tv;
  gettimeofday(&tv, NULL);
  int ms = tv.tv_usec / 1000;
  strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%S", &timeinfo);
  char out[80];
  snprintf(out, sizeof(out), "%s.%03dZ", buf, ms);
  return String(out);
}

// Construye JSON y publica por MQTT; además muestra datos en la pantalla.
void publishAndShowSensorData(const char* sensor_id, const char* sensor_type, const char* street_id, const Location &loc, const WeatherData &data) {
  // 1) Mostrar en pantalla (táctica: 7 líneas para 'data')
  clearDataArea();
  drawDataLine(0, String("temperature (C)"), String(data.temperature_celsius, 1));
  drawDataLine(1, String("humidity (%)"), String(data.humidity_percent, 1));
  drawDataLine(2, String("AQI"), String(data.air_quality_index));
  drawDataLine(3, String("luminosity (lux)"), String(data.luminosity_lux, 1));
  drawDataLine(4, String("sound (dB)"), String(data.sound_db, 1));
  drawDataLine(5, String("pressure (hPa)"), String(data.atmospheric_pressure_hpa, 1));
  drawDataLine(6, String("UV index"), String(data.uv_index));

  // Alerts are handled separately by mqttCallback and cleared in loop();

  // 2) Construir JSON
  String timestamp = isoTimeNow();
  String json = "{";
  json += "\"sensor_id\": \""; json += sensor_id; json += "\",";
  json += "\"sensor_type\": \""; json += sensor_type; json += "\",";
  json += "\"street_id\": \""; json += street_id; json += "\",";
  json += "\"timestamp\": \""; json += timestamp; json += "\",";
  json += "\"location\": {";
    json += "\"lat\": "; json += String(loc.latitude, 6); json += ",";
    json += "\"long\": "; json += String(loc.longitude, 6); json += ",";
    json += "\"alt\": "; json += String(loc.altitude_meters, 1); json += ",";
    json += "\"district\": \""; json += loc.district; json += "\",";
    json += "\"neighborhood\": \""; json += loc.neighborhood; json += "\"";
  json += "},";
  json += "\"data\": {";
    json += "\"temp\": "; json += String(data.temperature_celsius, 2); json += ",";
    json += "\"humid\": "; json += String(data.humidity_percent, 2); json += ",";
    json += "\"aqi\": "; json += String(data.air_quality_index); json += ",";
  json += "\"lux\": "; json += String(data.luminosity_lux, 2); json += ",";
  json += "\"sound_db\": "; json += String(data.sound_db, 2); json += ",";
    json += "\"atmhpa\": "; json += String(data.atmospheric_pressure_hpa, 2); json += ",";
    json += "\"uv_index\": "; json += String(data.uv_index);
  json += "}";
  json += "}";

  // 3) Publicar por MQTT
  if (mqttClient.connected()) {
    bool ok = mqttClient.publish(topic_pub, json.c_str()); // retain=true
    int json_length = json.length();
    Serial.print("Longitud JSON: "); Serial.print(json_length); Serial.println(" bytes");
    Serial.print("Publicado JSON (ok="); Serial.print(ok); Serial.print(") -> "); Serial.println(json);
  } else {
    Serial.println("No conectado a MQTT: no se publica");
  }
}

// ---------------------- BSEC / helper ----------------------
void checkIaqSensorStatus(void)
{
  if (iaqSensor.bsecStatus != BSEC_OK) {
    if (iaqSensor.bsecStatus < BSEC_OK) {
      Serial.print("BSEC error code : "); Serial.println(iaqSensor.bsecStatus);
      // no bloqueamos aquí; simplemente lo informamos
    } else {
      Serial.print("BSEC warning code : "); Serial.println(iaqSensor.bsecStatus);
    }
  }

  if (iaqSensor.bme68xStatus != BME68X_OK) {
    if (iaqSensor.bme68xStatus < BME68X_OK) {
      Serial.print("BME68X error code : "); Serial.println(iaqSensor.bme68xStatus);
      // informar y continuar
    } else {
      Serial.print("BME68X warning code : "); Serial.println(iaqSensor.bme68xStatus);
    }
  }
}
