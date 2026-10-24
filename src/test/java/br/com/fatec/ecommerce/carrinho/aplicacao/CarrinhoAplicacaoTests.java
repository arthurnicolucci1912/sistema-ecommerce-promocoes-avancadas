package br.com.fatec.ecommerce.carrinho.aplicacao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.fatec.ecommerce.carrinho.dominio.*;
import br.com.fatec.ecommerce.catalogo.aplicacao.ConsultarItensVendaveis;
import br.com.fatec.ecommerce.catalogo.dominio.*;
import br.com.fatec.ecommerce.cliente.dominio.Cliente;
import br.com.fatec.ecommerce.cliente.dominio.ClienteRepository;
import br.com.fatec.ecommerce.cliente.dominio.Cpf;
import br.com.fatec.ecommerce.cliente.CpfsParaTeste;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import br.com.fatec.ecommerce.estoque.aplicacao.ConsultarSaldo;
import br.com.fatec.ecommerce.preco.aplicacao.PipelinePreco;
import br.com.fatec.ecommerce.preco.dominio.EtapaSubtotal;
import br.com.fatec.ecommerce.preco.dominio.EtapaTotal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CarrinhoAplicacaoTests {
    /** Novo tipo de item apenas de teste; os consumidores existentes nao precisam reconhecer sua classe. */
    static class PacoteEspecial extends ItemVendavel {
        PacoteEspecial() { super("PACOTE", "Pacote especial", Dinheiro.de("25")); }
        @Override public Long getId() { return 909L; }
        @Override public boolean estaDisponivelParaVenda() { return true; }
        @Override public List<ComponenteEstoque> componentesDeEstoque(int quantidade) {
            return List.of(new ComponenteEstoque(7L, quantidade * 3));
        }
    }

    @AfterEach
    void limparContexto() { SecurityContextHolder.clearContext(); }

    @Test
    void novoTipoPassaPeloCatalogoCarrinhoPrecoEConsultaDeEstoqueSemAlterarConsumidores() {
        var repositorioCatalogo = mock(ItemVendavelRepository.class);
        var categorias = mock(CategoriaRepository.class);
        var pacote = new PacoteEspecial();
        when(repositorioCatalogo.buscarPorSkus(anyCollection())).thenReturn(List.of(pacote));
        when(categorias.listarTodas()).thenReturn(List.of());
        var catalogo = new ConsultarItensVendaveis(repositorioCatalogo, categorias);
        var saldos = mock(ConsultarSaldo.class);
        when(saldos.porVariacoes(anyCollection())).thenReturn(List.of());
        Carrinho carrinho = new Carrinho(1L);
        var repositorio = mock(CarrinhoRepository.class);
        when(repositorio.buscarPorClienteId(any())).thenReturn(Optional.of(carrinho));
        var clientes = mock(ClienteRepository.class);
        when(clientes.buscarPorUsuarioId(1L)).thenReturn(Optional.of(new Cliente(1L, "Cliente",
                Cpf.de(CpfsParaTeste.valido(601)), null)));
        var aplicacao = new GerenciarCarrinho(repositorio, clientes, catalogo,
                new ConsultarDisponibilidadeDoCarrinho(saldos),
                new PipelinePreco(List.of(new EtapaSubtotal(), new EtapaTotal())));
        autenticar();
        var resultado = aplicacao.incluir("PACOTE", 2);
        assertThat(resultado.total()).isEqualByComparingTo("50");
        assertThat(resultado.itens()).hasSize(1);
        assertThat(resultado.itens().get(0).sku()).isEqualTo("PACOTE");
        assertThat(resultado.insuficiencias()).containsExactly(
                new ConsultarDisponibilidadeDoCarrinho.Insuficiencia(7L, 6L, 0));
        verify(repositorio).salvar(carrinho);
    }

    @Test
    void referenciaAusenteContinuaVisivelComAvisoEPermiteRemocao() {
        Carrinho carrinho = new Carrinho(1L);
        carrinho.incluir("ANTIGO", 4L, "Item anterior", Dinheiro.de("12.50"), 2);
        var repositorio = mock(CarrinhoRepository.class);
        when(repositorio.buscarPorClienteId(any())).thenReturn(Optional.of(carrinho));
        var clientes = mock(ClienteRepository.class);
        when(clientes.buscarPorUsuarioId(1L)).thenReturn(Optional.of(new Cliente(1L, "Cliente",
                Cpf.de(CpfsParaTeste.valido(602)), null)));
        var catalogo = mock(ConsultarItensVendaveis.class);
        when(catalogo.existentesPorSkus(anyCollection())).thenReturn(List.of());
        var saldos = mock(ConsultarSaldo.class);
        var aplicacao = new GerenciarCarrinho(repositorio, clientes, catalogo,
                new ConsultarDisponibilidadeDoCarrinho(saldos),
                new PipelinePreco(List.of(new EtapaSubtotal(), new EtapaTotal())));
        autenticar();
        var resultado = aplicacao.consultar();
        assertThat(resultado.itens().get(0).precoDeReferencia()).isTrue();
        assertThat(resultado.itens().get(0).disponivelParaVenda()).isFalse();
        assertThat(resultado.total()).isEqualByComparingTo("25");
        assertThat(aplicacao.remover("ANTIGO").itens()).isEmpty();
        verifyNoInteractions(saldos);
    }

    private static void autenticar() {
        Jwt jwt = Jwt.withTokenValue("teste").header("alg", "none").subject("1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
