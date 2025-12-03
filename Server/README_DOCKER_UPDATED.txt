Instrucciones para levantar la infraestructura Docker (actualizado)

Requisitos:
- Docker y Docker Compose instalados en el sistema (no es necesario Maven localmente).

Resumen de cambios:
- El `Dockerfile` de Tomcat ahora es multi-stage y compila el WAR dentro de un contenedor Maven.
- Se añadió el endpoint `GET /GetDataByDate?date=YYYY-MM-DD` para consultar mediciones de una fecha concreta.

Pasos para levantar los servicios (sin Maven en host):
1) Abrir PowerShell en la carpeta raíz del proyecto (donde está `docker-compose.yml`).
2) Ejecutar:

```powershell
docker compose up --build -d
```

3) Verificar contenedores:

```powershell
docker compose ps
```

Comprobaciones y ejemplos:
- Acceder a la app: `http://localhost:8080/`
- Obtener todas las mediciones: `http://localhost:8080/GetData`
- Obtener mediciones para `2025-09-18`:
  `http://localhost:8080/GetDataByDate?date=2025-09-18`

Logs de los servicios:
```powershell
docker compose logs -f tomcat-app
docker compose logs -f mosquitto
```

Notas:
- MariaDB se inicializa automáticamente con `db/init/init.sql` la primera vez que arranca.
- MariaDB no está expuesto al host por seguridad; sólo Tomcat (8080) y MQTT (1883) están publicados.

Si quieres que también añada pruebas unitarias que ejecuten consultas contra una base de datos embebida (H2) o que ejecute tests dentro del contenedor Maven, puedo añadir un módulo `src/test` y adaptar el `pom.xml`.
