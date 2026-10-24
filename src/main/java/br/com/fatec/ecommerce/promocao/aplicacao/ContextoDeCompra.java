package br.com.fatec.ecommerce.promocao.aplicacao;

import br.com.fatec.ecommerce.promocao.dominio.*;
import br.com.fatec.ecommerce.pedido.dominio.PedidoRepository;
import br.com.fatec.ecommerce.cliente.dominio.Endereco;
import br.com.fatec.ecommerce.cliente.dominio.EnderecoRepository;
import br.com.fatec.ecommerce.compartilhado.dominio.Dinheiro;
import java.time.Clock;
import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Contexto real: historico, endereco e autoridades do servidor, nunca flags enviadas pelo consumidor. */
@Service
public class ContextoDeCompra {
    private final PedidoRepository pedidos;
    private final EnderecoRepository enderecos;
    private final Clock clock;
    public ContextoDeCompra(PedidoRepository pedidos,EnderecoRepository enderecos,Clock clock) {
        this.pedidos=pedidos; this.enderecos=enderecos; this.clock=clock;
    }
    public String ufPrincipal(Long clienteId) {
        return enderecos.listarAtivos(clienteId).stream().filter(Endereco::isPrincipal).findFirst().map(Endereco::getUf).orElse(null);
    }
    public ContextoPreco criar(List<ItemPreco> itens,Long clienteId,String uf,Dinheiro subtotal,String cupom) {
        return criar(itens,clienteId,uf,subtotal,cupom,Dinheiro.zero());
    }
    public ContextoPreco criar(List<ItemPreco> itens,Long clienteId,String uf,Dinheiro subtotal,String cupom,Dinheiro freteBruto) {
        var momento=clock.instant();
        var auth=SecurityContextHolder.getContext().getAuthentication();
        Set<String> perfis=auth==null ? Set.of() : auth.getAuthorities().stream().map(a->a.getAuthority())
                .filter(a->a.startsWith("ROLE_")).map(a->a.substring(5)).collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new ContextoPreco(itens,clienteId,!pedidos.existePagamentoAprovadoAte(clienteId,momento),uf,
                subtotal,momento,cupom,perfis,freteBruto);
    }
}
