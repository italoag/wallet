package dev.bloco.wallet.hub.domain;

import lombok.Getter;

import java.util.UUID;

/**
 * Represents a user in the system with a unique identifier, name, and email address.
 * Each User instance is assigned a randomly generated UUID at the time of creation.
 *<p/>
 * This class is immutable, ensuring that once a user is created, its properties
 * cannot be modified directly.
 */
@Getter
public class User {
    private final UUID id;
    private final String name;
    private final String email;

  /**
   * Constructs a new User with the specified name and email address. Each User
   * instance is assigned a unique identifier (UUID) at the time of creation.
   *
   * @param name  the name of the user
   * @param email the email address of the user
   */
  public User(String name, String email) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.email = email;
    }
}
