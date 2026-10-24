-- Adicao opcional: pedidos anteriores preservam valores e nao recebem modalidade inventada.
ALTER TABLE pedido
    ADD COLUMN opcao_entrega_codigo VARCHAR(128),
    ADD COLUMN opcao_entrega_fornecedor VARCHAR(80),
    ADD COLUMN opcao_entrega_modalidade VARCHAR(40),
    ADD COLUMN opcao_entrega_prazo_dias INTEGER,
    ADD CONSTRAINT ck_pedido_opcao_entrega CHECK (
        (opcao_entrega_codigo IS NULL AND opcao_entrega_fornecedor IS NULL
            AND opcao_entrega_modalidade IS NULL AND opcao_entrega_prazo_dias IS NULL)
        OR
        (opcao_entrega_codigo IS NOT NULL AND opcao_entrega_fornecedor IS NOT NULL
            AND opcao_entrega_modalidade IS NOT NULL AND opcao_entrega_prazo_dias IS NOT NULL
            AND length(trim(opcao_entrega_fornecedor)) > 0 AND length(trim(opcao_entrega_modalidade)) > 0
            AND opcao_entrega_codigo = opcao_entrega_fornecedor || ':' || opcao_entrega_modalidade
            AND opcao_entrega_prazo_dias > 0)
    );
