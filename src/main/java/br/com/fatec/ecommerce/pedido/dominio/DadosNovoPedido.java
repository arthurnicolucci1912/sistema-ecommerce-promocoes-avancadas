package br.com.fatec.ecommerce.pedido.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.util.List;
import java.util.Objects;

/**
 * Snapshot comercial a partir do qual um pedido e criado (o checkout monta esta estrutura). Os totais vem
 * informados e sao conferidos por {@link Pedido#criar}: o total de cada item e derivado de preco e quantidade.
 *
 * @param clienteId      cliente dono do pedido
 * @param endereco       endereco de entrega copiado
 * @param itens          itens comprados, ao menos um
 * @param descontos      descontos aplicados (pode ser vazio)
 * @param frete          valor do frete
 * @param subtotal       soma dos totais dos itens
 * @param totalDescontos soma dos descontos
 * @param total          subtotal menos descontos mais frete
 */
public record DadosNovoPedido(Long clienteId, EnderecoEntrega endereco, List<Item> itens, List<Desconto> descontos,
        Dinheiro frete, Dinheiro subtotal, Dinheiro totalDescontos, Dinheiro total, DadosEntregaSelecionada opcaoEntrega) {
    /** Snapshots legados e testes de ciclo de vida podem nao possuir modalidade registrada. */
    public DadosNovoPedido(Long clienteId, EnderecoEntrega endereco, List<Item> itens, List<Desconto> descontos,
            Dinheiro frete, Dinheiro subtotal, Dinheiro totalDescontos, Dinheiro total) {
        this(clienteId,endereco,itens,descontos,frete,subtotal,totalDescontos,total,null);
    }

    /** Item do snapshot: identificacao, nome e variacao do produto no momento da compra. */
    public record Item(String sku, String nomeProduto, String descricaoVariacao, int quantidade,
            Dinheiro precoUnitario) {
    }

    /** Desconto aplicado, com nome (descricao) e valor. */
    public record Desconto(String descricao, OrigemDesconto origem, AlvoDoDesconto alvo, Dinheiro valor,
            Long referenciaId, String codigo) {
        public Desconto(String descricao, OrigemDesconto origem, AlvoDoDesconto alvo, Dinheiro valor) {
            this(descricao, origem, alvo, valor, null, null);
        }
    }

    public DadosNovoPedido {
        Objects.requireNonNull(clienteId, "o cliente do pedido nao pode ser nulo");
        Objects.requireNonNull(endereco, "o endereco de entrega nao pode ser nulo");
        itens = List.copyOf(Objects.requireNonNull(itens, "os itens nao podem ser nulos"));
        descontos = List.copyOf(descontos == null ? List.of() : descontos);
        Objects.requireNonNull(frete, "o frete nao pode ser nulo");
        Objects.requireNonNull(subtotal, "o subtotal nao pode ser nulo");
        Objects.requireNonNull(totalDescontos, "o total de descontos nao pode ser nulo");
        Objects.requireNonNull(total, "o total nao pode ser nulo");
    }
}
