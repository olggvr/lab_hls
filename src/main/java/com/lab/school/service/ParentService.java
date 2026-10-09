package com.lab.school.service;

import com.lab.school.dto.parent.ParentCreateRequest;
import com.lab.school.dto.parent.ParentResponse;
import com.lab.school.entity.Parent;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.exception.ConflictException;
import com.lab.school.exception.NotFoundException;
import com.lab.school.mapper.ParentMapper;
import com.lab.school.repository.ParentRepository;
import com.lab.school.utils.PageRequests;
import com.lab.school.utils.ProfileGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Модуль родителей. Единственный владелец parents.
 * Связь родитель—ученик живёт в {@link ParentStudentService}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParentService {

    private final ParentRepository parentRepository;
    private final UserService userService;
    private final ParentMapper parentMapper;

    public Page<ParentResponse> listParents(int page, int size) {
        return parentRepository.findAll(PageRequests.buildSortedById(page, size)).map(parentMapper::toResponse);
    }

    public ParentResponse getParent(Long id) {
        return parentMapper.toResponse(requireParent(id));
    }

    @Transactional
    public ParentResponse createParent(ParentCreateRequest request) {
        User user = userService.requireUser(request.userId());
        ProfileGuard.requireRole(user, UserRole.PARENT);
        if (parentRepository.existsByUserId(user.getId())) {
            throw new ConflictException("Профиль родителя для пользователя id=" + user.getId() + " уже существует");
        }
        Parent parent = new Parent();
        parent.setUser(user);
        return parentMapper.toResponse(parentRepository.save(parent));
    }

    @Transactional
    public void deleteParent(Long id) {
        parentRepository.delete(requireParent(id));
        parentRepository.flush();
    }

    /**
     * Точка входа для других модулей: вернуть родителя или 404.
     */
    public Parent requireParent(Long id) {
        return parentRepository.findById(id).orElseThrow(() -> new NotFoundException("Родитель", id));
    }

    public Optional<Parent> findParentByUserId(Long userId) {
        return parentRepository.findByUserId(userId);
    }
}
