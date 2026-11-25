package br.com.jeniffer.pessoas.model;

import java.time.LocalDate;
import java.util.Objects;

/** Entidade de domínio. Sem anotações de ORM: o mapeamento é feito manualmente
 *  em cada repositório, para deixar explícito o que cada banco recebe. */
public class Pessoa {

    private Long id;
    private String nome;
    private String email;
    private String cpf;
    private LocalDate dataNascimento;

    public Pessoa() {
    }

    public Pessoa(String nome, String email, String cpf, LocalDate dataNascimento) {
        this.nome = nome;
        this.email = email;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
    }

    public Pessoa(Long id, String nome, String email, String cpf, LocalDate dataNascimento) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pessoa pessoa)) return false;
        return Objects.equals(id, pessoa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Pessoa{id=%d, nome='%s', email='%s', cpf='%s', dataNascimento=%s}"
                .formatted(id, nome, email, cpf, dataNascimento);
    }
}
