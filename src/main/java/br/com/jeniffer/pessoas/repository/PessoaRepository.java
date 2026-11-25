package br.com.jeniffer.pessoas.repository;

import br.com.jeniffer.pessoas.model.Pessoa;
import br.com.jeniffer.pessoas.util.DbConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PostgreSQL: armazenamento principal da entidade Pessoa.
 *  JDBC puro, sem ORM — cada método abre e fecha sua própria conexão. */
public class PessoaRepository {

    private Connection abrirConexao() throws SQLException {
        return DriverManager.getConnection(
                DbConfig.POSTGRES_URL, DbConfig.POSTGRES_USER, DbConfig.POSTGRES_PASSWORD);
    }

    public Pessoa salvar(Pessoa pessoa) {
        String sql = "INSERT INTO pessoa (nome, email, cpf, data_nascimento) VALUES (?, ?, ?, ?)";
        try (Connection conn = abrirConexao();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, pessoa.getNome());
            stmt.setString(2, pessoa.getEmail());
            stmt.setString(3, pessoa.getCpf());
            stmt.setObject(4, pessoa.getDataNascimento());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    pessoa.setId(keys.getLong(1));
                }
            }
            return pessoa;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar pessoa no PostgreSQL", e);
        }
    }

    public Optional<Pessoa> buscarPorId(Long id) {
        String sql = "SELECT id, nome, email, cpf, data_nascimento FROM pessoa WHERE id = ?";
        try (Connection conn = abrirConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar pessoa por id no PostgreSQL", e);
        }
    }

    public Optional<Pessoa> buscarPorCpf(String cpf) {
        String sql = "SELECT id, nome, email, cpf, data_nascimento FROM pessoa WHERE cpf = ?";
        try (Connection conn = abrirConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar pessoa por CPF no PostgreSQL", e);
        }
    }

    public List<Pessoa> listarTodas() {
        String sql = "SELECT id, nome, email, cpf, data_nascimento FROM pessoa ORDER BY id";
        List<Pessoa> pessoas = new ArrayList<>();
        try (Connection conn = abrirConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                pessoas.add(mapear(rs));
            }
            return pessoas;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar pessoas no PostgreSQL", e);
        }
    }

    public boolean atualizar(Pessoa pessoa) {
        String sql = "UPDATE pessoa SET nome = ?, email = ?, cpf = ?, data_nascimento = ? WHERE id = ?";
        try (Connection conn = abrirConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pessoa.getNome());
            stmt.setString(2, pessoa.getEmail());
            stmt.setString(3, pessoa.getCpf());
            stmt.setObject(4, pessoa.getDataNascimento());
            stmt.setLong(5, pessoa.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar pessoa no PostgreSQL", e);
        }
    }

    public boolean excluir(Long id) {
        String sql = "DELETE FROM pessoa WHERE id = ?";
        try (Connection conn = abrirConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir pessoa no PostgreSQL", e);
        }
    }

    public boolean excluirPorCpf(String cpf) {
        String sql = "DELETE FROM pessoa WHERE cpf = ?";
        try (Connection conn = abrirConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao excluir pessoa por CPF no PostgreSQL", e);
        }
    }

    private Pessoa mapear(ResultSet rs) throws SQLException {
        return new Pessoa(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("email"),
                rs.getString("cpf"),
                rs.getObject("data_nascimento", LocalDate.class));
    }
}
