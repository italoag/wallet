package dev.bloco.wallet.hub.domain;

import lombok.Getter;

import java.util.UUID;

@Getter
public class User {
    private final UUID id;
    private final String name;
    private final String email;

    public User(String name, String email) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.email = email;
    }
}
