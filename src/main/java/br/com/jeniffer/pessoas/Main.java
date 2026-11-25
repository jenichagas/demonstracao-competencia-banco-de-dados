package br.com.jeniffer.pessoas;

import br.com.jeniffer.pessoas.model.Pessoa;
import br.com.jeniffer.pessoas.repository.LogRepository;
import br.com.jeniffer.pessoas.repository.PessoaCacheRepository;
import br.com.jeniffer.pessoas.repository.PessoaRepository;
import br.com.jeniffer.pessoas.repository.RelacionamentoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Demonstração de competência: CRUD de Pessoa combinando 4 bancos de dados,
 * cada um com um papel específico:
 *   - PostgreSQL  -> armazenamento principal
 *   - Redis       -> cache de leitura por CPF
 *   - MongoDB     -> log de auditoria das operações
 *   - Neo4j       -> relacionamentos (amizade) entre pessoas
 *
 * Pré-requisito: "docker compose up -d" (ver README.md) e o schema.sql
 * executado no PostgreSQL antes de rodar esta classe.
 */
public class Main {

    // CPFs fixos usados pela demonstração — permitem rodar a classe várias
    // vezes seguidas (útil ao gravar o vídeo) sem violar as constraints
    // UNIQUE de cpf/email no PostgreSQL.
    private static final List<String> CPFS_DEMO = List.of("11122233344", "22233344455", "33344455566");

    public static void main(String[] args) {
        PessoaRepository pessoaRepository = new PessoaRepository();

        try (PessoaCacheRepository cache = new PessoaCacheRepository();
             LogRepository log = new LogRepository();
             RelacionamentoRepository relacionamentos = new RelacionamentoRepository()) {

            try {
                executarDemonstracao(pessoaRepository, cache, log, relacionamentos);
            } catch (RuntimeException e) {
                // Erro inesperado em qualquer passo: grava no MongoDB antes de interromper
                registrarErroSemInterromper(log, "DEMONSTRACAO", e);
                throw e;
            }

        } catch (Exception e) {
            System.err.println();
            System.err.println("Falha ao executar a demonstração: " + e.getMessage());
            System.err.println("Verifique se os 4 bancos estão no ar (docker compose up -d) "
                    + "e se o schema.sql já foi executado no PostgreSQL.");
            throw new RuntimeException(e);
        }
    }

    private static void executarDemonstracao(PessoaRepository pessoaRepository, PessoaCacheRepository cache,
                                             LogRepository log, RelacionamentoRepository relacionamentos) {
        secao("0. Preparando ambiente (removendo dados de uma execução anterior, se houver)");
        for (String cpf : CPFS_DEMO) {
            pessoaRepository.excluirPorCpf(cpf);
            cache.invalidar(cpf);
            relacionamentos.removerPessoaPorCpf(cpf);
        }
        System.out.println("  Ambiente limpo — a demonstração pode ser executada quantas vezes for preciso.");

        secao("1. CREATE — cadastrando pessoas no PostgreSQL");
        Pessoa lais = criar(pessoaRepository, log, relacionamentos,
                "Laís Vicente", "lais@exemplo.com", "11122233344", LocalDate.of(1997, 3, 12));
        Pessoa rafael = criar(pessoaRepository, log, relacionamentos,
                "Rafael Souza", "rafael@exemplo.com", "22233344455", LocalDate.of(1995, 7, 2));
        Pessoa bia = criar(pessoaRepository, log, relacionamentos,
                "Bia Torres", "bia@exemplo.com", "33344455566", LocalDate.of(1999, 11, 20));

        secao("2. ERRO — tentando cadastrar um CPF que já existe");
        tentarCadastrarCpfDuplicado(pessoaRepository, log, lais.getCpf());

        secao("3. READ — consulta por CPF (Postgres + cache Redis)");
        buscarPorCpf(pessoaRepository, cache, log, lais.getCpf()); // cache miss
        buscarPorCpf(pessoaRepository, cache, log, lais.getCpf()); // cache hit

        secao("4. UPDATE — atualizando e-mail (Postgres + invalidação de cache)");
        lais.setEmail("lais.vicente@exemplo.com");
        pessoaRepository.atualizar(lais);
        cache.invalidar(lais.getCpf());
        log.registrar("UPDATE", "Pessoa id=" + lais.getId() + " teve o e-mail atualizado");
        relacionamentos.sincronizarPessoa(lais.getId(), lais.getNome(), lais.getCpf());
        System.out.println("  [PostgreSQL] e-mail atualizado: " + lais.getEmail());
        System.out.println("  [Redis] cache da Laís invalidado após o update");

        buscarPorCpf(pessoaRepository, cache, log, lais.getCpf()); // volta a dar miss (foi invalidado)

        secao("5. Neo4j — relacionamentos de amizade");
        relacionamentos.criarAmizade(lais.getId(), rafael.getId());
        relacionamentos.criarAmizade(rafael.getId(), bia.getId());
        log.registrar("RELACIONAMENTO", "Amizade criada entre " + lais.getNome() + " e " + rafael.getNome());
        log.registrar("RELACIONAMENTO", "Amizade criada entre " + rafael.getNome() + " e " + bia.getNome());

        List<String> amigosDoRafael = relacionamentos.listarAmigos(rafael.getId());
        System.out.println("  [Neo4j] amigos de " + rafael.getNome() + ": " + amigosDoRafael);

        secao("6. DELETE — removendo a Bia do PostgreSQL, do Redis e do Neo4j (e registrando no MongoDB)");
        pessoaRepository.excluir(bia.getId());
        cache.invalidar(bia.getCpf());
        relacionamentos.removerPessoa(bia.getId());
        log.registrar("DELETE", "Pessoa id=" + bia.getId() + " (" + bia.getNome() + ") removida");
        System.out.println("  [PostgreSQL] pessoa removida: " + bia.getNome());
        System.out.println("  [Redis] cache da Bia invalidado");
        System.out.println("  [Neo4j] nó e relacionamentos da Bia removidos");
        System.out.println("  [MongoDB] exclusão registrada no log");

        secao("7. Estado final no PostgreSQL");
        for (Pessoa pessoa : pessoaRepository.listarTodas()) {
            System.out.println("  " + pessoa);
        }

        secao("8. Auditoria — logs desta execução no MongoDB");
        log.imprimirLogsDaExecucao();
    }

    private static Pessoa criar(PessoaRepository pessoaRepository, LogRepository log,
                                 RelacionamentoRepository relacionamentos,
                                 String nome, String email, String cpf, LocalDate nascimento) {
        Pessoa pessoa = pessoaRepository.salvar(new Pessoa(nome, email, cpf, nascimento));
        log.registrar("CREATE", "Pessoa criada: id=" + pessoa.getId() + ", nome=" + nome);
        relacionamentos.sincronizarPessoa(pessoa.getId(), pessoa.getNome(), pessoa.getCpf());
        System.out.println("  [PostgreSQL] pessoa criada: " + pessoa);
        System.out.println("  [Neo4j] nó :Pessoa sincronizado para " + pessoa.getNome());
        return pessoa;
    }

    private static void buscarPorCpf(PessoaRepository pessoaRepository, PessoaCacheRepository cache,
                                      LogRepository log, String cpf) {
        Optional<Pessoa> emCache = cache.buscar(cpf);
        if (emCache.isPresent()) {
            System.out.println("  [Redis] CACHE HIT para CPF " + cpf + " -> " + emCache.get());
            log.registrar("READ", "CPF " + cpf + " lido do cache Redis (cache hit)");
            return;
        }

        System.out.println("  [Redis] cache MISS para CPF " + cpf + " — consultando PostgreSQL...");
        Optional<Pessoa> pessoa = pessoaRepository.buscarPorCpf(cpf);
        if (pessoa.isPresent()) {
            cache.salvar(pessoa.get());
            System.out.println("  [PostgreSQL] pessoa encontrada e armazenada no cache: " + pessoa.get());
            log.registrar("READ", "CPF " + cpf + " lido do PostgreSQL (cache miss) e recolocado em cache");
        } else {
            System.out.println("  [PostgreSQL] nenhuma pessoa encontrada para o CPF " + cpf);
            log.registrar("READ", "CPF " + cpf + " não encontrado");
        }
    }

    // O CPF tem UNIQUE no schema.sql, então o PostgreSQL recusa o segundo cadastro
    private static void tentarCadastrarCpfDuplicado(PessoaRepository pessoaRepository, LogRepository log,
                                                     String cpfExistente) {
        try {
            pessoaRepository.salvar(new Pessoa("Cadastro Duplicado", "duplicado@exemplo.com",
                    cpfExistente, LocalDate.of(2000, 1, 1)));
            System.out.println("  [PostgreSQL] cadastro aceito (não era esperado)");
        } catch (RuntimeException e) {
            String mensagem = descreverErro(e);
            log.registrarErro("CREATE", mensagem);
            System.out.println("  [PostgreSQL] cadastro recusado: " + mensagem);
            System.out.println("  [MongoDB] erro registrado no log");
        }
    }

    // Se o próprio MongoDB estiver fora do ar, avisa no console em vez de esconder o erro original
    private static void registrarErroSemInterromper(LogRepository log, String operacao, RuntimeException erro) {
        try {
            log.registrarErro(operacao, descreverErro(erro));
        } catch (RuntimeException falhaNoLog) {
            System.err.println("  (não foi possível gravar o erro no MongoDB: " + falhaNoLog.getMessage() + ")");
        }
    }

    // Junta a mensagem do repositório com a primeira linha da causa (ex.: a mensagem do PostgreSQL)
    private static String descreverErro(RuntimeException erro) {
        Throwable causa = erro.getCause();
        if (causa == null || causa.getMessage() == null) {
            return erro.getMessage();
        }
        return erro.getMessage() + ": " + causa.getMessage().lines().findFirst().orElse("");
    }

    private static void secao(String titulo) {
        System.out.println();
        System.out.println("=== " + titulo + " ===");
    }
}
