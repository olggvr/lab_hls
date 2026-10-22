package com.lab.school.service;

import com.lab.school.dto.user.UserCreateRequest;
import com.lab.school.dto.user.UserResponse;
import com.lab.school.dto.user.UserUpdateRequest;
import com.lab.school.entity.Role;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.exception.BusinessException;
import com.lab.school.exception.ConflictException;
import com.lab.school.exception.NotFoundException;
import com.lab.school.mapper.UserMapper;
import com.lab.school.repository.RoleRepository;
import com.lab.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Модуль пользователей. Единственный владелец users и roles: остальные модули
 * получают пользователя через {@link #requireUser(Long)}, а не через UserRepository.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    public Page<UserResponse> listUsers(int page, int size) {
        return userRepository.findAll(PageRequests.byId(page, size)).map(userMapper::toResponse);
    }

    public UserResponse getUser(Long id) {
        return userMapper.toResponse(requireUser(id));
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Пользователь с логином '" + request.username() + "' уже существует");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Пользователь с email '" + request.email() + "' уже существует");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.getRoles().add(requireRoleEntity(request.role()));
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = requireUser(id);
        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ConflictException("Пользователь с email '" + request.email() + "' уже существует");
        }
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.delete(requireUser(id));
        userRepository.flush();
    }

    @Transactional
    public UserResponse addRole(Long id, UserRole roleName) {
        User user = requireUser(id);
        if (user.hasRole(roleName)) {
            throw new ConflictException("У пользователя уже есть роль " + roleName);
        }
        user.getRoles().add(requireRoleEntity(roleName));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse removeRole(Long id, UserRole roleName) {
        User user = requireUser(id);
        if (user.getRole() == roleName) {
            throw new BusinessException("Нельзя снять основную роль пользователя (" + roleName + ")");
        }
        boolean removed = user.getRoles().removeIf(r -> r.getName() == roleName);
        if (!removed) {
            throw new BusinessException("У пользователя нет роли " + roleName);
        }
        return userMapper.toResponse(user);
    }

    /**
     * Точка входа для других модулей: вернуть пользователя или 404.
     */
    public User requireUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Пользователь", id));
    }

    /**
     * Пакетная загрузка пользователей (например, получателей сообщения): либо все, либо 404.
     */
    public List<User> requireUsers(Collection<Long> ids) {
        List<User> users = userRepository.findAllById(ids);
        if (users.size() != ids.size()) {
            Set<Long> found = users.stream().map(User::getId).collect(Collectors.toSet());
            Long missing = ids.stream().filter(id -> !found.contains(id)).findFirst().orElse(null);
            throw new NotFoundException("Пользователь", missing);
        }
        return users;
    }

    private Role requireRoleEntity(UserRole roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Роль " + roleName + " отсутствует в справочнике"));
    }
}
