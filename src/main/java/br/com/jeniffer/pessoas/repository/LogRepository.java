package br.com.jeniffer.pessoas.repository;

import br.com.jeniffer.pessoas.util.DbConfig;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.time.Instant;
import java.util.UUID;

/** MongoDB: logs de auditoria das operações realizadas sobre Pessoa
 *  (criação, leitura, atualização, exclusão, cache hit/miss, erros).
 *  Cada execução do programa recebe um id próprio, gravado em todos os seus logs. */
public class LogRepository implements AutoCloseable {

    private final MongoClient client;
    private final MongoCollection<Document> collection;
    private final String execucaoId = UUID.randomUUID().toString();

    public LogRepository() {
        this.client = MongoClients.create(DbConfig.MONGO_URI);
        this.collection = client
                .getDatabase(DbConfig.MONGO_DATABASE)
                .getCollection(DbConfig.MONGO_COLLECTION);
    }

    public void registrar(String operacao, String detalhes) {
        Document log = novoLog("INFO", operacao)
                .append("detalhes", detalhes);
        collection.insertOne(log);
    }

    public void registrarErro(String operacao, String mensagemErro) {
        Document log = novoLog("ERRO", operacao)
                .append("erro", mensagemErro);
        collection.insertOne(log);
    }

    /** Imprime só os logs desta execução, do mais antigo para o mais novo. */
    public void imprimirLogsDaExecucao() {
        System.out.println("  [MongoDB] logs da execução " + execucaoId + ":");
        for (Document doc : collection.find(Filters.eq("execucao", execucaoId)).sort(Sorts.ascending("_id"))) {
            System.out.println("    " + doc.toJson());
        }
    }

    private Document novoLog(String nivel, String operacao) {
        return new Document()
                .append("execucao", execucaoId)
                .append("nivel", nivel)
                .append("operacao", operacao)
                .append("timestamp", Instant.now().toString());
    }

    @Override
    public void close() {
        client.close();
    }
}
