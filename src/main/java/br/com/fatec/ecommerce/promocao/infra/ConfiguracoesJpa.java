package br.com.fatec.ecommerce.promocao.infra;

import br.com.fatec.ecommerce.promocao.dominio.*;
import br.com.fatec.ecommerce.compartilhado.dominio.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

interface CampanhaJpaRepository extends JpaRepository<PromocaoConfig,Long> {
    List<PromocaoConfig> findByAtivoTrueAndInicioLessThanEqualAndFimGreaterThan(Instant inicio,Instant fim);
}
interface CupomJpaRepository extends JpaRepository<Cupom,Long> { Optional<Cupom> findByCodigo(String codigo); }

/** Adaptador das portas de configuracao/leitura/uso. Atualizacoes de saldo participam da transacao chamadora. */
@Repository
public class ConfiguracoesJpa implements ConfiguracoesPromocionais, FonteDePromocoes, FonteDeCupons, UsoCupomRepository {
    private final CampanhaJpaRepository campanhas;
    private final CupomJpaRepository cupons;
    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final PromocaoFactoryRegistry fabricas;
    public ConfiguracoesJpa(CampanhaJpaRepository campanhas,CupomJpaRepository cupons,EntityManager em,
            JdbcTemplate jdbc,PromocaoFactoryRegistry fabricas) {
        this.campanhas=campanhas; this.cupons=cupons; this.em=em; this.jdbc=jdbc; this.fabricas=fabricas;
    }
    @Override public PromocaoConfig salvar(PromocaoConfig config) { return campanhas.saveAndFlush(config); }
    @Override public Cupom salvar(Cupom cupom) {
        try {
            Cupom salvo=cupons.saveAndFlush(cupom);
            jdbc.update("INSERT INTO saldo_cupom(cupom_id) VALUES (?) ON CONFLICT DO NOTHING",salvo.id()); return salvo;
        } catch (DataIntegrityViolationException erro) {
            if(erro.getMostSpecificCause().getMessage().contains("uk_cupom_codigo")) { throw new ConflitoException("Código de cupom já cadastrado."); }
            throw erro;
        }
    }
    @Override public Optional<PromocaoConfig> promocao(Long id) { return campanhas.findById(id); }
    @Override public Optional<Cupom> cupom(Long id) { return cupons.findById(id); }
    @Override public Cupom bloquearCupom(Long id) {
        Cupom cupom=cupons.findById(id).orElseThrow(()->new RecursoNaoEncontradoException("Cupom não encontrado."));
        em.refresh(cupom,LockModeType.PESSIMISTIC_WRITE); return cupom;
    }
    @Override public Pagina<PromocaoConfig> promocoes(int pagina,int tamanho) {
        var p=campanhas.findAll(PageRequest.of(pagina,tamanho,Sort.by("id")));
        return new Pagina<>(p.getContent(),pagina,tamanho,p.getTotalElements());
    }
    @Override public Pagina<Cupom> cupons(int pagina,int tamanho) {
        var p=cupons.findAll(PageRequest.of(pagina,tamanho,Sort.by("id")));
        return new Pagina<>(p.getContent(),pagina,tamanho,p.getTotalElements());
    }
    @Override public List<Promocao> vigentes(Instant momento) {
        return campanhas.findByAtivoTrueAndInicioLessThanEqualAndFimGreaterThan(momento,momento).stream().map(fabricas::criar).toList();
    }
    @Override public Optional<Disponivel> porCodigo(String codigo,Long clienteId) {
        return cupons.findByCodigo(codigo).map(c->new Disponivel(c,usosAtivos(c.id()),usosCliente(c.id(),clienteId)));
    }
    @Override public int usosAtivos(Long id) {
        return jdbc.queryForObject("SELECT usos_ativos FROM saldo_cupom WHERE cupom_id=?",Integer.class,id);
    }
    private long usosCliente(Long id,Long clienteId) {
        return jdbc.queryForObject("SELECT count(*) FROM uso_cupom WHERE cupom_id=? AND cliente_id=? AND situacao='CONSUMIDO'",Long.class,id,clienteId);
    }
    @Override public long maiorUsoIndividual(Long id) {
        return jdbc.queryForObject("SELECT coalesce(max(q),0) FROM (SELECT count(*) q FROM uso_cupom WHERE cupom_id=? AND situacao='CONSUMIDO' GROUP BY cliente_id) t",Long.class,id);
    }
    @Override public void consumir(Long cupomId,Long versao,Long pedidoId,Long clienteId,Instant momento) {
        Cupom c=bloquearCupom(cupomId);
        if(!Objects.equals(c.versao(),versao)) { throw new CupomRejeitadoException("CONFIGURACAO_ALTERADA"); }
        if(!c.ativo()) { throw new CupomRejeitadoException("INATIVO"); }
        if(!c.vigenteEm(momento)) { throw new CupomRejeitadoException("FORA_DA_VIGENCIA"); }
        if(jdbc.queryForObject("SELECT count(*) FROM uso_cupom WHERE pedido_id=?",Integer.class,pedidoId)>0) {
            throw new ConflitoException("O pedido já possui uso de cupom.");
        }
        if(usosAtivos(cupomId)>=c.limiteTotal()) { throw new CupomRejeitadoException("ESGOTADO"); }
        if(c.limiteCliente()!=null && usosCliente(cupomId,clienteId)>=c.limiteCliente()) { throw new CupomRejeitadoException("LIMITE_CLIENTE"); }
        jdbc.update("INSERT INTO uso_cupom(pedido_id,cliente_id,cupom_id,situacao,consumido_em) VALUES (?,?,?,'CONSUMIDO',?)",pedidoId,clienteId,cupomId,Timestamp.from(momento));
        jdbc.update("UPDATE saldo_cupom SET usos_ativos=usos_ativos+1 WHERE cupom_id=?",cupomId);
    }
    @Override public void devolver(Long pedidoId,Instant momento) {
        var ids=jdbc.queryForList("SELECT cupom_id FROM uso_cupom WHERE pedido_id=?",Long.class,pedidoId);
        if(ids.isEmpty()) { return; }
        Long cupomId=ids.getFirst(); bloquearCupom(cupomId);
        int alterados=jdbc.update("UPDATE uso_cupom SET situacao='DEVOLVIDO',devolvido_em=? WHERE pedido_id=? AND situacao='CONSUMIDO'",Timestamp.from(momento),pedidoId);
        if(alterados==1) { jdbc.update("UPDATE saldo_cupom SET usos_ativos=usos_ativos-1 WHERE cupom_id=?",cupomId); }
    }
}
