package dev.fassi.conversaoapi;

import org.springframework.boot.SpringApplication;

public class TestConversaoApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(ConversaoApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
