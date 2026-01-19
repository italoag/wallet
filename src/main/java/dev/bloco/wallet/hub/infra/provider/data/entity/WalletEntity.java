package dev.bloco.wallet.hub.infra.provider.data.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a wallet entity in the database. This class maps to the "wallets"
 * table
 * and provides fields and methods to manage wallet information.
 * <p/>
 * Fields:
 * - id: The unique identifier of the wallet, automatically generated as a UUID.
 * - userId: The UUID of the user to whom the wallet belongs. This field is
 * mandatory.
 * - balance: The current balance of the wallet. This field is mandatory.
 * <p/>
 * Annotations from Jakarta Persistence API (@Entity, @Table) are used for
 * database mapping,
 * while Lombok annotations
 * (@Getter, @Setter, @ToString, @RequiredArgsConstructor) provide
 * automatic generation of boilerplate code such as getters, setters, and
 * constructors.
 * <p/>
 * This class overrides the equals and hashCode methods to ensure proper
 * equality checks
 * and hashcode generation, taking into account the possibility of proxy objects
 * using
 * Hibernate.
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "wallets")
public class WalletEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal balance;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass)
            return false;
        WalletEntity that = (WalletEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
