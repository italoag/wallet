package dev.bloco.wallet.hub.infra.provider.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Represents a User entity in the database. This class maps to the "users"
 * table
 * and is used to store and retrieve user-related data.
 * <p/>
 * The UserEntity class contains the following fields:
 * - id: The unique identifier for the user, automatically generated as a UUID.
 * - name: The name of the user. This field is mandatory and cannot be null.
 * - email: The email of the user. This field is mandatory and cannot be null.
 * <p/>
 * Annotations such as @Entity and @Table are used for JPA persistence,
 * and @Id and @GeneratedValue indicate that the id field is the primary key
 * and will be automatically generated.
 * <p/>
 * Additionally, the @Setter and @Getter annotations from Lombok
 * are used to automatically generate getter and setter methods for the fields.
 */
@Setter
@Getter
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
