#!/usr/bin/env python3
import argparse
import json
import logging
import random
import time
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Dict, Optional, Tuple

import paho.mqtt.client as mqtt


# -------------------------
# Configuración de estaciones
# -------------------------
@dataclass
class Location:
    lat: float
    long: float
    alt: float
    district: str
    neighborhood: str


@dataclass
class StationConfig:
    sensor_id: str = "LABJAV77-G2"
    sensor_type: str = "weather"
    street_id: str = "ST_0777"
    location: Location = field(default_factory=lambda: Location(
        lat=41.3971536, long=-4.6734246, alt=650.0,
        district="Arganzuela", neighborhood="Imperial"
    ))
    publish_every_s: float = 1.0


@dataclass
class StationState:
    rng: random.Random
    error_mode: bool = False
    last_alert: str = ""
    force_publish: bool = False

    # Estado interno para deriva suave
    temp_c: float = 22.0
    humid_pct: float = 45.0
    aqi: float = 40.0
    lux: float = 250.0
    sound_db: float = 45.0
    pressure_hpa: float = 1013.0
    uv_index: float = 2.0

    # Para “extra” cuando error_mode=True
    static_iaq: float = 40.0


def iso_utc_now_ms() -> str:
    # Ej: 2025-12-20T14:22:33.123Z
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")


def clamp(x: float, lo: float, hi: float) -> float:
    return lo if x < lo else hi if x > hi else x


def drift(value: float, target: float, alpha: float) -> float:
    # alpha pequeño = deriva lenta hacia target
    return value + alpha * (target - value)


def simulate_next(state: StationState, base: Dict[str, float]) -> None:
    """
    Simulación sencilla:
    - deriva hacia un valor base
    - ruido gaussiano
    - clamps por rangos razonables
    """
    # Temperatura
    state.temp_c = drift(state.temp_c, base["temp_c"], alpha=0.02) + state.rng.gauss(0, 0.12)
    state.temp_c = clamp(state.temp_c, -5, 45)

    # Humedad
    state.humid_pct = drift(state.humid_pct, base["humid_pct"], alpha=0.02) + state.rng.gauss(0, 0.4)
    state.humid_pct = clamp(state.humid_pct, 0, 100)

    # AQI (aprox BSEC IAQ 0..500)
    state.aqi = drift(state.aqi, base["aqi"], alpha=0.03) + state.rng.gauss(0, 2.0)
    state.aqi = clamp(state.aqi, 0, 500)

    # Lux
    state.lux = drift(state.lux, base["lux"], alpha=0.02) + state.rng.gauss(0, 8.0)
    state.lux = clamp(state.lux, 0, 120000)

    # Ruido dB
    state.sound_db = drift(state.sound_db, base["sound_db"], alpha=0.02) + state.rng.gauss(0, 0.7)
    state.sound_db = clamp(state.sound_db, 20, 110)

    # Presión
    state.pressure_hpa = drift(state.pressure_hpa, base["pressure_hpa"], alpha=0.01) + state.rng.gauss(0, 0.25)
    state.pressure_hpa = clamp(state.pressure_hpa, 870, 1085)

    # Índice UV
    state.uv_index = drift(state.uv_index, base["uv_index"], alpha=0.04) + state.rng.gauss(0, 0.25)
    state.uv_index = clamp(state.uv_index, 0, 11)

    # Static IAQ como suavizado
    state.static_iaq = 0.9 * state.static_iaq + 0.1 * state.aqi


def build_payload(cfg: StationConfig, st: StationState) -> str:
    root = {
        "sensor_id": cfg.sensor_id,
        "sensor_type": cfg.sensor_type,
        "street_id": cfg.street_id,
        "timestamp": iso_utc_now_ms(),
        "location": {
            "lat": round(cfg.location.lat, 6),
            "long": round(cfg.location.long, 6),
            "alt": round(cfg.location.alt, 1),
            "district": cfg.location.district,
            "neighborhood": cfg.location.neighborhood,
        },
        "data": {
            "temp": round(st.temp_c, 2),
            "humid": round(st.humid_pct, 2),
            "aqi": int(round(st.aqi)),
            "lux": round(st.lux, 2),
            "sound_db": round(st.sound_db, 2),
            "atmhpa": round(st.pressure_hpa, 2),
            "uv_index": int(round(st.uv_index)),
        }
    }

    if st.error_mode:
        # Valores “extra” inspirados en tu JSON (no hace falta que sean perfectos; solo coherentes)
        co2_eq = clamp(400.0 + (st.aqi * 3.0), 400, 5000)
        breath_voc_eq = clamp(0.5 + (st.aqi / 120.0), 0.0, 10.0)

        root["extra"] = {
            "bsec_status": 0,
            "iaq": round(st.aqi, 2),
            "static_iaq": round(st.static_iaq, 2),
            "co2_eq": round(co2_eq, 2),
            "breath_voc_eq": round(breath_voc_eq, 2),
            "raw_temperature": round(st.temp_c + st.rng.gauss(0, 0.25), 2),
            "raw_humidity": round(st.humid_pct + st.rng.gauss(0, 1.0), 2),
            "pressure_hpa": round(st.pressure_hpa, 2),
            "gas_resistance_ohm": round(clamp(5000 + (500 - st.aqi) * 30 + st.rng.gauss(0, 250), 500, 200000), 2),
            "gas_percentage": round(clamp(100.0 - (st.aqi / 5.0) + st.rng.gauss(0, 1.5), 0, 100), 2),
            "stabilization_status": 3,
            "run_in_status": 1,
            "sensor_heat_comp_temp": round(st.temp_c, 2),
            "sensor_heat_comp_hum": round(st.humid_pct, 2),
        }

    return json.dumps(root, ensure_ascii=False, separators=(",", ":"))


def topics_for_station(cfg: StationConfig) -> Tuple[str, str]:
    topic_pub = f"sensors/{cfg.street_id}/{cfg.sensor_id}"
    topic_alerts = f"{topic_pub}/alerts/#"
    return topic_pub, topic_alerts


def parse_alert_payload(payload_bytes: bytes) -> str:
    raw = payload_bytes.decode("utf-8", errors="replace").strip()
    # En tu Arduino intentas parsear JSON y si falla usas string plano
    try:
        doc = json.loads(raw)
        if isinstance(doc, dict):
            if "alerta" in doc:
                return str(doc["alerta"])
            if "message" in doc:
                return str(doc["message"])
    except Exception:
        pass
    return raw


def main():
    parser = argparse.ArgumentParser(description="Simulador de estaciones meteorológicas (MQTT + JSON).")
    parser.add_argument("--host", default="192.168.2.156")
    parser.add_argument("--port", type=int, default=1883)
    parser.add_argument("--user", default="")
    parser.add_argument("--password", default="")
    parser.add_argument("--street-id", default="ST_0777")
    parser.add_argument("--interval", type=float, default=1.5, help="Intervalo de publicación por estación (s).")
    parser.add_argument("--count", type=int, default=5, help="Número de estaciones simuladas.")
    parser.add_argument("--seed", type=int, default=1234)
    parser.add_argument("--log", default="INFO")
    args = parser.parse_args()

    logging.basicConfig(level=getattr(logging, args.log.upper(), logging.INFO),
                        format="%(asctime)s %(levelname)s %(message)s")

    # Crea N estaciones con IDs distintos
    stations_cfg: Dict[str, StationConfig] = {}
    stations_state: Dict[str, StationState] = {}

    for i in range(args.count):
        sensor_id = f"LABJAV09-G{i+1}"
        cfg = StationConfig(
            sensor_id=sensor_id,
            street_id=args.street_id,
            publish_every_s=args.interval,
            # Puedes variar ubicación por estación si quieres:
            location=Location(
                lat=40.3971536 + (i * 0.41255),
                long=-3.6734246 + (i * 0.31000),
                alt=650.0,
                district="Arganzuela",
                neighborhood="Imperial"
            )
        )
        stations_cfg[sensor_id] = cfg
        stations_state[sensor_id] = StationState(rng=random.Random(args.seed + i))

    # Valores base (puedes cambiarlos por estación si quieres)
    base = {
        "temp_c": 22.0,
        "humid_pct": 45.0,
        "aqi": 55.0,
        "lux": 300.0,
        "sound_db": 48.0,
        "pressure_hpa": 1012.0,
        "uv_index": 2.0
    }

    client_id = f"weather-sim-{int(time.time())}"
    client = mqtt.Client(client_id=client_id, clean_session=True)

    if args.user:
        client.username_pw_set(args.user, args.password)

    # Reconexión automática simple (backoff)
    reconnect = {
        "delay": 1.0,
        "max_delay": 60.0
    }

    def on_connect(c, userdata, flags, rc):
        logging.info("MQTT conectado rc=%s client_id=%s", rc, client_id)
        reconnect["delay"] = 1.0
        # Suscribirse a alerts de todas las estaciones
        for cfg in stations_cfg.values():
            _, topic_alerts = topics_for_station(cfg)
            c.subscribe(topic_alerts, qos=0)
            logging.info("Suscrito a %s", topic_alerts)

    def on_disconnect(c, userdata, rc):
        logging.warning("MQTT desconectado rc=%s", rc)
        # Intento de reconexión con backoff
        while True:
            delay = reconnect["delay"]
            logging.warning("Reintentando en %.1fs...", delay)
            time.sleep(delay)
            try:
                c.reconnect()
                return
            except Exception as e:
                logging.error("Fallo al reconectar: %s", e)
                reconnect["delay"] = min(reconnect["delay"] * 2.0, reconnect["max_delay"])

    def on_message(c, userdata, msg):
        topic = msg.topic
        alert_val = parse_alert_payload(msg.payload)

        # Extraer sensor_id del topic: sensors/<street>/<sensor>/alerts/...
        parts = topic.split("/")
        sensor_id = parts[2] if len(parts) >= 3 else None

        if sensor_id not in stations_state:
            logging.info("Alerta para sensor desconocido topic=%s payload=%s", topic, alert_val)
            return

        st = stations_state[sensor_id]
        st.last_alert = alert_val

        if alert_val == "WTH001":
            st.error_mode = True
            st.force_publish = True
            logging.warning("Sensor %s -> ERROR mode (WTH001)", sensor_id)
        elif alert_val == "WTH002":
            st.error_mode = False
            st.force_publish = True
            logging.info("Sensor %s -> NORMAL mode (WTH002)", sensor_id)
        else:
            # Otras alertas: solo registra y fuerza publicación para que se vea “reacción”
            st.force_publish = True
            logging.info("Sensor %s alerta=%s", sensor_id, alert_val)

    client.on_connect = on_connect
    client.on_disconnect = on_disconnect
    client.on_message = on_message

    logging.info("Conectando a MQTT %s:%s ...", args.host, args.port)
    client.connect(args.host, args.port, keepalive=60)

    # Loop en segundo plano para callbacks (suscripción/recepción de alerts)
    client.loop_start()

    # Planificación de publicaciones por estación
    next_pub: Dict[str, float] = {}
    now = time.monotonic()
    for sid, cfg in stations_cfg.items():
        # escalona ligeramente para que no publiquen todos a la vez
        next_pub[sid] = now + (stations_state[sid].rng.random() * 0.3)

    try:
        while True:
            t = time.monotonic()

            for sid, cfg in stations_cfg.items():
                st = stations_state[sid]
                due = (t >= next_pub[sid]) or st.force_publish
                if not due:
                    continue

                st.force_publish = False
                simulate_next(st, base)

                topic_pub, _ = topics_for_station(cfg)
                payload = build_payload(cfg, st)

                ok = client.publish(topic_pub, payload, qos=0, retain=False)
                logging.info("PUB %s ok=%s bytes=%d error_mode=%s",
                             topic_pub, getattr(ok, "rc", None), len(payload), st.error_mode)

                next_pub[sid] = t + cfg.publish_every_s

            time.sleep(0.1)

    except KeyboardInterrupt:
        logging.info("Saliendo...")

    finally:
        try:
            client.loop_stop()
            client.disconnect()
        except Exception:
            pass


if __name__ == "__main__":
    main()
