### Multi-stage Dockerfile: build with Maven then run on Tomcat using JDK 24

# Builder: use Eclipse Temurin JDK 24 and install Maven manually
FROM eclipse-temurin:24-jdk AS builder
WORKDIR /build

# Install tools and Maven from the package repository to avoid external tarball downloads
RUN apt-get update && apt-get install -y maven wget tar gzip ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Copy project files
COPY pom.xml ./
COPY src ./src

# Build WAR (skip tests to speed up)
RUN mvn -B -DskipTests package
# Normalize WAR name so later stage can copy it reliably
RUN mkdir -p target && cp target/*.war target/app.war || true

## Runtime: use official Tomcat image to avoid downloading Tomcat tarball
FROM tomcat:11-jdk17

# Install wget (used to fetch MariaDB JDBC driver), mysql client and certificates
RUN apt-get update && apt-get install -y wget default-mysql-client ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Download MariaDB JDBC driver into Tomcat lib
RUN wget -q -O /usr/local/tomcat/lib/mariadb-java-client.jar \
    https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.2.0/mariadb-java-client-3.2.0.jar

# Copy context and WAR built in the builder stage
COPY tomcat/context.xml /usr/local/tomcat/conf/context.xml
COPY --from=builder /build/target/app.war /usr/local/tomcat/webapps/ROOT.war

# Copy wait-for script and make it executable
COPY tomcat/wait-for.sh /usr/local/bin/wait-for.sh
RUN chmod +x /usr/local/bin/wait-for.sh
# Normalize line endings inside image in case the host file used CRLF
RUN sed -i 's/\r$//' /usr/local/bin/wait-for.sh || true

ENV CATALINA_HOME=/usr/local/tomcat
EXPOSE 8080
# Wait for MariaDB to be available before starting Tomcat to avoid initial connection refused
CMD ["/usr/local/bin/wait-for.sh","mariadb","3306","/usr/local/tomcat/bin/catalina.sh","run"]
