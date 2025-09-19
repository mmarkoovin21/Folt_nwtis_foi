FROM payara/micro:6.2025.4-jdk21

COPY target/mmarkovin21_vjezba_08_dz_3_servis-1.1.0.war /opt/payara/deployments/app.war

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/opt/payara/payara-micro.jar", "--deploy", "/opt/payara/deployments/app.war", "--contextroot", ""]
