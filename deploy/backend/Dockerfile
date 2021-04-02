FROM bellsoft/liberica-openjdk-alpine:11.0.10-9

COPY build/libs/idel-backend.jar /opt

ENTRYPOINT ["java", "-jar", "/opt/idel-backend.jar"]