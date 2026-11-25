package br.com.jeniffer.pessoas.util;

/** Configuração de conexão dos 4 bancos. Valores batem com o docker-compose.yml
 *  do projeto; ajuste aqui se você estiver rodando os bancos de outra forma. */
public final class DbConfig {

    // PostgreSQL
    public static final String POSTGRES_URL = "jdbc:postgresql://localhost:5432/pessoas";
    public static final String POSTGRES_USER = "pessoas_app";
    public static final String POSTGRES_PASSWORD = "pessoas_app";

    // Redis
    public static final String REDIS_HOST = "localhost";
    public static final int REDIS_PORT = 6380;
    public static final int REDIS_CACHE_TTL_SEGUNDOS = 300;

    // MongoDB
    public static final String MONGO_URI = "mongodb://localhost:27017";
    public static final String MONGO_DATABASE = "pessoas_logs";
    public static final String MONGO_COLLECTION = "operacoes";

    // Neo4j
    public static final String NEO4J_URI = "bolt://localhost:7687";
    public static final String NEO4J_USER = "neo4j";
    public static final String NEO4J_PASSWORD = "pessoas_app";

    private DbConfig() {
    }
}
