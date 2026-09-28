<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.quyq.gwsu</groupId>
        <artifactId>root-pom</artifactId>
        <version>@@PLATFORM_VERSION@@</version>
        <relativePath/>
    </parent>

    <groupId>@@GROUP_ID@@</groupId>
    <artifactId>@@PROJECT_NAME@@</artifactId>
    <packaging>pom</packaging>

    <name>@@PROJECT_NAME@@</name>

    <properties>
        <gwsu.project.version>@@PLATFORM_VERSION@@</gwsu.project.version>
        <project.modules.version>@@PROJECT_VERSION@@</project.modules.version>
    </properties>

    <modules>
        <module>@@PROJECT_ID@@-core</module>
        <module>business</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>@@GROUP_ID@@</groupId>
                <artifactId>@@PROJECT_ID@@-core</artifactId>
                <version>${project.modules.version}</version>
            </dependency>
            <dependency>
                <groupId>@@GROUP_ID@@</groupId>
                <artifactId>business-system-api</artifactId>
                <version>${project.modules.version}</version>
            </dependency>
            <dependency>
                <groupId>@@GROUP_ID@@</groupId>
                <artifactId>business-system-server</artifactId>
                <version>${project.modules.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
