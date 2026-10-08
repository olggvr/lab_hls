package com.lab.school.service;

import com.lab.school.dto.message.BroadcastAudience;
import com.lab.school.dto.message.BroadcastRequest;
import com.lab.school.dto.message.DirectMessageRequest;
import com.lab.school.dto.message.InboxItemResponse;
import com.lab.school.dto.message.MessageResponse;
import com.lab.school.entity.Message;
import com.lab.school.entity.MessageRecipient;
import com.lab.school.entity.ParentStudent;
import com.lab.school.entity.Student;
import com.lab.school.entity.User;
import com.lab.school.entity.enums.MessageType;
import com.lab.school.entity.enums.UserRole;
import com.lab.school.exception.BusinessException;
import com.lab.school.exception.NotFoundException;
import com.lab.school.mapper.MessageMapper;
import com.lab.school.repository.MessageRecipientRepository;
import com.lab.school.repository.MessageRepository;
import com.lab.school.repository.ParentStudentRepository;
import com.lab.school.repository.StudentRepository;
import com.lab.school.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageRecipientRepository recipientRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final MessageMapper messageMapper;
    private final EntityFinder finder;

    public MessageService(MessageRepository messageRepository,
                          MessageRecipientRepository recipientRepository,
                          UserRepository userRepository,
                          StudentRepository studentRepository,
                          ParentStudentRepository parentStudentRepository,
                          MessageMapper messageMapper,
                          EntityFinder finder) {
        this.messageRepository = messageRepository;
        this.recipientRepository = recipientRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.parentStudentRepository = parentStudentRepository;
        this.messageMapper = messageMapper;
        this.finder = finder;
    }

    public MessageResponse getById(Long id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Сообщение", id));
        return messageMapper.toResponse(message);
    }

    /**
     * Личное сообщение одному или нескольким пользователям (сообщение + получатели — одной транзакцией).
     */
    @Transactional
    public MessageResponse sendDirect(DirectMessageRequest request) {
        User sender = finder.user(request.senderId());
        List<User> recipients = userRepository.findAllById(request.recipientIds());
        if (recipients.size() != request.recipientIds().size()) {
            Set<Long> found = recipients.stream().map(User::getId).collect(Collectors.toSet());
            Long missing = request.recipientIds().stream().filter(id -> !found.contains(id)).findFirst().orElse(null);
            throw new NotFoundException("Пользователь", missing);
        }
        Message message = new Message(sender, request.subject(), request.body(), MessageType.DIRECT);
        recipients.forEach(message::addRecipient);
        return messageMapper.toResponse(messageRepository.save(message));
    }

    /**
     * ТРАНЗАКЦИЯ №2: массовая рассылка по классу / параллели / всей школе.
     * <p>
     * Вставляется одна запись в messages и N записей в message_recipients. Всё выполняется в одной
     * транзакции: при ошибке на любой из вставок откатывается вся рассылка — не бывает ситуации,
     * когда сообщение получила только часть адресатов.
     */
    @Transactional
    public MessageResponse broadcast(BroadcastRequest request) {
        User sender = finder.user(request.senderId());
        if (sender.getRole() != UserRole.TEACHER && sender.getRole() != UserRole.SUPERVISOR) {
            throw new BusinessException("Массовую рассылку может делать только учитель или администратор");
        }

        List<Student> students = switch (request.scope()) {
            case CLASS -> {
                if (request.classId() == null) {
                    throw new BusinessException("Для рассылки по классу нужно указать classId");
                }
                finder.schoolClass(request.classId());
                yield studentRepository.findBySchoolClassId(request.classId());
            }
            case PARALLEL -> {
                if (request.year() == null) {
                    throw new BusinessException("Для рассылки по параллели нужно указать year");
                }
                yield studentRepository.findBySchoolClass_Year(request.year());
            }
            case SCHOOL -> studentRepository.findAllBy();
        };

        Map<Long, User> recipients = collectRecipients(students, request.audience());
        recipients.remove(sender.getId());
        if (recipients.isEmpty()) {
            throw new BusinessException("Не найдено ни одного получателя для рассылки");
        }

        Message message = new Message(sender, request.subject(), request.body(), MessageType.BROADCAST);
        recipients.values().forEach(message::addRecipient);
        return messageMapper.toResponse(messageRepository.save(message));
    }

    public Page<InboxItemResponse> inbox(Long userId, int page, int size) {
        finder.user(userId);
        return recipientRepository.findByUserIdOrderByIdDesc(userId, PageRequests.unsorted(page, size))
                .map(messageMapper::toInboxItem);
    }

    @Transactional
    public InboxItemResponse markRead(Long messageId, Long userId) {
        MessageRecipient recipient = recipientRepository.findByMessageIdAndUserId(messageId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Сообщение id=" + messageId + " для пользователя id=" + userId + " не найдено"));
        recipient.markRead();
        return messageMapper.toInboxItem(recipient);
    }

    @Transactional
    public void delete(Long id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Сообщение", id));
        messageRepository.delete(message);
    }

    private Map<Long, User> collectRecipients(List<Student> students, BroadcastAudience audience) {
        Map<Long, User> recipients = new LinkedHashMap<>();
        if (audience == BroadcastAudience.STUDENTS || audience == BroadcastAudience.ALL) {
            students.forEach(s -> recipients.putIfAbsent(s.getUser().getId(), s.getUser()));
        }
        if ((audience == BroadcastAudience.PARENTS || audience == BroadcastAudience.ALL) && !students.isEmpty()) {
            List<Long> studentIds = students.stream().map(Student::getId).toList();
            for (ParentStudent link : parentStudentRepository.findByStudentIdIn(studentIds)) {
                User parentUser = link.getParent().getUser();
                recipients.putIfAbsent(parentUser.getId(), parentUser);
            }
        }
        return recipients;
    }
}
