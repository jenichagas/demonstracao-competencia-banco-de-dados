package br.com.jeniffer.pessoas.repository;

import br.com.jeniffer.pessoas.util.DbConfig;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Neo4j: grafo de relacionamentos entre pessoas (ex.: amizade).
 *  Cada Pessoa vira um nó :Pessoa espelhando o id do PostgreSQL. */
public class RelacionamentoRepository implements AutoCloseable {

    private final Driver driver;

    public RelacionamentoRepository() {
        this.driver = GraphDatabase.driver(
                DbConfig.NEO4J_URI, AuthTokens.basic(DbConfig.NEO4J_USER, DbConfig.NEO4J_PASSWORD));
    }

    public void sincronizarPessoa(Long id, String nome, String cpf) {
        String cypher = "MERGE (p:Pessoa {id: $id}) SET p.nome = $nome, p.cpf = $cpf";
        try (Session session = driver.session()) {
            session.executeWrite(tx -> tx.run(cypher, Map.of("id", id, "nome", nome, "cpf", cpf)).consume());
        }
    }

    public void removerPessoa(Long id) {
        String cypher = "MATCH (p:Pessoa {id: $id}) DETACH DELETE p";
        try (Session session = driver.session()) {
            session.executeWrite(tx -> tx.run(cypher, Map.of("id", id)).consume());
        }
    }

    public void removerPessoaPorCpf(String cpf) {
        String cypher = "MATCH (p:Pessoa {cpf: $cpf}) DETACH DELETE p";
        try (Session session = driver.session()) {
            session.executeWrite(tx -> tx.run(cypher, Map.of("cpf", cpf)).consume());
        }
    }

    public void criarAmizade(Long idPessoa1, Long idPessoa2) {
        String cypher = """
                MATCH (a:Pessoa {id: $id1}), (b:Pessoa {id: $id2})
                MERGE (a)-[:AMIGO_DE]-(b)
                """;
        try (Session session = driver.session()) {
            session.executeWrite(tx -> tx.run(cypher, Map.of("id1", idPessoa1, "id2", idPessoa2)).consume());
        }
    }

    public List<String> listarAmigos(Long idPessoa) {
        String cypher = """
                MATCH (p:Pessoa {id: $id})-[:AMIGO_DE]-(amigo:Pessoa)
                RETURN amigo.nome AS nome
                """;
        List<String> amigos = new ArrayList<>();
        try (Session session = driver.session()) {
            List<Record> registros = session.executeRead(tx -> tx.run(cypher, Map.of("id", idPessoa)).list());
            for (Record registro : registros) {
                amigos.add(registro.get("nome").asString());
            }
        }
        return amigos;
    }

    @Override
    public void close() {
        driver.close();
    }
}
