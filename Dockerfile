FROM amazoncorretto:21-alpine3.17-jdk
COPY ./target/*.jar /usr/local/lib/app.jar
COPY ./dockerinit.sh /usr/dockerinit.sh
RUN chmod +x /usr/dockerinit.sh
ENTRYPOINT ["/bin/sh","/usr/dockerinit.sh"]