<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>@@GROUP_ID@@</groupId>
        <artifactId>application</artifactId>
        <version>@@PROJECT_VERSION@@</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>single</artifactId>
    <packaging>pom</packaging>

    <modules>
        <module>@@PROJECT_ID@@-application</module>
    </modules>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
    </dependencies>
</project>
