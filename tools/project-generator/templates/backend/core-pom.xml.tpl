<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>@@GROUP_ID@@</groupId>
        <artifactId>@@PROJECT_NAME@@</artifactId>
        <version>@@PROJECT_VERSION@@</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>@@PROJECT_ID@@-core</artifactId>
    <name>@@PROJECT_ID@@ 项目公共模块</name>

    <dependencies>
        <dependency>
            <groupId>org.quyq.gwsu</groupId>
            <artifactId>common-core</artifactId>
        </dependency>
    </dependencies>
</project>
