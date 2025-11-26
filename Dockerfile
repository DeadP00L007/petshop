#
#    Copyright 2010-2023 the original author or authors.
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#       https://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#

# FROM openjdk:17-jdk-slim
# COPY . /usr/src/myapp
# WORKDIR /usr/src/myapp
# RUN ./mvnw clean package
# CMD ./mvnw cargo:run -P tomcat90



# -------- Petshop --------



# build stage
FROM maven:3.9.0-eclipse-temurin-17 as build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# runtime stage (Tomcat)
FROM tomcat:9.0-jdk17
COPY --from=build /workspace/target/*.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]

