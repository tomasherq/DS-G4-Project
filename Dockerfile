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


# Make repo dir and clone git repo
#RUN mkdir /root/git_repo
#WORKDIR /root/git_repo
#RUN git clone https://github.com/tomasherq/DS-G4-Project.git

# Copy the source files from Dockerfile's folder to the image if you can't clone git
COPY . ./project
COPY . ./src
COPY . ./pom.xml

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
#WORKDIR /root/git_repo/DS-G4-Project
COPY --from=build ./target ./target
