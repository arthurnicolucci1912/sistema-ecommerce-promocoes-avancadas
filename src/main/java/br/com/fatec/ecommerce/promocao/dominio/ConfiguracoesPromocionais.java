package br.com.fatec.ecommerce.promocao.dominio;

import br.com.fatec.ecommerce.compartilhado.dominio.Pagina;
import java.util.Optional;

/** Portas de configuracao; o saldo de utilizacoes nao faz parte da edicao da entidade versionada. */
public interface ConfiguracoesPromocionais {
    PromocaoConfig salvar(PromocaoConfig config);
    Cupom salvar(Cupom cupom);
    Optional<PromocaoConfig> promocao(Long id);
    Optional<Cupom> cupom(Long id);
    Cupom bloquearCupom(Long id);
    Pagina<PromocaoConfig> promocoes(int pagina, int tamanho);
    Pagina<Cupom> cupons(int pagina, int tamanho);
    int usosAtivos(Long cupomId);
    long maiorUsoIndividual(Long cupomId);
}
