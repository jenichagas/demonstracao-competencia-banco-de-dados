package br.com.jeniffer.pessoas.repository;

import br.com.jeniffer.pessoas.model.Pessoa;
import br.com.jeniffer.pessoas.util.DbConfig;
import redis.clients.jedis.JedisPooled;

import java.time.LocalDate;
import java.util.Optional;

/** Redis: cache de leitura para consultas de Pessoa por CPF.
 *  Chave: "pessoa:cpf:<cpf>" -> valor serializado como "id|nome|email|cpf|dataNascimento". */
public class PessoaCacheRepository implements AutoCloseable {

    private static final String PREFIXO_CHAVE = "pessoa:cpf:";
    private final JedisPooled jedis;

    public PessoaCacheRepository() {
        this.jedis = new JedisPooled(DbConfig.REDIS_HOST, DbConfig.REDIS_PORT);
    }

    public Optional<Pessoa> buscar(String cpf) {
        String valor = jedis.get(PREFIXO_CHAVE + cpf);
        return valor == null ? Optional.empty() : Optional.of(desserializar(valor));
    }

    public void salvar(Pessoa pessoa) {
        jedis.setex(PREFIXO_CHAVE + pessoa.getCpf(), DbConfig.REDIS_CACHE_TTL_SEGUNDOS, serializar(pessoa));
    }

    public void invalidar(String cpf) {
        jedis.del(PREFIXO_CHAVE + cpf);
    }

    private String serializar(Pessoa pessoa) {
        return String.join("|",
                String.valueOf(pessoa.getId()),
                pessoa.getNome(),
                pessoa.getEmail(),
                pessoa.getCpf(),
                pessoa.getDataNascimento().toString());
    }

    private Pessoa desserializar(String valor) {
        String[] campos = valor.split("\\|", 5);
        return new Pessoa(
                Long.valueOf(campos[0]),
                campos[1],
                campos[2],
                campos[3],
                LocalDate.parse(campos[4]));
    }

    @Override
    public void close() {
        jedis.close();
    }
}
