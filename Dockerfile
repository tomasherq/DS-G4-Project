#Create Ubuntu image
FROM ubuntu:20.04 AS runner

# LABEL about the custom image
LABEL version="0.1"
LABEL description="This is custom Docker Image for a distributed scala project"

# Disable Prompt During Packages Installation
ARG DEBIAN_FRONTEND=noninteractive

# Update Ubuntu Software repository
RUN apt-get update
# Install softwares
RUN apt-get -y install git openjdk-17-jre-headless


# Clone git repo
RUN mkdir /home/git_repo
WORKDIR /home/git_repo
RUN mkdir -m 700 /root/.ssh; \
  touch -m 600 /root/.ssh/known_hosts; \
  ssh-keyscan github.com > /root/.ssh/known_hosts
# Run docker like this: docker build --ssh github=~/.ssh/id_ed25519 -t testbuild .
RUN --mount=type=ssh,id=github git clone git@github.com:tomasherq/DS-G4-Project.git
WORKDIR /home/git_repo/DS-G4-Project

# Copy the source files from Dockerfile's folder to the image
#COPY . ./project
#COPY . ./src
#COPY . ./pom.xml

#
# Build stage
#
FROM maven:3.8.4-openjdk-17-slim AS build
COPY src ./src
COPY pom.xml .
RUN mvn -f ./pom.xml clean compile package

#
# Copy generated jar file to original place
#
FROM runner AS cont_runner
WORKDIR /home/git_repo/DS-G4-Project
COPY --from=build ./target ./target

# Default command --> we don't need this as we have the command in the compose file
#CMD ["java",  "-jar",  "./target/DS-Project-V1.0.0-jar-with-dependencies.jar",  "--type",  "broken",  "--ID",  "1"]