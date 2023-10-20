package com.plomteux.ncconnector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan({"com.plomteux.ncconnector.entity"})
@EnableJpaRepositories({"com.plomteux.ncconnector.repository"})
public class NcConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(NcConnectorApplication.class, args);
    }

}
