package com.lab.school.service;

import com.lab.school.dto.user.UserCreateRequest;
import com.lab.school.dto.user.UserResponse;
import com.lab.school.entity.Role;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.exception.ConflictException;
import com.lab.school.mapper.UserMapper;
import com.lab.school.repository.RoleRepository;
import com.lab.school.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EntityFinder finder;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, passwordEncoder,
                Mappers.getMapper(UserMapper.class), finder);
    }

    @Test
    void create_storesOnlyBcryptHash_andAssignsRole() {
        UserCreateRequest request = new UserCreateRequest(
                "teacher1", "secret123", "Учитель", "t@school.local", null, UserRole.TEACHER);
        when(userRepository.existsByUsername("teacher1")).thenReturn(false);
        when(userRepository.existsByEmail("t@school.local")).thenReturn(false);
        when(roleRepository.findByName(UserRole.TEACHER)).thenReturn(Optional.of(new Role(UserRole.TEACHER)));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.create(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isNotEqualTo("secret123").startsWith("$2");
        assertThat(passwordEncoder.matches("secret123", saved.getPasswordHash())).isTrue();
        assertThat(response.roles()).containsExactly(UserRole.TEACHER);
        assertThat(response.username()).isEqualTo("teacher1");
    }

    @Test
    void create_withTakenUsername_throwsConflict() {
        when(userRepository.existsByUsername("dup")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(new UserCreateRequest(
                "dup", "secret123", "X", "x@school.local", null, UserRole.STUDENT)))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }
}
