package com.lab.school.service;

import com.lab.school.dto.user.UserCreateRequest;
import com.lab.school.dto.user.UserResponse;
import com.lab.school.dto.user.UserUpdateRequest;
import com.lab.school.entity.Role;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.exception.BusinessException;
import com.lab.school.exception.ConflictException;
import com.lab.school.mapper.UserMapper;
import com.lab.school.repository.RoleRepository;
import com.lab.school.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EntityFinder finder;

    public Page<UserResponse> findAll(int page, int size) {
        return userRepository.findAll(PageRequests.byId(page, size)).map(userMapper::toResponse);
    }

    public UserResponse getById(Long id) {
        return userMapper.toResponse(finder.user(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Пользователь с логином '" + request.username() + "' уже существует");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Пользователь с email '" + request.email() + "' уже существует");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.getRoles().add(findRole(request.role()));
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = finder.user(id);
        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ConflictException("Пользователь с email '" + request.email() + "' уже существует");
        }
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = finder.user(id);
        userRepository.delete(user);
        userRepository.flush();
    }

    @Transactional
    public UserResponse addRole(Long id, UserRole roleName) {
        User user = finder.user(id);
        if (user.hasRole(roleName)) {
            throw new ConflictException("У пользователя уже есть роль " + roleName);
        }
        user.getRoles().add(findRole(roleName));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse removeRole(Long id, UserRole roleName) {
        User user = finder.user(id);
        if (user.getRole() == roleName) {
            throw new BusinessException("Нельзя снять основную роль пользователя (" + roleName + ")");
        }
        boolean removed = user.getRoles().removeIf(r -> r.getName() == roleName);
        if (!removed) {
            throw new BusinessException("У пользователя нет роли " + roleName);
        }
        return userMapper.toResponse(user);
    }

    private Role findRole(UserRole roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Роль " + roleName + " отсутствует в справочнике"));
    }
}
