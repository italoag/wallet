package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.domain.model.user.User;
import dev.bloco.wallet.hub.infra.provider.data.entity.UserEntity;
import dev.bloco.wallet.hub.infra.provider.data.entity.UserEntity;
import dev.bloco.wallet.hub.infra.provider.mapper.UserMapper;
import dev.bloco.wallet.hub.infra.provider.repository.JpaUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("JPA User Repository Tests")
@ExtendWith(MockitoExtension.class)
class JpaUserRepositoryTest {

    @Mock
    private SpringDataUserRepository springDataUserRepository;

    @Mock
    private UserMapper userMapper;

    private JpaUserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JpaUserRepository(springDataUserRepository, userMapper);
    }

    @Test
    @DisplayName("findByEmail should delegate to SpringDataUserRepository")
    void findById_shouldMapEntityToDomain() {
        // given
        UUID id = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setName("Alice");
        entity.setEmail("alice@example.com");

        User domain = User.create(id, "Alice", "alice@example.com", "password");

        when(springDataUserRepository.findById(id)).thenReturn(Optional.of(entity));
        when(userMapper.toDomain(entity)).thenReturn(domain);

        // when
        Optional<User> result = repository.findById(id);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(domain);
        verify(springDataUserRepository).findById(id);
        verify(userMapper).toDomain(entity);
    }

    @Test
    @DisplayName("save should delegate to SpringDataUserRepository")
    void save_shouldMapDomainToEntity_andBackToDomain() {
        // given
        UUID userId = UUID.randomUUID();
        User toSave = User.create(userId, "Bob", "bob@example.com", "password");
        UserEntity mappedEntity = new UserEntity();
        mappedEntity.setName("Bob");
        mappedEntity.setEmail("bob@example.com");

        UserEntity savedEntity = new UserEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setName("Bob");
        savedEntity.setEmail("bob@example.com");

        User mappedBack = User.create(savedEntity.getId(), "Bob", "bob@example.com", "password");

        when(userMapper.toEntity(toSave)).thenReturn(mappedEntity);
        when(springDataUserRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(userMapper.toDomain(savedEntity)).thenReturn(mappedBack);

        // when
        User result = repository.save(toSave);

        // then
        assertThat(result).isSameAs(mappedBack);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(springDataUserRepository).save(captor.capture());
        UserEntity captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("Bob");
        assertThat(captured.getEmail()).isEqualTo("bob@example.com");

        verify(userMapper).toEntity(toSave);
        verify(userMapper).toDomain(savedEntity);
        verifyNoMoreInteractions(userMapper, springDataUserRepository);
    }
}
