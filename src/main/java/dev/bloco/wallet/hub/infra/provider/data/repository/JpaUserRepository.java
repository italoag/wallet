package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.domain.model.user.User;
import dev.bloco.wallet.hub.domain.model.user.UserStatus;
import dev.bloco.wallet.hub.domain.gateway.UserRepository;
import dev.bloco.wallet.hub.infra.provider.data.entity.UserEntity;
import dev.bloco.wallet.hub.infra.provider.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link UserRepository} interface using JPA for persistence
 * and retrieval of user data. This class acts as an adapter between the domain layer
 * and the persistence layer, leveraging both the {@link SpringDataUserRepository}
 * and the {@link UserMapper} to perform its operations.
 *<p/>
 * This repository is responsible for the following:
 * - Persisting user data into the database.
 * - Retrieving user data by its unique identifier (UUID).
 * - Mapping between domain objects ({@link User}) and persistence entities
 *   ({@link UserEntity}).
 *<p/>
 * Internally, this class uses the {@link SpringDataUserRepository}, which is a Spring
 * Data JPA repository, to perform the database operations. It also relies on the
 * {@link UserMapper} to handle the transformation between {@link User} and
 * {@link UserEntity}.
 *<p/>
 * This class is annotated with {@link Repository}, indicating that it is a Spring
 * managed component responsible for persistence-related functionality.
 */
@Repository
public class JpaUserRepository implements UserRepository {
    private final SpringDataUserRepository springDataUserRepository;
    private final UserMapper userMapper;

    @Autowired
    public JpaUserRepository(SpringDataUserRepository springDataUserRepository, UserMapper userMapper) {
        this.springDataUserRepository = springDataUserRepository;
        this.userMapper = userMapper;
    }

  /**
   * Retrieves a user by their unique identifier (UUID) from the database.
   * The method returns an Optional containing the user if found, or an empty Optional
   * if no user exists with the given identifier.
   *
   * @param id the unique identifier (UUID) of the user to be retrieved
   * @return an Optional containing the user represented as a domain object
   *         if found, or an empty Optional if the user does not exist
   */
  @Override
    public Optional<User> findById(UUID id) {
        return springDataUserRepository.findById(id)
                .map(userMapper::toDomain);
    }

  /**
   * Saves a user entity to the database. This method converts a domain object
   * {@link User} into a persistence entity {@link UserEntity}, persists it, and then
   * converts the saved entity back into a domain object before returning.
   *
   * @param user the {@link User} object to be saved to the database
   * @return the saved {@link User} object after successful persistence
   */
  @Override
    public User save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        return userMapper.toDomain(springDataUserRepository.save(entity));
    }

    @Override
    public void update(User user) {
        UserEntity entity = userMapper.toEntity(user);
        springDataUserRepository.save(entity);
    }

    @Override
    public void delete(UUID id) {
        springDataUserRepository.deleteById(id);
    }

    @Override
    public List<User> findAll() {
        return springDataUserRepository.findAll().stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<User> findByEmail(String email) {
        // Fallback implementation - filter all users by email
        return findAll().stream()
                .filter(user -> email != null && email.equals(user.getEmail()))
                .findFirst();
    }

    @Override
    public List<User> findByStatus(UserStatus status) {
        // Fallback implementation - filter all users by status
        return findAll().stream()
                .filter(user -> status != null && status.equals(user.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataUserRepository.existsById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public List<User> findActiveUsers() {
        return findByStatus(UserStatus.ACTIVE);
    }

    @Override
    public Optional<User> findByEmailVerificationToken(String token) {
        // Fallback implementation - filter all users by verification token
        return findAll().stream()
                .filter(user -> token != null && token.equals(user.getEmailVerificationToken()))
                .findFirst();
    }
}
