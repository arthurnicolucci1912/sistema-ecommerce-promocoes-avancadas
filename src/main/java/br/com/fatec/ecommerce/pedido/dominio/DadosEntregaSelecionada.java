package br.com.fatec.ecommerce.pedido.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.DadoInvalidoException;
import jakarta.persistence.*;
import java.util.Objects;

/** Snapshot imutavel da modalidade contratada; nao referencia tabelas nem provedores de entrega. */
@Embeddable
public class DadosEntregaSelecionada {
    @Column(name="opcao_entrega_codigo", updatable=false, length=128) private String codigo;
    @Column(name="opcao_entrega_fornecedor", updatable=false, length=80) private String fornecedor;
    @Column(name="opcao_entrega_modalidade", updatable=false, length=40) private String modalidade;
    @Column(name="opcao_entrega_prazo_dias", updatable=false) private Integer prazoDias;
    protected DadosEntregaSelecionada() {}
    public DadosEntregaSelecionada(String codigo, String fornecedor, String modalidade, int prazoDias) {
        if (fornecedor == null || !fornecedor.matches("[a-z0-9_-]{1,80}")
                || modalidade == null || !modalidade.matches("[A-Z0-9_-]{1,40}")
                || !Objects.equals(codigo, fornecedor + ":" + modalidade) || prazoDias <= 0)
            throw new DadoInvalidoException("Dados da entrega selecionada inválidos.");
        this.codigo=codigo; this.fornecedor=fornecedor; this.modalidade=modalidade; this.prazoDias=prazoDias;
    }
    public String getCodigo() { return codigo; }
    public String getFornecedor() { return fornecedor; }
    public String getModalidade() { return modalidade; }
    public Integer getPrazoDias() { return prazoDias; }
}
