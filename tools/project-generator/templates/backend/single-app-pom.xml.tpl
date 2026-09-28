<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>@@GROUP_ID@@</groupId>
        <artifactId>single</artifactId>
        <version>@@PROJECT_VERSION@@</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>@@PROJECT_ID@@-application</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.quyq.gwsu</groupId>
            <artifactId>business-security-server</artifactId>
        </dependency>
        <dependency>
            <groupId>@@GROUP_ID@@</groupId>
            <artifactId>business-system-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.quyq.gwsu</groupId>
            <artifactId>business-log-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.quyq.gwsu</groupId>
            <artifactId>business-headless-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.quyq.gwsu</groupId>
            <artifactId>business-kit-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.graalvm.buildtools</groupId>
                <artifactId>native-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
