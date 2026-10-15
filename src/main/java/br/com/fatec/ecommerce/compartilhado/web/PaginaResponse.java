package br.com.fatec.ecommerce.compartilhado.web;

import br.com.fatec.ecommerce.compartilhado.dominio.Pagina;
import java.util.List;
import java.util.function.Function;

/** Pagina de resultados: {@code page} comeca em 0 e {@code size} e o tamanho efetivo da pagina. */
public record PaginaResponse<T>(List<T> conteudo, int page, int size, long totalElementos, int totalPaginas) {

    public static <D, R> PaginaResponse<R> de(Pagina<D> pagina, Function<? super D, ? extends R> conversor) {
        return new PaginaResponse<>(pagina.conteudo().stream().<R>map(conversor).toList(), pagina.pagina(),
                pagina.tamanho(), pagina.totalElementos(), pagina.totalPaginas());
    }
}
