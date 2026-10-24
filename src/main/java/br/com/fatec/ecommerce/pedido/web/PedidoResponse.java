package br.com.fatec.ecommerce.pedido.web;

import br.com.fatec.ecommerce.pedido.dominio.AlvoDoDesconto;
import br.com.fatec.ecommerce.pedido.dominio.DescontoPedido;
import br.com.fatec.ecommerce.pedido.dominio.EnderecoEntrega;
import br.com.fatec.ecommerce.pedido.dominio.EntradaHistorico;
import br.com.fatec.ecommerce.pedido.dominio.ItemPedido;
import br.com.fatec.ecommerce.pedido.dominio.OrigemDesconto;
import br.com.fatec.ecommerce.pedido.dominio.Pedido;
import br.com.fatec.ecommerce.pedido.dominio.SituacaoPedido;
import br.com.fatec.ecommerce.pedido.dominio.TipoResponsavel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Detalhe do pedido: snapshot comercial (itens, descontos, frete, totais e endereço de entrega), situação atual e
 * histórico em ordem cronológica. O CEP vai somente com dígitos.
 */
public record PedidoResponse(Long id, Long clienteId, SituacaoPedido situacao, List<ItemResponse> itens,
        List<DescontoResponse> descontos, BigDecimal frete, BigDecimal subtotal, BigDecimal totalDescontos,
        BigDecimal total, EnderecoEntregaResponse enderecoEntrega, Instant criadoEm,
        List<EntradaHistoricoResponse> historico, EntregaSelecionadaResponse opcaoEntrega, BigDecimal freteLiquido) {
    public record EntregaSelecionadaResponse(String codigo, String fornecedor, String modalidade, int prazoDias) {
        public static EntregaSelecionadaResponse de(br.com.fatec.ecommerce.pedido.dominio.DadosEntregaSelecionada e) {
            return e == null ? null : new EntregaSelecionadaResponse(e.getCodigo(), e.getFornecedor(), e.getModalidade(), e.getPrazoDias());
        }
    }

    public record ItemResponse(String sku, String nomeProduto, String descricaoVariacao, int quantidade,
            BigDecimal precoUnitario, BigDecimal totalItem) {

        static ItemResponse de(ItemPedido i) {
            return new ItemResponse(i.getSku(), i.getNomeProduto(), i.getDescricaoVariacao(), i.getQuantidade(),
                    i.getPrecoUnitario().valor(), i.getTotalItem().valor());
        }
    }

    public record DescontoResponse(String descricao, OrigemDesconto origem, AlvoDoDesconto alvo, BigDecimal valor, Long referenciaId, String codigo) {

        static DescontoResponse de(DescontoPedido d) {
            return new DescontoResponse(d.getDescricao(), d.getOrigem(), d.getAlvo(), d.getValor().valor(), d.getReferenciaId(), d.getCodigo());
        }
    }

    public record EnderecoEntregaResponse(String apelido, String cep, String logradouro, String numero,
            String complemento, String bairro, String cidade, String uf) {

        static EnderecoEntregaResponse de(EnderecoEntrega e) {
            return new EnderecoEntregaResponse(e.getApelido(), e.getCep(), e.getLogradouro(), e.getNumero(),
                    e.getComplemento(), e.getBairro(), e.getCidade(), e.getUf());
        }
    }

    /** Responsável pela transição: o tipo e, quando há usuário autenticado, o id dele (nulo para o sistema). */
    public record ResponsavelResponse(TipoResponsavel tipo, Long usuarioId) {
    }

    /** Uma transição: {@code de} é nulo na criação do pedido. */
    public record EntradaHistoricoResponse(SituacaoPedido de, SituacaoPedido para, Instant ocorridoEm,
            ResponsavelResponse responsavel, String motivo) {

        static EntradaHistoricoResponse de(EntradaHistorico h) {
            return new EntradaHistoricoResponse(h.getDe(), h.getPara(), h.getOcorridoEm(),
                    new ResponsavelResponse(h.getResponsavel().tipo(), h.getResponsavel().usuarioId()),
                    h.getMotivo());
        }
    }

    public static PedidoResponse de(Pedido p) {
        return new PedidoResponse(p.getId(), p.getClienteId(), p.getSituacao(),
                p.getItens().stream().map(ItemResponse::de).toList(),
                p.getDescontos().stream().map(DescontoResponse::de).toList(), p.getFrete().valor(),
                p.getSubtotal().valor(), p.getTotalDescontos().valor(), p.getTotal().valor(),
                EnderecoEntregaResponse.de(p.getEntrega()), p.getCriadoEm(),
                p.getHistorico().stream().map(EntradaHistoricoResponse::de).toList(),
                EntregaSelecionadaResponse.de(p.getOpcaoEntrega()), p.getFreteLiquido().valor());
    }
}
