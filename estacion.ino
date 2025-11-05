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
// Light & sound sensors
#include <BH1750.h>

// ---------------------- CONFIGURACIÓN ----------------------
const char* ssid = "ASUS";
const char* password = "atar a la rata";

const char* mqtt_server = "test.mosquitto.org"; // Broker MQTT
const int mqtt_port = 1883;
const char* mqtt_user = "";
const char* mqtt_pass = "";

// topic donde publicaremos los JSON (puedes cambiarlo)
const char* topic_pub = "sensors/ws/data";
// topic para recibir avisos (alerts) desde el servidor
const char* topic_alerts = "sensors/alerts/esp32/#";

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
// Screen size (width x height)
const int SCREEN_W = 240;
const int SCREEN_H = 320;
// Alert box (small, top-right corner)
const int ALERT_X = 190;
const int ALERT_Y = 4;
const int ALERT_W = 46;
const int ALERT_H = 14;
// Bottom alert banner (large, red, centered text)
const int ALERT_B_X = 0;
const int ALERT_B_W = 240;
const int ALERT_B_H = 28; // height of banner
const int ALERT_B_Y = SCREEN_H - ALERT_B_H - 2; // 2px margin from bottom

// Alert & mode state
bool alertActive = false;
unsigned long alertStart = 0;
const unsigned long ALERT_DURATION_MS = 10000UL; // 10 seconds
String lastFullAlertMessage = ""; // last received alert message

// Mode: normal or error (WTH001 sets error mode, WTH002 resets)
bool errorMode = false;

// ---------------------- MQTT / WiFi ----------------------
WiFiClient espClient;
PubSubClient mqttClient(espClient);

String lastAlert = "";       // último aviso recibido
unsigned long alertTime = 0;  // cuando llegó
// Non-blocking MQTT reconnect control
unsigned long lastMqttAttempt = 0;
const unsigned long MQTT_RETRY_MS = 5000UL; // reintentar cada 5 s sin bloquear

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

// Cache for BSEC/raw fields so we can display the same "extra" data that was published
struct BsecCache {
  int bsecStatus;
  float iaq;
  float staticIaq;
  float co2Equivalent;
  float breathVocEquivalent;
  float rawTemperature;
  float rawHumidity;
  float pressure_hpa;
  float gasResistance;
  float gasPercentage;
  int stabStatus;
  int runInStatus;
  float sensorCompTemperature;
  float sensorCompHumidity;
};

BsecCache lastBsecCache;

// Cache último dato leído para poder forzar publicación/redibujo
Location lastLocationCache;
WeatherData lastWeatherCache;
bool haveLastData = false;
// Último estado de publicación
bool lastPublishOk = false;
unsigned long lastPublishMillis = 0;
// Control de refresco de pantalla (igual que la tasa de datos)
const unsigned long DISPLAY_REFRESH_MS = 1000UL; // 1s (igual que el throttle de datos)
unsigned long lastDataDrawMs = 0;   // last time we redrew the data area
unsigned long lastFooterDrawMs = 0; // last time we redrew the footer

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

// Light / Sound / UV sensors pins and params
const int UV_PIN = 34;       // GUVA-S12SD (sensor UV) - ADC1_6 on ESP32
const int SOUND_PIN = 35;    // Sensor de sonido XY376 - ADC1_7 on ESP32

// UV sensor parameters (method B)
const float I_sens = 113e-9; // A per (mW/cm^2) (113 nA per mW/cm^2)
const float RL = 1000000.0;  // Load resistor in ohms used with GUVA (1MΩ as in your test)

// Sound sensor parameters
const float SOUND_SENSITIVITY = 0.04; // V/Pa (XY376)
const float P0 = 0.00002; // reference pressure for dB
const int SOUND_SAMPLE_COUNT = 100; // samples for RMS

BH1750 lightMeter;

// ---------------------- SETUP ----------------------
void setup() {
  Serial.begin(115200);
  delay(500);

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
  // Increase PubSubClient buffer to allow larger JSON payloads (no need to modify .h)
  if (mqttClient.setBufferSize(1024)) {
    Serial.println("hola: buffer set to 1024");
  } else {
    Serial.println("hola: setBufferSize(1024) failed or not supported");
  }

  // --- Inicializar BME680 / BSEC via I2C ---
  Serial.println("Inicializando I2C para BME680...");
  Wire.begin(I2C_SDA, I2C_SCL); // SDA, SCL
  Wire.setClock(400000); // fast mode (opcional)
  // Inicializar BH1750 (sensor de luz) en el mismo bus I2C
  if (lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE)) {
    Serial.println("BH1750 iniciado correctamente");
  } else {
    Serial.println("Error al iniciar BH1750. Revisa conexiones I2C (SDA=21, SCL=22)");
  }
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

  // Configurar pines analógicos para UV y sonido
  pinMode(UV_PIN, INPUT);
  pinMode(SOUND_PIN, INPUT);
  // Configure ADC attenuation so analogRead maps ~0..3.3V (ESP32 ADC attenuation)
  // This improves voltage accuracy for sensors expecting full-scale 3.3V.
#ifdef ADC_11db
  analogSetPinAttenuation(SOUND_PIN, ADC_11db);
  analogSetPinAttenuation(UV_PIN, ADC_11db);
#endif

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
      
      // Leer BH1750 (lux)
      float lux = lightMeter.readLightLevel();
      if (lux < 0) lux = 0.0;
      d.luminosity_lux = lux;

      // Leer sensor de sonido: RMS sobre SOUND_SAMPLE_COUNT muestras
      // Use mean-subtracted RMS to avoid assuming the DC bias is exactly Vcc/2.
      // This is more robust to ADC offset and ensures we compute the AC RMS only.
      float sumV = 0.0;
      float sumV2 = 0.0;
      for (int i = 0; i < SOUND_SAMPLE_COUNT; i++) {
        int raw = analogRead(SOUND_PIN);
        float v = raw * (3.3 / 4095.0);
        sumV += v;
        sumV2 += v * v;
        delayMicroseconds(100);
      }
      float meanV = sumV / (float)SOUND_SAMPLE_COUNT;
      float meanV2 = sumV2 / (float)SOUND_SAMPLE_COUNT;
      float variance = meanV2 - (meanV * meanV);
      if (variance < 0.0) variance = 0.0;
      float rms_v = sqrt(variance);
      // Convert RMS voltage to Pascals using sensor sensitivity (V/Pa)
      float sound_Pa = rms_v / SOUND_SENSITIVITY;
      float sound_db = 0.0;
      if (sound_Pa > 0.0) sound_db = 20.0 * log10(sound_Pa / P0);
      d.sound_db = sound_db;
      // Debug: show raw stats
      Serial.print("SOUND: meanV="); Serial.print(meanV, 4);
      Serial.print(" rms_v="); Serial.print(rms_v, 6);
      Serial.print(" Pa="); Serial.print(sound_Pa, 6);
      Serial.print(" dB="); Serial.println(sound_db, 2);

      // Leer sensor UV (GUVA-S12SD) — método B
      int uv_raw = analogRead(UV_PIN);
      float uv_voltage = uv_raw * (3.3 / 4095.0);
      float i_photo = 0.0;
      int uv_index = 0;
      if (RL > 0 && I_sens > 0) {
        i_photo = uv_voltage / RL; // A
        float mW_per_cm2 = i_photo / I_sens; // mW/cm^2
        float irradiance_Wm2 = mW_per_cm2 * 10.0; // W/m^2
        float uvf = irradiance_Wm2 * 40.0; // índice UV aproximado
        if (uvf < 0) uvf = 0;
        uv_index = (int)round(uvf);
      }
      d.uv_index = uv_index;

      d.atmospheric_pressure_hpa = iaqSensor.pressure / 100.0; // convertir Pa -> hPa

      // Debug prints (visible por Serial aunque la pantalla esté muerta)
      Serial.print("SENSORS: lux="); Serial.print(d.luminosity_lux,2);
      Serial.print(" lx, sound_dB="); Serial.print(d.sound_db,1);
      Serial.print(" dB, uv_index="); Serial.println(d.uv_index);

      publishAndShowSensorData(SENSOR_ID, SENSOR_TYPE, STREET_ID, loc, d);
    }
  } else {
    // Si no hay nuevos datos, revisar estado ocasionalmente
    checkIaqSensorStatus();
  }

  // Handle full-screen alert expiration and footer rendering
  if (alertActive && (millis() - alertStart >= ALERT_DURATION_MS)) {
    // Alert time expired: leave a single-line footer with the last message
    alertActive = false;
    // restore header and data area
    drawHeader();
    tft.setTextColor(TFT_WHITE, BG);
    tft.setTextSize(1);
    // After the overlay expires, immediately redraw data area from the cached
    // last sensor values so the user sees the most recent readings plus any
    // extra fields while in errorMode (per user request).
  if (haveLastData) {
      // draw cached data lines
      clearDataArea();
      drawDataLine(0, String("temperature (C)"), String(lastWeatherCache.temperature_celsius, 1));
      drawDataLine(1, String("humidity (%)"), String(lastWeatherCache.humidity_percent, 1));
      drawDataLine(2, String("AQI"), String(lastWeatherCache.air_quality_index));
      drawDataLine(3, String("luminosity (lux)"), String(lastWeatherCache.luminosity_lux, 1));
      drawDataLine(4, String("sound (dB)"), String(lastWeatherCache.sound_db, 1));
      drawDataLine(5, String("pressure (hPa)"), String(lastWeatherCache.atmospheric_pressure_hpa, 1));
      drawDataLine(6, String("UV index"), String(lastWeatherCache.uv_index));
      if (errorMode) drawErrorDataLines();
      lastDataDrawMs = millis();
    }
    // Draw footer containing last alert message and last publish status
    drawFooter();
  }

  // If not in alertActive state and there is no recent footer (first-run), ensure footer shows something
  if (!alertActive && lastPublishMillis != 0) {
    // keep footer updated with latest publish status
    // (this will redraw footer each loop; it's cheap and keeps UI in sync)
    if (millis() - lastFooterDrawMs >= DISPLAY_REFRESH_MS) {
      drawFooter();
      lastFooterDrawMs = millis();
    }
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
    // store alert and display a full-screen red banner with centered text
    lastAlert = msg;
    lastFullAlertMessage = msg;
    alertTime = millis();
    alertActive = true;
    alertStart = millis();

    // Special command messages
    if (msg == "WTH001") {
      // Enter error mode
      errorMode = true;
      Serial.println("WTH001 received: entering ERROR mode");
    } else if (msg == "WTH002") {
      // Exit error mode
      errorMode = false;
      Serial.println("WTH002 received: returning to NORMAL mode");
    }

    // prepare short message that fits on screen
    String displayMsg = msg;
    if (displayMsg.length() > 48) displayMsg = displayMsg.substring(0, 45) + "...";

    // draw full-screen red background and centered white text (wrap into up to 2 lines)
    tft.fillScreen(TFT_RED);
    tft.setTextColor(TFT_WHITE, TFT_RED);
    // prefer 2 lines with smaller text so messages don't overflow
    tft.setTextSize(2);
    // Split message into up to two lines near the middle (try to split at space)
    String line1 = displayMsg;
    String line2 = "";
    if (displayMsg.length() > 22) {
      int mid = displayMsg.length() / 2;
      int splitPos = displayMsg.lastIndexOf(' ', mid);
      if (splitPos <= 2) splitPos = mid; // fallback to middle
      line1 = displayMsg.substring(0, splitPos);
      // Trim leading space on second line
      line2 = displayMsg.substring(splitPos);
      if (line2.length() && line2.charAt(0) == ' ') line2 = line2.substring(1);
    }
    if (line2.length() == 0) {
      // single-line centered
      tft.drawCentreString(line1, SCREEN_W / 2, SCREEN_H / 2 - 8, 2);
    } else {
      tft.drawCentreString(line1, SCREEN_W / 2, SCREEN_H / 2 - 12, 2);
      tft.drawCentreString(line2, SCREEN_W / 2, SCREEN_H / 2 + 8, 2);
    }
    // If in errorMode, show a few important BSEC values below the message (smaller)
    // Per request: do NOT show the detailed "extra" BSEC fields on the emergency overlay.
    // The extra/raw BSEC fields will be shown in the regular data area (along with
    // the normal parameters) after the overlay finishes and while errorMode is active.
    // ensure header area is not accidentally left in red

    // Force publish of last known data immediately (do not wait):
    if (haveLastData) {
      // publish last cached data; publishAndShowSensorData will skip redraw due to alertActive
      publishAndShowSensorData(SENSOR_ID, SENSOR_TYPE, STREET_ID, lastLocationCache, lastWeatherCache);
    }
  }
}




void mqttReconnect() {
  // Non-blocking reconnect: only attempt once per MQTT_RETRY_MS
  if (mqttClient.connected()) return;
  unsigned long now = millis();
  if (now - lastMqttAttempt < MQTT_RETRY_MS) return;
  lastMqttAttempt = now;
  Serial.print("Conectando MQTT...");
  String clientId = String("ESP32-") + String((uint32_t)ESP.getEfuseMac(), HEX);
  if (mqttClient.connect(clientId.c_str(), mqtt_user, mqtt_pass)) {
    Serial.println(" conectado bajo ID: " + clientId);
    mqttClient.subscribe(topic_alerts);
    // también suscribimos a cualquier otro topic que necesitemos
  } else {
    Serial.print(" fallo, rc="); Serial.println(mqttClient.state());
    // no delay here (non-blocking) so loop continues to read sensors and update display
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




// Dibuja el footer con el último mensaje de alerta y el estado de la última publicación
void drawFooter() {
  // clear footer area
  tft.fillRect(ALERT_B_X, ALERT_B_Y, ALERT_B_W, ALERT_B_H, BG);
  tft.setTextSize(1);
  tft.setTextColor(TFT_WHITE, BG);
  // left: last alert message (trimmed)
  String left = lastFullAlertMessage;
  if (left.length() == 0) left = lastAlert;
  if (left.length() > 36) left = left.substring(0, 33) + "...";
  tft.drawString(left, LEFT_COL_X, ALERT_B_Y + 6, 2);

  // right: publish status
  String pub;
  if (lastPublishMillis == 0) pub = "Published: -";
  else pub = (lastPublishOk ? "Published: OK" : "Published: FAIL");
  tft.drawRightString(pub, ALERT_B_X + ALERT_B_W - 6, ALERT_B_Y + 6, 2);
  //Serial.print("drawFooter: lastPublishOk="); Serial.print(lastPublishOk);
  //Serial.print(" lastPublishMillis="); Serial.println(lastPublishMillis);
}



// Draw extra BSEC fields in the data area (when in errorMode and not overlay)
void drawErrorDataLines() {
  // start after the existing 7 data rows
  int baseRow = 7;
  // prepare strings
  // Prefer cached BSEC values (from last publish) when available so UI matches
  // what was sent in MQTT. Fall back to iaqSensor if cache not present.
  if (haveLastData) {
    drawDataLine(baseRow + 0, "bsec status", String(lastBsecCache.bsecStatus));
    drawDataLine(baseRow + 1, "breath_voc_eq", String(lastBsecCache.breathVocEquivalent, 2));
    drawDataLine(baseRow + 2, "gas (Ω)", String(lastBsecCache.gasResistance, 2));
    drawDataLine(baseRow + 3, "stabilization", String(lastBsecCache.stabStatus));
    drawDataLine(baseRow + 4, "run_in", String(lastBsecCache.runInStatus));
  } else {
    drawDataLine(baseRow + 0, "bsec status", String(iaqSensor.bsecStatus));
    drawDataLine(baseRow + 1, "breath_voc_eq", String(iaqSensor.breathVocEquivalent, 2));
    drawDataLine(baseRow + 2, "gas (Ω)", String(iaqSensor.gasResistance, 2));
    drawDataLine(baseRow + 3, "stabilization", String(iaqSensor.stabStatus));
    drawDataLine(baseRow + 4, "run_in", String(iaqSensor.runInStatus));
  }
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
  // 1) Mostrar en pantalla (si no hay un alert full-screen activo)
  if (!alertActive) {
    // Only update display at the display refresh rate
  unsigned long now = millis();
  bool doDraw = (now - lastDataDrawMs >= DISPLAY_REFRESH_MS);
  Serial.print("publishAndShowSensorData: alertActive="); Serial.print(alertActive);
  Serial.print(" lastDataDrawMs="); Serial.print(lastDataDrawMs);
  Serial.print(" now="); Serial.print(now);
  Serial.print(" doDraw="); Serial.println(doDraw);
  if (doDraw) {
      clearDataArea();
      drawDataLine(0, String("temperature (C)"), String(data.temperature_celsius, 1));
      drawDataLine(1, String("humidity (%)"), String(data.humidity_percent, 1));
      drawDataLine(2, String("AQI"), String(data.air_quality_index));
      drawDataLine(3, String("luminosity (lux)"), String(data.luminosity_lux, 1));
      drawDataLine(4, String("sound (dB)"), String(data.sound_db, 1));
      drawDataLine(5, String("pressure (hPa)"), String(data.atmospheric_pressure_hpa, 1));
      drawDataLine(6, String("UV index"), String(data.uv_index));
      // If in errorMode, show extra BSEC lines in data area
      if (errorMode) drawErrorDataLines();
      lastDataDrawMs = millis();
      Serial.println("publishAndShowSensorData: drawn data to screen");
    }
  }

  // Alerts are handled separately by mqttCallback and cleared in loop();

  // 2) Construir JSON
  // 2) Construir JSON normal (siempre)
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

  // Si estamos en errorMode, añadimos un bloque "extra" con todos los datos crudos/estatus
  if (errorMode) {
    json += ",\"extra\":{";
    // Include BSEC status code (first key must not have a leading comma)
    json += "\"bsec_status\": "; json += String(iaqSensor.bsecStatus);
    // BSEC high-level outputs
    json += "\"iaq\": "; json += String(iaqSensor.iaq, 2); json += ",";
    json += "\"static_iaq\": "; json += String(iaqSensor.staticIaq, 2); json += ",";
    json += "\"co2_eq\": "; json += String(iaqSensor.co2Equivalent, 2); json += ",";
    json += "\"breath_voc_eq\": "; json += String(iaqSensor.breathVocEquivalent, 2); json += ",";

    // Raw / sensor-level readings
    json += "\"raw_temperature\": "; json += String(iaqSensor.rawTemperature, 2); json += ",";
    json += "\"raw_humidity\": "; json += String(iaqSensor.rawHumidity, 2); json += ",";
    // pressure provided by BSEC in Pa; convert to hPa for readability
    json += "\"pressure_hpa\": "; json += String(iaqSensor.pressure / 100.0, 2); json += ",";
    json += "\"gas_resistance_ohm\": "; json += String(iaqSensor.gasResistance, 2); json += ",";
    json += "\"gas_percentage\": "; json += String(iaqSensor.gasPercentage, 2); json += ",";
    // Status / housekeeping
    json += "\"stabilization_status\": "; json += String(iaqSensor.stabStatus); json += ",";
    json += "\"run_in_status\": "; json += String(iaqSensor.runInStatus); json += ",";
    json += "\"sensor_heat_comp_temp\": "; json += String(iaqSensor.temperature, 2); json += ",";
    json += "\"sensor_heat_comp_hum\": "; json += String(iaqSensor.humidity, 2);
    json += "}";
  }
  json += "}"; // cerrar objeto raíz

  // 3) Publicar por MQTT
  if (mqttClient.connected()) {
    // Before publishing, snapshot BSEC/raw fields into cache so the UI can
    // display the exact same "extra" values that we publish.
    lastBsecCache.bsecStatus = iaqSensor.bsecStatus;
    lastBsecCache.iaq = iaqSensor.iaq;
    lastBsecCache.staticIaq = iaqSensor.staticIaq;
    lastBsecCache.co2Equivalent = iaqSensor.co2Equivalent;
    lastBsecCache.breathVocEquivalent = iaqSensor.breathVocEquivalent;
    lastBsecCache.rawTemperature = iaqSensor.rawTemperature;
    lastBsecCache.rawHumidity = iaqSensor.rawHumidity;
    lastBsecCache.pressure_hpa = iaqSensor.pressure / 100.0;
    lastBsecCache.gasResistance = iaqSensor.gasResistance;
    lastBsecCache.gasPercentage = iaqSensor.gasPercentage;
    lastBsecCache.stabStatus = iaqSensor.stabStatus;
    lastBsecCache.runInStatus = iaqSensor.runInStatus;
    lastBsecCache.sensorCompTemperature = iaqSensor.temperature;
    lastBsecCache.sensorCompHumidity = iaqSensor.humidity;

    bool ok = mqttClient.publish(topic_pub, json.c_str());
    int json_length = json.length();
    Serial.print("Longitud JSON: "); Serial.print(json_length); Serial.println(" bytes");
    Serial.print("Publicado JSON (ok="); Serial.print(ok); Serial.print(") -> "); Serial.println(json);
    // actualizar estado de publicación
    lastPublishOk = ok;
    lastPublishMillis = millis();
  } else {
    Serial.println("No conectado a MQTT: no se publica");
    lastPublishOk = false;
    lastPublishMillis = millis();
  }

  // Guardar en caché el último dato publicado para poder forzar re-publicación cuando llegue una alerta
  lastLocationCache = loc;
  lastWeatherCache = data;
  haveLastData = true;

  // Si no hay overlay activo, actualizar footer inmediatamente para mostrar estado de publicación
  if (!alertActive) drawFooter();
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
