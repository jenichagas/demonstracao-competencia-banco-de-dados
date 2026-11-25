-- Executar manualmente no PostgreSQL antes de rodar a aplicação
-- (o Main não cria o schema sozinho, para deixar explícito o que existe no banco)

CREATE TABLE IF NOT EXISTS pessoa (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    cpf             VARCHAR(11)  NOT NULL UNIQUE,
    data_nascimento DATE         NOT NULL
);
