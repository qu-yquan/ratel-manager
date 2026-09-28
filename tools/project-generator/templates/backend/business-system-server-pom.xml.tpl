<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>@@GROUP_ID@@</groupId>
        <artifactId>business-system</artifactId>
        <version>@@PROJECT_VERSION@@</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>business-system-server</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
        </dependency>
        <dependency>
            <groupId>@@GROUP_ID@@</groupId>
            <artifactId>business-system-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.quyq.gwsu</groupId>
            <artifactId>business-security-api</artifactId>
        </dependency>
        <dependency>
            <groupId>org.quyq.gwsu</groupId>
            <artifactId>common-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.quyq.gwsu</groupId>
            <artifactId>common-database</artifactId>
        </dependency>
        <dependency>
            <groupId>org.quyq.gwsu</groupId>
            <artifactId>common-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>org.quyq.gwsu</groupId>
            <artifactId>common-authentication</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>
    </dependencies>
</project>
