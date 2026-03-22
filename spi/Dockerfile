FROM registry.access.redhat.com/ubi9/openjdk-17:latest AS builder

WORKDIR /build
COPY pom.xml .
COPY src/ src/

USER 0
RUN mvn package -DskipTests -q

FROM registry.access.redhat.com/ubi9-micro:latest

COPY --from=builder /build/target/rhbk-neuroface-spi.jar /opt/spi/rhbk-neuroface-spi.jar

CMD ["echo", "SPI JAR ready at /opt/spi/"]
