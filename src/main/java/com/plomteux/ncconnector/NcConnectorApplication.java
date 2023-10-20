package com.plomteux.ncconnector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan({"com.plomteux.ncconnector.entity"})
@EnableJpaRepositories({"com.plomteux.ncconnector.repository"})
@EnableScheduling
public class NcConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(NcConnectorApplication.class, args);
    }

}
