--liquibase formatted sql

-- Демо-данные. Применяются только с контекстом "demo" (LIQUIBASE_CONTEXTS=demo).
-- Аутентификации в ЛР1 нет, поэтому паролей у пользователей тоже нет.

--changeset school:003-demo-data context:demo
INSERT INTO subjects (name)
VALUES ('Математика'),
       ('Русский язык'),
       ('Физика');

INSERT INTO classes (name, year)
VALUES ('7А', 7),
       ('7Б', 7),
       ('8А', 8);

INSERT INTO users (username, full_name, email, phone, role)
VALUES ('admin', 'Администратор Школы', 'admin@school.local', '+70000000000', 'SUPERVISOR'),
       ('ivanova', 'Иванова Мария Петровна', 'ivanova@school.local', '+70000000001', 'TEACHER'),
       ('petrov', 'Петров Сергей Иванович', 'petrov@school.local', '+70000000002', 'TEACHER'),
       ('smirnov.a', 'Смирнов Алексей', 'smirnov.a@school.local', NULL, 'STUDENT'),
       ('kuznetsova.o', 'Кузнецова Ольга', 'kuznetsova.o@school.local', NULL, 'STUDENT'),
       ('volkov.d', 'Волков Дмитрий', 'volkov.d@school.local', NULL, 'STUDENT'),
       ('smirnova.e', 'Смирнова Елена', 'smirnova.e@mail.local', '+70000000010', 'PARENT'),
       ('kuznetsov.p', 'Кузнецов Павел', 'kuznetsov.p@mail.local', '+70000000011', 'PARENT');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN roles r ON r.name = u.role;

INSERT INTO teachers (user_id, subject_id)
VALUES ((SELECT id FROM users WHERE username = 'ivanova'), (SELECT id FROM subjects WHERE name = 'Математика')),
       ((SELECT id FROM users WHERE username = 'petrov'), (SELECT id FROM subjects WHERE name = 'Русский язык'));

INSERT INTO students (user_id, class_id)
VALUES ((SELECT id FROM users WHERE username = 'smirnov.a'), (SELECT id FROM classes WHERE name = '7А')),
       ((SELECT id FROM users WHERE username = 'kuznetsova.o'), (SELECT id FROM classes WHERE name = '7А')),
       ((SELECT id FROM users WHERE username = 'volkov.d'), (SELECT id FROM classes WHERE name = '7Б'));

INSERT INTO parents (user_id)
VALUES ((SELECT id FROM users WHERE username = 'smirnova.e')),
       ((SELECT id FROM users WHERE username = 'kuznetsov.p'));

INSERT INTO parent_student (parent_id, student_id, relation)
VALUES ((SELECT p.id FROM parents p JOIN users u ON u.id = p.user_id WHERE u.username = 'smirnova.e'),
        (SELECT s.id FROM students s JOIN users u ON u.id = s.user_id WHERE u.username = 'smirnov.a'),
        'MOTHER'),
       ((SELECT p.id FROM parents p JOIN users u ON u.id = p.user_id WHERE u.username = 'kuznetsov.p'),
        (SELECT s.id FROM students s JOIN users u ON u.id = s.user_id WHERE u.username = 'kuznetsova.o'),
        'FATHER');

INSERT INTO teacher_class_subject (teacher_id, class_id, subject_id, academic_year)
SELECT t.id, c.id, t.subject_id, '2026/2027'
FROM teachers t
         CROSS JOIN classes c
WHERE c.year = 7;

--rollback DELETE FROM teacher_class_subject; DELETE FROM parent_student; DELETE FROM parents; DELETE FROM students; DELETE FROM teachers; DELETE FROM user_roles; DELETE FROM users; DELETE FROM classes; DELETE FROM subjects;
