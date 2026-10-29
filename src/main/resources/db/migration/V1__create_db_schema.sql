
CREATE DATABASE IF NOT EXISTS edu_classes ;

USE edu_classes;

CREATE TABLE users
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role in ('USER', 'ADMIN'))
);

CREATE TABLE resources
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    category    VARCHAR(100) NOT NULL,
    type        VARCHAR(30)  NOT NULL,
    difficulty  VARCHAR(20)  NOT NULL,
    metadata    JSON         NOT NULL,
    created_by  BIGINT      NOT NULL,
    created_at  TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6)  ON UPDATE CURRENT_TIMESTAMP(6),


    CONSTRAINT pk_resources PRIMARY KEY (id),
    CONSTRAINT chk_resources_type CHECK (type IN ('VIDEO', 'PDF', 'ARTICLE')),
    CONSTRAINT chk_resources_difficulty CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT uk_resource_title_user UNIQUE (title,created_by),
    CONSTRAINT fk_resource_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT

);

CREATE INDEX idx_users_email ON users(email);



