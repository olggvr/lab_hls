--liquibase formatted sql

-- Демо-данные. Применяются только с контекстом "demo" (LIQUIBASE_CONTEXTS=demo).
-- Пароль у всех демо-пользователей: password

--changeset school:003-demo-data context:demo
INSERT INTO subjects (name)
VALUES ('Математика'),
       ('Русский язык'),
       ('Физика');

INSERT INTO classes (name, year)
VALUES ('7А', 7),
       ('7Б', 7),
       ('8А', 8);

INSERT INTO users (username, password_hash, full_name, email, phone, role)
VALUES ('admin', '$2a$10$A2wcoAHtI/scHiw22IWpoe9oe8e/gNgM8URRlz9wigHDkGPtIofym', 'Администратор Школы', 'admin@school.local', '+70000000000', 'SUPERVISOR'),
       ('ivanova', '$2a$10$A2wcoAHtI/scHiw22IWpoe9oe8e/gNgM8URRlz9wigHDkGPtIofym', 'Иванова Мария Петровна', 'ivanova@school.local', '+70000000001', 'TEACHER'),
       ('petrov', '$2a$10$A2wcoAHtI/scHiw22IWpoe9oe8e/gNgM8URRlz9wigHDkGPtIofym', 'Петров Сергей Иванович', 'petrov@school.local', '+70000000002', 'TEACHER'),
       ('smirnov.a', '$2a$10$A2wcoAHtI/scHiw22IWpoe9oe8e/gNgM8URRlz9wigHDkGPtIofym', 'Смирнов Алексей', 'smirnov.a@school.local', NULL, 'STUDENT'),
       ('kuznetsova.o', '$2a$10$A2wcoAHtI/scHiw22IWpoe9oe8e/gNgM8URRlz9wigHDkGPtIofym', 'Кузнецова Ольга', 'kuznetsova.o@school.local', NULL, 'STUDENT'),
       ('volkov.d', '$2a$10$A2wcoAHtI/scHiw22IWpoe9oe8e/gNgM8URRlz9wigHDkGPtIofym', 'Волков Дмитрий', 'volkov.d@school.local', NULL, 'STUDENT'),
       ('smirnova.e', '$2a$10$A2wcoAHtI/scHiw22IWpoe9oe8e/gNgM8URRlz9wigHDkGPtIofym', 'Смирнова Елена', 'smirnova.e@mail.local', '+70000000010', 'PARENT'),
       ('kuznetsov.p', '$2a$10$A2wcoAHtI/scHiw22IWpoe9oe8e/gNgM8URRlz9wigHDkGPtIofym', 'Кузнецов Павел', 'kuznetsov.p@mail.local', '+70000000011', 'PARENT');

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

INSERT INTO assignments (teacher_id, class_id, subject_id, title, description, due_date)
VALUES ((SELECT t.id FROM teachers t JOIN users u ON u.id = t.user_id WHERE u.username = 'ivanova'),
        (SELECT id FROM classes WHERE name = '7А'),
        (SELECT id FROM subjects WHERE name = 'Математика'),
        'Линейные уравнения', 'Учебник, §12, задачи 1–10', CURRENT_DATE + 7),
       ((SELECT t.id FROM teachers t JOIN users u ON u.id = t.user_id WHERE u.username = 'petrov'),
        (SELECT id FROM classes WHERE name = '7А'),
        (SELECT id FROM subjects WHERE name = 'Русский язык'),
        'Сочинение «Как я провёл лето»', 'Объём — не менее 1 страницы', CURRENT_DATE + 5);

INSERT INTO submissions (assignment_id, student_id, content_url, status)
VALUES ((SELECT id FROM assignments WHERE title = 'Линейные уравнения'),
        (SELECT s.id FROM students s JOIN users u ON u.id = s.user_id WHERE u.username = 'smirnov.a'),
        'https://files.school.local/solutions/smirnov-linear.pdf',
        'SUBMITTED');
--rollback DELETE FROM submissions; DELETE FROM assignments; DELETE FROM teacher_class_subject; DELETE FROM parent_student; DELETE FROM parents; DELETE FROM students; DELETE FROM teachers; DELETE FROM user_roles; DELETE FROM users; DELETE FROM classes; DELETE FROM subjects;
