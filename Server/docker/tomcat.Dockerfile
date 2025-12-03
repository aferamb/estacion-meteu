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

# Install wget (used to fetch MariaDB JDBC driver) and certificates
RUN apt-get update && apt-get install -y wget ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Download MariaDB JDBC driver into Tomcat lib
RUN wget -q -O /usr/local/tomcat/lib/mariadb-java-client.jar \
    https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.2.0/mariadb-java-client-3.2.0.jar

# Copy context and WAR built in the builder stage
COPY tomcat/context.xml /usr/local/tomcat/conf/context.xml
COPY --from=builder /build/target/app.war /usr/local/tomcat/webapps/ROOT.war

ENV CATALINA_HOME=/usr/local/tomcat
EXPOSE 8080
CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
