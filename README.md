# estacion-meteu

Libreria para pantalla Adafruit ST7735 and ST7789 Library:1.11.0 y libreria TFT_eSPI
Pines pantalla:
// For ESP32 Dev board (only tested with GC9A01 display)
// The hardware SPI can be mapped to any pins

#define TFT_MOSI 23 // In some display driver board, it might be written as "SDA" and so on.
#define TFT_SCLK 18
#define TFT_CS   5  // Chip select control pin
#define TFT_DC   17  // Data Command control pin
#define TFT_RST  16  // Reset pin (could connect to Arduino RESET pin)
//#define TFT_BL   22  // LED back-light


Libreria para el sensor BME680: [text](https://github.com/boschsensortec/BSEC-Arduino-library)


Actualizacion de la libreria PubSubClient para admitir mensaje largo
Documentos > Arduino > libraries > PubSubClient > src > PubSubClient.h
#define MQTT_MAX_PACKET_SIZE 1024
