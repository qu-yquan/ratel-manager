<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>@@GROUP_ID@@</groupId>
        <artifactId>business</artifactId>
        <version>@@PROJECT_VERSION@@</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>application</artifactId>
    <packaging>pom</packaging>

    <modules>
        <module>distributed</module>
        <module>single</module>
    </modules>

    <dependencies>
        <dependency>
            <groupId>org.quyq.gwsu</groupId>
            <artifactId>common-deploy</artifactId>
        </dependency>
        <dependency>
            <groupId>org.quyq.gwsu</groupId>
            <artifactId>common-log</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-reactor</artifactId>
        </dependency>
    </dependencies>

    <build>
        <finalName>${project.artifactId}</finalName>
    </build>
</project>
