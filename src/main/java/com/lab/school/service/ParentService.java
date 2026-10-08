package com.lab.school.service;

import com.lab.school.dto.parent.ChildResponse;
import com.lab.school.dto.parent.ParentCreateRequest;
import com.lab.school.dto.parent.ParentResponse;
import com.lab.school.entity.Parent;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.exception.ConflictException;
import com.lab.school.mapper.ParentMapper;
import com.lab.school.repository.ParentRepository;
import com.lab.school.repository.ParentStudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository parentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final ParentMapper parentMapper;
    private final EntityFinder finder;

    public Page<ParentResponse> findAll(int page, int size) {
        return parentRepository.findAll(PageRequests.byId(page, size)).map(parentMapper::toResponse);
    }

    public ParentResponse getById(Long id) {
        return parentMapper.toResponse(finder.parent(id));
    }

    @Transactional
    public ParentResponse create(ParentCreateRequest request) {
        User user = finder.user(request.userId());
        ProfileGuard.requireRole(user, UserRole.PARENT);
        if (parentRepository.existsByUserId(user.getId())) {
            throw new ConflictException("Профиль родителя для пользователя id=" + user.getId() + " уже существует");
        }
        Parent parent = new Parent();
        parent.setUser(user);
        return parentMapper.toResponse(parentRepository.save(parent));
    }

    @Transactional
    public void delete(Long id) {
        parentRepository.delete(finder.parent(id));
        parentRepository.flush();
    }

    public List<ChildResponse> getChildren(Long parentId) {
        finder.parent(parentId);
        return parentStudentRepository.findByParentId(parentId).stream()
                .map(parentMapper::toChild)
                .toList();
    }
}
