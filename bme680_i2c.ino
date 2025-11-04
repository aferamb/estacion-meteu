#include "bsec.h"
#include <Wire.h>

// Pines I2C para ESP32 (ajusta si es necesario)
const uint8_t I2C_SDA = 21;  // SDA
const uint8_t I2C_SCL = 22;  // SCL

/*
| BME680 Pin    | ESP32 Pin                     | Descripción      |
| ------------- | ----------------------------- | ---------------- |
| **VDD/VDDIO** | 3.3 V                         | Alimentación     |
| **GND**       | GND                           | Tierra común     |
| **SCL**       | GPIO 22                       | Reloj I²C        |
| **SDA**       | GPIO 21                       | Datos I²C        |
| **CSB**       | 3.3 V                         | Selección de I²C |
| **SDO**       | GND (→ 0x76) o 3.3 V (→ 0x77) | Dirección I²C    |
| **Pull-ups**  | 4.7 kΩ a 3.3 V en SDA y SCL   | Necesarios       |
*/

// Crear objeto BSEC
Bsec iaqSensor;
String output;

// Declaraciones
void checkIaqSensorStatus(void);
void errLeds(void);

void setup(void)
{
  Serial.begin(115200);
  delay(1000);
  pinMode(LED_BUILTIN, OUTPUT);

  Serial.println("\n--- Iniciando BME680 con BSEC ---");

  // Inicialización I2C
  Serial.println("Inicializando bus I2C...");
  Wire.begin(I2C_SDA, I2C_SCL);       // SDA, SCL
  Wire.setClock(400000);              // Fast Mode (400 kHz)
  Serial.printf("Wire.begin() en SDA=%d, SCL=%d\n", I2C_SDA, I2C_SCL);

  // Dirección depende de conexión física del pin SDO
  // SDO → GND  → dirección 0x76
  // SDO → 3.3V → dirección 0x77
  iaqSensor.begin(BME68X_I2C_ADDR_LOW, Wire); // Usa LOW (0x76) si SDO está a GND
  // iaqSensor.begin(BME68X_I2C_ADDR_HIGH, Wire); // Usa esta si SDO está a 3.3V

  Serial.println("Sensor inicializado, leyendo versión BSEC...");
  output = "BSEC library version " + String(iaqSensor.version.major) + "." +
           String(iaqSensor.version.minor) + "." +
           String(iaqSensor.version.major_bugfix) + "." +
           String(iaqSensor.version.minor_bugfix);
  Serial.println(output);
  checkIaqSensorStatus();

  // Lista de sensores virtuales
  bsec_virtual_sensor_t sensorList[] = {
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

  // Encabezado CSV
  output = "Timestamp [ms], IAQ, IAQ accuracy, Static IAQ, CO2 eq, VOC eq, raw T [°C], P [hPa], raw RH [%], gas [Ω], Stab, RunIn, comp T [°C], comp RH [%], gas %";
  Serial.println(output);
}

void loop(void)
{
  unsigned long time_trigger = millis();

  if (iaqSensor.run()) {
    digitalWrite(LED_BUILTIN, LOW);

    output = String(time_trigger);
    output += ", " + String(iaqSensor.iaq);
    output += ", " + String(iaqSensor.iaqAccuracy);
    output += ", " + String(iaqSensor.staticIaq);
    output += ", " + String(iaqSensor.co2Equivalent);
    output += ", " + String(iaqSensor.breathVocEquivalent);
    output += ", " + String(iaqSensor.rawTemperature);
    output += ", " + String(iaqSensor.pressure / 100.0);  // Convertir a hPa
    output += ", " + String(iaqSensor.rawHumidity);
    output += ", " + String(iaqSensor.gasResistance);
    output += ", " + String(iaqSensor.stabStatus);
    output += ", " + String(iaqSensor.runInStatus);
    output += ", " + String(iaqSensor.temperature);
    output += ", " + String(iaqSensor.humidity);
    output += ", " + String(iaqSensor.gasPercentage);

    Serial.println(output);
    digitalWrite(LED_BUILTIN, HIGH);
  } else {
    checkIaqSensorStatus();
  }
}

void checkIaqSensorStatus(void)
{
  if (iaqSensor.bsecStatus != BSEC_OK) {
    if (iaqSensor.bsecStatus < BSEC_OK) {
      Serial.printf("BSEC error code: %d\n", iaqSensor.bsecStatus);
      for (;;)
        errLeds();
    } else {
      Serial.printf("BSEC warning code: %d\n", iaqSensor.bsecStatus);
    }
  }

  if (iaqSensor.bme68xStatus != BME68X_OK) {
    if (iaqSensor.bme68xStatus < BME68X_OK) {
      Serial.printf("BME68X error code: %d\n", iaqSensor.bme68xStatus);
      for (;;)
        errLeds();
    } else {
      Serial.printf("BME68X warning code: %d\n", iaqSensor.bme68xStatus);
    }
  }
}

void errLeds(void)
{
  digitalWrite(LED_BUILTIN, HIGH);
  delay(500);
  digitalWrite(LED_BUILTIN, LOW);
  delay(500);
  Serial.printf("[errLeds] bsecStatus=%d, bme68xStatus=%d\n", iaqSensor.bsecStatus, iaqSensor.bme68xStatus);
}
