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
const float I_sens = 113e-9;  // Sensibilidad del GUVA-S12SD: 113 nA por mW/cm²


#define RL 1000000.0   // Resistencia de carga en ohmios (100 kΩ) (guva) igual es 1e6 Ohmios (resistencia de carga R3 = 1MΩ)


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
/*
// Prueba A del sensor UV GUVA-S12SD mediante tablas
  // Leer el valor ADC del pin UV
  int uv_raw = analogRead(UV_PIN);

  // Convertir la lectura ADC en voltaje (ESP32: 12 bits, 3.3V)
  float uv_voltage = uv_raw * (3.3 / 4095.0);

  // Calcular la fotocorriente generada (I = V / RL)
  float photocurrent = uv_voltage / RL; // amperios

  // Convertir la corriente en Índice UV usando la pendiente del datasheet:
  // I_photo (A) = 2.67×10⁻⁸ × IUV
  // => IUV = I_photo / (2.67×10⁻⁸)
  float uv_index = photocurrent / (2.67e-8);
*/
// Prueba B del sensor UV GUVA-S12SD mediante fórmula simplificada con datos de fabricante
// Photocurrent ≈ 101..125 nA per 1 mW/cm² -> usamos 113 nA como valor medio.
  // Fotocorriente I_photo = Vout / RL
  float i_photo = uv_voltage / RL;         // amperios

  // mW/cm^2 = I_photo / I_sens
  float mW_per_cm2 = i_photo / I_sens;

  // Convertimos mW/cm^2 a W/m^2 (1 mW/cm^2 = 10 W/m^2) y luego al Índice UV (x40)
  float irradiance_Wm2 = mW_per_cm2 * 10.0;
  float uv_index2 = irradiance_Wm2 * 40.0;


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
/*
  Serial.print("Prueba A - ");
  Serial.print("Lectura ADC: ");
  Serial.print(uv_raw);
  Serial.print(" | Voltaje: ");
  Serial.print(uv_voltage, 3);
  Serial.print(" V | I_photo: ");
  Serial.print(photocurrent, 8);
  Serial.print(" A | UV Index: ");
  Serial.println(uv_index, 2);
*/
  Serial.print("Prueba B - ");
  Serial.print("Vout: "); Serial.print(uv_voltage, 4);
  Serial.print(" V | I_photo: "); Serial.print(i_photo, 10);
  Serial.print(" A | mW/cm2: "); Serial.print(mW_per_cm2, 6);
  Serial.print(" | UV Index: "); Serial.println(uv_index2, 2);


  delay(1000); // 1 lectura por segundo
}
