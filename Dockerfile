FROM openjdk:8

# set a directory for the app
WORKDIR /usr/bin/app

# copy all the files to the container
COPY . .

RUN tar -xvzf spark-3.0.1-bin-hadoop3.2.tgz && mv spark-3.0.1-bin-hadoop3.2 /opt/spark && rm spark-3.0.1-bin-hadoop3.2.tgz


#/opt/spark/bin/spark-submit --class cloud.gks29.CloudMLTester --driver-java-options "-Dlog4j.configuration=file:/usr/bin/app/conf/log4j.properties" /usr/bin/app/CloudMLTester.jar 0 /usr/bin/app/models/lrModel $@

ENTRYPOINT ["/opt/spark/bin/spark-submit", "--class", "cloud.gks29.CloudMLTester", "--driver-java-options", "\"-Dlog4j.configuration=file:/usr/bin/app/conf/log4j.properties\"", "/usr/bin/app/CloudMLTester.jar", "0", "/usr/bin/app/models/"]

