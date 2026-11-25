# Demonstração de Competência — CRUD de Pessoa com múltiplos bancos de dados

Projeto individual em **Java puro** (sem framework, sem GUI, sem API REST) que
implementa um CRUD da entidade **Pessoa**, combinando quatro bancos de dados,
cada um com uma responsabilidade específica:

| Banco | Papel no sistema |
|---|---|
| **PostgreSQL** | Armazenamento principal dos registros de Pessoa (JDBC puro, sem ORM) |
| **Redis** | Cache de leitura para consultas por CPF |
| **MongoDB** | Log de auditoria de todas as operações (create/read/update/delete, cache hit/miss, relacionamentos) |
| **Neo4j** | Grafo de relacionamentos entre pessoas (amizade) |

## Integrante

- Jeniffer Chagas

## Tecnologias

- Java 21 (compilado com `--release 21`; testado com JDK 25)
- Maven
- Drivers nativos: `postgresql`, `jedis` (Redis), `mongodb-driver-sync`, `neo4j-java-driver`
- Docker Compose (para subir os 4 bancos localmente)

## Estrutura do projeto

```
262-demonstracao-competencia/
├── README.md
├── pom.xml
├── docker-compose.yml           → sobe PostgreSQL, Redis, MongoDB e Neo4j
├── sql/
│   └── schema.sql                → tabela "pessoa" (rodar uma vez, manualmente)
└── src/main/java/br/com/jeniffer/pessoas/
    ├── Main.java                  → ponto de entrada; roda a demonstração completa
    ├── model/
    │   └── Pessoa.java
    ├── repository/
    │   ├── PessoaRepository.java        → CRUD no PostgreSQL
    │   ├── PessoaCacheRepository.java   → cache no Redis
    │   ├── LogRepository.java           → logs no MongoDB
    │   └── RelacionamentoRepository.java → relacionamentos no Neo4j
    └── util/
        └── DbConfig.java          → strings de conexão dos 4 bancos
```

## Pré-requisitos

- JDK 21 ou superior
- Maven (ou usar o Maven embutido no Eclipse)
- Docker + Docker Compose

## Como executar

### 1. Subir os bancos de dados

```bash
docker compose up -d
```

> Se a porta **6379** (Redis) já estiver em uso por outro serviço na sua
> máquina, o `docker-compose.yml` já mapeia o Redis deste projeto para a
> porta **6380** do host, então não há conflito.

Aguarde alguns segundos para os containers ficarem saudáveis:

```bash
docker compose ps
```

### 2. Criar a tabela no PostgreSQL (uma única vez)

```bash
docker exec -i pessoas-postgres psql -U pessoas_app -d pessoas < sql/schema.sql
```

### 3. Rodar a aplicação

**Pela linha de comando (Maven):**

```bash
mvn exec:java
```

**Pelo Eclipse:**
Importe o projeto como *Existing Maven Project*, aguarde o Maven baixar as
dependências e rode `src/main/java/br/com/jeniffer/pessoas/Main.java` como
*Java Application* (botão direito → Run As → Java Application).

**Gerando um .jar executável:**

```bash
mvn package
java -jar target/demonstracao-competencia-pessoas-jar-with-dependencies.jar
```

A classe `Main` pode ser executada **quantas vezes for necessário**: o
primeiro passo da demonstração ("0. Preparando ambiente") remove os dados de
uma execução anterior antes de recriá-los, então não há erro de CPF/e-mail
duplicado ao rodar de novo durante a gravação do vídeo.

### 4. Encerrar os bancos (quando terminar)

```bash
docker compose down          # mantém os dados
docker compose down -v       # remove também os volumes (apaga os dados)
```

## O que a demonstração mostra

Rodando `Main`, o console imprime, em sequência:

1. **Preparação do ambiente** — remove dados de uma execução anterior (idempotência).
2. **CREATE** — cadastra 3 pessoas no PostgreSQL e sincroniza cada uma como
   nó `:Pessoa` no Neo4j; cada operação gera um log no MongoDB.
3. **READ** — consulta uma pessoa por CPF duas vezes: a primeira dá *cache
   miss* (vai ao PostgreSQL e povoa o Redis); a segunda dá *cache hit* (lê
   direto do Redis, sem tocar no PostgreSQL).
4. **UPDATE** — atualiza o e-mail de uma pessoa no PostgreSQL, invalida a
   entrada correspondente no Redis e mostra a consulta seguinte voltando a
   dar *cache miss*.
5. **Neo4j** — cria relacionamentos `AMIGO_DE` entre pessoas e consulta os
   amigos de uma delas.
6. **DELETE** — remove uma pessoa dos 4 bancos (PostgreSQL, Redis, Neo4j) e
   registra a operação no MongoDB.
7. **Estado final** — lista as pessoas restantes no PostgreSQL.
8. **Auditoria** — imprime os últimos 10 registros de log gravados no
   MongoDB durante a execução.

## Vídeo de apresentação

Vídeo de até 5 minutos publicado no YouTube, mostrando o funcionamento do
sistema e a integração com os 4 bancos de dados: **[link a adicionar]**
