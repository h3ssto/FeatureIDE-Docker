FROM gradle:7-jdk11 AS build
COPY --chown=gradle:gradle ./docker-payload/ /home/gradle/src
WORKDIR /home/gradle/src
RUN mkdir lib

ADD https://github.com/FeatureIDE/FeatureIDE/releases/download/v3.8.2/de.ovgu.featureide.lib.fm-v3.8.2.jar lib/de.ovgu.featureide.lib.fm-v3.8.2.jar
ADD https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/antlr-3.4.jar lib/antlr-3.4.jar
ADD https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/org.sat4j.core.jar lib/org.sat4j.core.jar
ADD https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/uvl-parser.jar lib/uvl-parser.jar

RUN gradle shadowJar --no-daemon

FROM openjdk:8-jre-alpine
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/converter.jar
RUN mkdir /app/files
WORKDIR /app
ENV ARGS=--all
CMD java -jar converter.jar $ARGS