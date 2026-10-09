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
import com.lab.school.utils.PageRequests;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Модуль сообщений. Единственный владелец messages и message_recipients.
 * Пользователей, учеников и связи с родителями берёт у соответствующих сервисов.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageRecipientRepository recipientRepository;
    private final UserService userService;
    private final StudentService studentService;
    private final ParentStudentService parentStudentService;
    private final MessageMapper messageMapper;

    public MessageResponse getMessage(Long id) {
        return messageMapper.toResponse(requireMessage(id));
    }

    /**
     * Личное сообщение одному или нескольким пользователям (сообщение + получатели — одной транзакцией).
     */
    @Transactional
    public MessageResponse sendDirectMessage(DirectMessageRequest request) {
        User sender = userService.requireUser(request.senderId());
        List<User> recipients = userService.requireUsers(request.recipientIds());
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
    public MessageResponse broadcastMessage(BroadcastRequest request) {
        User sender = userService.requireUser(request.senderId());
        if (sender.getRole() != UserRole.TEACHER && sender.getRole() != UserRole.SUPERVISOR) {
            throw new BusinessException("Массовую рассылку может делать только учитель или администратор");
        }

        List<Student> students = switch (request.scope()) {
            case CLASS -> {
                if (request.classId() == null) {
                    throw new BusinessException("Для рассылки по классу нужно указать classId");
                }
                yield studentService.collectStudentsOfClass(request.classId());
            }
            case PARALLEL -> {
                if (request.year() == null) {
                    throw new BusinessException("Для рассылки по параллели нужно указать year");
                }
                yield studentService.collectStudentsOfParallel(request.year());
            }
            case SCHOOL -> studentService.collectAllStudents();
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

    public Page<InboxItemResponse> listInbox(Long userId, int page, int size) {
        userService.requireUser(userId);
        return recipientRepository.findByUserIdOrderByIdDesc(userId, PageRequests.buildUnsorted(page, size))
                .map(messageMapper::toInboxItem);
    }

    @Transactional
    public InboxItemResponse markMessageRead(Long messageId, Long userId) {
        MessageRecipient recipient = recipientRepository.findByMessageIdAndUserId(messageId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Сообщение id=" + messageId + " для пользователя id=" + userId + " не найдено"));
        recipient.markRead();
        return messageMapper.toInboxItem(recipient);
    }

    @Transactional
    public void deleteMessage(Long id) {
        messageRepository.delete(requireMessage(id));
    }

    /**
     * Уведомление об оценке. Вызывается модулем оценок внутри его транзакции
     * (REQUIRED присоединяется к уже открытой), поэтому оценка и уведомление
     * сохраняются атомарно.
     */
    @Transactional
    public Message notifyAboutGrade(User sender, String subject, String body, Collection<User> recipients) {
        Message message = new Message(sender, subject, body, MessageType.GRADE_NOTIFICATION);
        recipients.forEach(message::addRecipient);
        return messageRepository.save(message);
    }

    private Message requireMessage(Long id) {
        return messageRepository.findById(id).orElseThrow(() -> new NotFoundException("Сообщение", id));
    }

    private Map<Long, User> collectRecipients(List<Student> students, BroadcastAudience audience) {
        Map<Long, User> recipients = new LinkedHashMap<>();
        if (audience == BroadcastAudience.STUDENTS || audience == BroadcastAudience.ALL) {
            students.forEach(s -> recipients.putIfAbsent(s.getUser().getId(), s.getUser()));
        }
        if ((audience == BroadcastAudience.PARENTS || audience == BroadcastAudience.ALL) && !students.isEmpty()) {
            List<Long> studentIds = students.stream().map(Student::getId).toList();
            for (ParentStudent link : parentStudentService.collectLinksOfStudents(studentIds)) {
                User parentUser = link.getParent().getUser();
                recipients.putIfAbsent(parentUser.getId(), parentUser);
            }
        }
        return recipients;
    }
}
