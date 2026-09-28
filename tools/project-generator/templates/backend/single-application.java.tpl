package @@GROUP_ID@@;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @@PROJECT_ID@@ 单体应用启动类。
 */
@SpringBootApplication(scanBasePackages = {"@@GROUP_ID@@", "org.quyq.gwsu"})
@Slf4j
public class @@PROJECT_CLASS_NAME@@Application {

    public static void main(String[] args) {
        SpringApplication.run(@@PROJECT_CLASS_NAME@@Application.class, args);
    }
}
