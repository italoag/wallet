package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.domain.model.user.User;
import dev.bloco.wallet.hub.domain.gateway.UserRepository;
import dev.bloco.wallet.hub.infra.provider.data.entity.UserEntity;
import dev.bloco.wallet.hub.infra.provider.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

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
}
