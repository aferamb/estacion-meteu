#include <Wire.h>
#include <BH1750.h>

// --- Pines analógicos ---
const int UV_PIN = 34;       // GUVA-S12SD (sensor UV)
const int SOUND_PIN = 35;    // Sensor de sonido XY376

// --- Objeto del sensor de luz ---
BH1750 lightMeter;

// --- Variables de lectura ---
float lux = 0.0;
float uv_voltage = 0.0;
float sound_voltage = 0.0;
float sound_dB = 0.0;

// --- Configuración del sensor de sonido ---
const float sensitivity = 0.05;  // V/Pa (sensibilidad XY376)
const float P0 = 0.00002;        // Pa, referencia 0 dB SPL
const int sampleCount = 100;     // Número de muestras para RMS

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n=== Iniciando sensores ===");

  // --- Inicializar I2C y BH1750 ---
  Wire.begin(21, 22); // SDA = 21, SCL = 22
  if (lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE)) {
    Serial.println(" BH1750 iniciado correctamente");
  } else {
    Serial.println(" Error al iniciar BH1750. Revisa conexiones I2C (SDA=21, SCL=22)");
  }

  // --- Configurar pines analógicos ---
  pinMode(UV_PIN, INPUT);
  pinMode(SOUND_PIN, INPUT);

  Serial.println(" GUVA-S12SD (UV) y XY376 (Sonido) listos\n");
}

void loop() {
  // --- Leer sensor BH1750 ---
  lux = lightMeter.readLightLevel();
  if (lux < 0) lux = 0; // Evitar valores negativos en caso de error

  // --- Leer sensor GUVA ---
  int uv_raw = analogRead(UV_PIN);
  uv_voltage = uv_raw * (3.3 / 4095.0);

  // --- Leer sensor de sonido con RMS ---
  float sumSquares = 0.0;
  for (int i = 0; i < sampleCount; i++) {
    int sound_raw = analogRead(SOUND_PIN);
    float v = sound_raw * (3.3 / 4095.0);  // Convertir a voltios
    float v_centered = v - 1.65;           // Centrar señal (suponiendo alimentación 3.3V)
    sumSquares += v_centered * v_centered;
    delayMicroseconds(100); // Pequeño retardo para muestreo ~10 kHz
  }
  float rms_voltage = sqrt(sumSquares / sampleCount);
  float sound_Pa = rms_voltage / sensitivity;
  sound_dB = 20.0 * log10(sound_Pa / P0);

  // --- Mostrar datos ---
  Serial.print("{");
  Serial.print("\"luz_lux\": "); Serial.print(lux, 2); Serial.print(", ");
  Serial.print("\"uv_voltage\": "); Serial.print(uv_voltage, 3); Serial.print(", ");
  Serial.print("\"sound_dB\": "); Serial.print(sound_dB, 1);
  Serial.println("}");

  delay(1000); // 1 lectura por segundo
}
