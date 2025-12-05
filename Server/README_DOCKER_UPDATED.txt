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