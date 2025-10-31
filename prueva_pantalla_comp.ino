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

// ---------------------- CONFIGURACIÓN ----------------------
const char* ssid = "Tu_SSID";
const char* password = "Tu_PASSWORD";

const char* mqtt_server = "test.mosquitto.org"; // Broker MQTT
const int mqtt_port = 1883;
const char* mqtt_user = "";
const char* mqtt_pass = "";

// topic donde publicaremos los JSON (puedes cambiarlo)
const char* topic_pub = "sensors/ws/data";
// topic para recibir avisos (alerts) desde el servidor
const char* topic_alerts = "sensors/alerts/#";

// Identificador del sensor (se incluye dentro del JSON)
const char* SENSOR_ID = "WS_001";
const char* SENSOR_TYPE = "weather";
const char* STREET_ID = "ST_001";

// ---------------------- LCD / TFT ----------------------
TFT_eSPI tft = TFT_eSPI();
const uint16_t BG = TFT_GREY; // color de fondo (definido en User_Setup.h)

// Layout constants
const int LEFT_COL_X = 8;
const int RIGHT_COL_X = 150;
const int ROW_HEIGHT = 20;

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
  float wind_speed_kmh;
  int wind_direction_degrees;
  float atmospheric_pressure_hpa;
  int uv_index;
};

// ---------------------- DECLARACIONES ----------------------
void setup_wifi();
void mqttCallback(char* topic, byte* payload, unsigned int length);
void mqttReconnect();
void drawHeader();
void clearDataArea();
void drawDataLine(int row, const String &sensorName, const String &paramName, const String &value);
String isoTimeNow();
void publishAndShowSensorData(const char* sensor_id, const char* sensor_type, const char* street_id, const Location &loc, const WeatherData &data);

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

  // Mostrar nota de inicio
  tft.drawCentreString("Esperando datos...", 120, 220, 4);
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

  // Ejemplo/demo: publicar datos ficticios cada 5 segundos
  if (millis() - lastDemo > 5000) {
    lastDemo = millis();
    Location loc = {40.416775, -3.703790, 650.0, "Centro", "Sol"};
    WeatherData d;
    d.temperature_celsius = 20.0 + random(-50,50)/10.0; // simulación
    d.humidity_percent = 50.0 + random(-200,200)/10.0;
    d.air_quality_index = random(10,80);
    d.wind_speed_kmh = random(0,200)/10.0;
    d.wind_direction_degrees = random(0,360);
    d.atmospheric_pressure_hpa = 1010.0 + random(-200,200)/10.0;
    d.uv_index = random(0,11);

    publishAndShowSensorData(SENSOR_ID, SENSOR_TYPE, STREET_ID, loc, d);
  }

  // Borrar alerta después de 10 s
  if (lastAlert.length() && millis() - alertTime > 10000) {
    lastAlert = "";
    // repintar zona de alertas
    tft.fillRect(RIGHT_COL_X, 8, 120, 40, BG);
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
    lastAlert = msg;
    alertTime = millis();
    // mostrar en pantalla en rojo
    tft.setTextColor(TFT_RED, BG);
    tft.setTextSize(1);
    tft.fillRect(RIGHT_COL_X, 8, 120, 40, BG);
    tft.drawString("ALERTA:", RIGHT_COL_X, 8, 4);
    tft.drawString(msg, RIGHT_COL_X, 28, 2);
    tft.setTextColor(TFT_WHITE, BG);
  }
}

void mqttReconnect() {
  if (mqttClient.connected()) return;
  Serial.print("Conectando MQTT...");
  String clientId = String("ESP32-") + String((uint32_t)ESP.getEfuseMac(), HEX);
  if (mqttClient.connect(clientId.c_str(), mqtt_user, mqtt_pass)) {
    Serial.println(" conectado");
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
  // Columna izquierda - datos
  tft.drawString("Sensor", LEFT_COL_X, 40, 2);
  tft.drawString("Param", LEFT_COL_X + 70, 40, 2);
  tft.drawString("Valor", RIGHT_COL_X, 40, 2);
}

void clearDataArea() {
  tft.fillRect(0, 56, 240, 200, BG);
}

// Dibuja una línea de datos: row 0..n
void drawDataLine(int row, const String &sensorName, const String &paramName, const String &value) {
  int y = 56 + row * ROW_HEIGHT;
  // borrar área de la línea
  tft.fillRect(0, y, 240, ROW_HEIGHT, BG);
  tft.setTextSize(1);
  tft.setTextColor(TFT_WHITE, BG);
  tft.drawString(sensorName, LEFT_COL_X, y+2, 2);
  tft.drawString(paramName, LEFT_COL_X + 70, y+2, 2);
  tft.drawRightString(value, 230, y+2, 2);
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
  drawDataLine(0, String(sensor_id), "temperature (C)", String(data.temperature_celsius, 1));
  drawDataLine(1, String(sensor_id), "humidity (%)", String(data.humidity_percent, 1));
  drawDataLine(2, String(sensor_id), "AQI", String(data.air_quality_index));
  drawDataLine(3, String(sensor_id), "wind (km/h)", String(data.wind_speed_kmh, 1));
  drawDataLine(4, String(sensor_id), "wind dir (deg)", String(data.wind_direction_degrees));
  drawDataLine(5, String(sensor_id), "pressure (hPa)", String(data.atmospheric_pressure_hpa, 1));
  drawDataLine(6, String(sensor_id), "UV index", String(data.uv_index));

  // Si hay alerta activa, imprimirla a la derecha (ya lo hace el callback) - si no, limpiar área
  if (lastAlert.length() == 0) {
    tft.fillRect(RIGHT_COL_X, 8, 120, 40, BG);
  }

  // 2) Construir JSON
  String timestamp = isoTimeNow();
  String json = "{";
  json += "\"sensor_id\": \""; json += sensor_id; json += "\",";
  json += "\"sensor_type\": \""; json += sensor_type; json += "\",";
  json += "\"street_id\": \""; json += street_id; json += "\",";
  json += "\"timestamp\": \""; json += timestamp; json += "\",";
  json += "\"location\": {";
    json += "\"latitude\": "; json += String(loc.latitude, 6); json += ",";
    json += "\"longitude\": "; json += String(loc.longitude, 6); json += ",";
    json += "\"altitude_meters\": "; json += String(loc.altitude_meters, 1); json += ",";
    json += "\"district\": \""; json += loc.district; json += "\",";
    json += "\"neighborhood\": \""; json += loc.neighborhood; json += "\"";
  json += "},";
  json += "\"data\": {";
    json += "\"temperature_celsius\": "; json += String(data.temperature_celsius, 2); json += ",";
    json += "\"humidity_percent\": "; json += String(data.humidity_percent, 2); json += ",";
    json += "\"air_quality_index\": "; json += String(data.air_quality_index); json += ",";
    json += "\"wind_speed_kmh\": "; json += String(data.wind_speed_kmh, 2); json += ",";
    json += "\"wind_direction_degrees\": "; json += String(data.wind_direction_degrees); json += ",";
    json += "\"atmospheric_pressure_hpa\": "; json += String(data.atmospheric_pressure_hpa, 2); json += ",";
    json += "\"uv_index\": "; json += String(data.uv_index);
  json += "}";
  json += "}";

  // 3) Publicar por MQTT
  if (mqttClient.connected()) {
    bool ok = mqttClient.publish(topic_pub, json.c_str());
    Serial.print("Publicado JSON (ok="); Serial.print(ok); Serial.print(") -> "); Serial.println(json);
  } else {
    Serial.println("No conectado a MQTT: no se publica");
  }
}
