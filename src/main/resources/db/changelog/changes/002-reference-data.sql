--liquibase formatted sql

--changeset school:002-roles
INSERT INTO roles (name)
VALUES ('SUPERVISOR'),
       ('TEACHER'),
       ('STUDENT'),
       ('PARENT');
--rollback DELETE FROM roles;
