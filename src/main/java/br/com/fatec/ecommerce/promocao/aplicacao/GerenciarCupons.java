package br.com.fatec.ecommerce.promocao.aplicacao;

import br.com.fatec.ecommerce.compartilhado.aplicacao.PublicarOperacaoAdministrativa;
import br.com.fatec.ecommerce.promocao.dominio.*;
import br.com.fatec.ecommerce.compartilhado.dominio.*;
import java.time.Clock;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Publica configuração/versão permitidas via Observer na transação original, sem credenciais. */
@Service @Transactional @PreAuthorize("hasAnyRole('ADMIN','PROMOCOES')")
public class GerenciarCupons {
    private final PublicarOperacaoAdministrativa auditoria;
    private final ConfiguracoesPromocionais repositorio;
    private final BeneficioCupomRegistry beneficios;
    private final CondicaoRegistry condicoes;
    private final ValidarConfiguracaoComercial validar;
    private final Clock clock;
    public GerenciarCupons(ConfiguracoesPromocionais repositorio,BeneficioCupomRegistry beneficios,
            CondicaoRegistry condicoes,ValidarConfiguracaoComercial validar,Clock clock, PublicarOperacaoAdministrativa auditoria) {
        this.auditoria = auditoria;
        this.repositorio=repositorio; this.beneficios=beneficios; this.condicoes=condicoes; this.validar=validar; this.clock=clock;
    }
    public CupomInfo criar(DadosCupom d) {
        var c=new Cupom(d.codigo(),d.nome(),d.tipo(),d.inicio(),d.fim(),d.acumulavel(),d.limiteTotal(),d.limiteCliente(),d.parametros(),d.condicao(),clock.instant());
        conferir(c); var salvo=info(repositorio.salvar(c));
        auditoria.publicar("CUPOM_CRIADO", "CUPOM", salvo.id(), null, auditoria.snapshot(salvo)); return salvo;
    }
    public CupomInfo alterar(Long id,DadosCupom d) {
        var c=repositorio.bloquearCupom(id); GerenciarPromocoes.versao(c.versao(),d.versao()); var antes=auditoria.snapshot(info(c));
        if(!c.codigo().equals(Cupom.normalizar(d.codigo()))) { throw Parametros.invalido("codigo imutável"); }
        if(d.limiteTotal()<repositorio.usosAtivos(id) || (d.limiteCliente()!=null && d.limiteCliente()<repositorio.maiorUsoIndividual(id))) {
            throw new ConflitoException("O limite não pode ser menor que o consumo ativo.");
        }
        c.alterar(d.nome(),d.tipo(),d.inicio(),d.fim(),d.acumulavel(),d.limiteTotal(),d.limiteCliente(),d.parametros(),d.condicao());
        conferir(c); var salvo=info(repositorio.salvar(c));
        auditoria.publicar("CUPOM_ALTERADO", "CUPOM", id, antes, auditoria.snapshot(salvo)); return salvo;
    }
    public CupomInfo ativo(Long id,Long versao,boolean ativo) {
        var c=repositorio.bloquearCupom(id); GerenciarPromocoes.versao(c.versao(),versao); var antes=auditoria.snapshot(info(c)); c.definirAtivo(ativo); var salvo=info(repositorio.salvar(c));
        auditoria.publicar("CUPOM_ATIVACAO_ALTERADA", "CUPOM", id, antes, auditoria.snapshot(salvo)); return salvo;
    }
    @Transactional(readOnly=true) public CupomInfo consultar(Long id) {
        return info(repositorio.cupom(id).orElseThrow(()->new RecursoNaoEncontradoException("Cupom não encontrado.")));
    }
    @Transactional(readOnly=true) public Pagina<CupomInfo> listar(int page,int size) {
        ValidarConfiguracaoComercial.pagina(page,size); var p=repositorio.cupons(page,size);
        return new Pagina<>(p.conteudo().stream().map(this::info).toList(),page,size,p.totalElementos());
    }
    public Map<String,List<String>> tipos() { return beneficios.tipos(); }
    public Map<String,List<String>> condicoes() { return condicoes.tipos(); }
    private CupomInfo info(Cupom c) { return CupomInfo.de(c,repositorio.usosAtivos(c.id())); }
    private void conferir(Cupom c) { beneficios.criar(c); condicoes.criar(c.condicao()); validar.referencias(c.parametros(),c.condicao()); }
}
