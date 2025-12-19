Instrucciones para levantar la infraestructura Docker (actualizado)

Requisitos:
- Docker y Docker Compose instalados en el sistema (no es necesario Maven localmente).


Pasos para levantar los servicios (sin Maven en host):
1) Abrir PowerShell en la carpeta raíz del proyecto (donde está `docker-compose.yml`).
2) Ejecutar:

docker compose up --build -d
o
Docker compose -f docker-compose.yml up -d --build


Comprobaciones y ejemplos:
- Acceder a la app webb: `http://localhost:8080/`

Iniciar sesion y comprobar servicios



{"sensor_id": "LABJAV73-G1","sensor_type": "weather","street_id": "ST_0886","timestamp": "2025-12-05T23:16:34.704Z","location": {"lat": 40.397154,"long": -3.673425,"alt": 650.0,"district": "Arganzuela","neighborhood": "Imperial"},"data": {"temp": 55.55,"humid": 55.55,"aqi": 55,"lux": 55.55,"sound_db": 5.39,"atmhpa": 936.36,"uv_index": 0}}

{"sensor_id": "LABJAV69-G1","sensor_type": "weather","street_id": "ST_0886","timestamp": "2025-12-05T23:16:34.704Z","location": {"lat": 40.397154,"long": -3.673425,"alt": 650.0,"district": "Arganzuela","neighborhood": "Imperial"},"data": {"temp": 55.55,"humid": 55.55,"aqi": 55,"lux": 55.55,"sound_db": 5.39,"atmhpa": 936.36,"uv_index": 0}}