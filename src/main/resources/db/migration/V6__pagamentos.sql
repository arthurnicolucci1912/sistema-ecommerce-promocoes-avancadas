-- IDs entre modulos, sem associacoes JPA. Nao fabricar cobrancas para pedidos legados.
CREATE TABLE cobranca (
    pedido_id BIGINT PRIMARY KEY CHECK (pedido_id > 0),
    fornecedor VARCHAR(80) NOT NULL CHECK (length(trim(fornecedor)) > 0),
    forma VARCHAR(30) NOT NULL CHECK (forma IN ('CARTAO_CREDITO','PIX','BOLETO')),
    total NUMERIC(12,2) NOT NULL CHECK (total >= 0),
    id_transacao_externa VARCHAR(160) NOT NULL UNIQUE CHECK (length(trim(id_transacao_externa)) > 0),
    situacao VARCHAR(20) NOT NULL CHECK (situacao IN ('PENDENTE','APROVADO','RECUSADO')),
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL CHECK (atualizado_em >= criado_em)
);
CREATE TABLE confirmacao_pagamento (
    id_transacao_externa VARCHAR(160) PRIMARY KEY REFERENCES cobranca(id_transacao_externa),
    resultado VARCHAR(20) NOT NULL CHECK (resultado IN ('APROVADO','RECUSADO')),
    recebido_em TIMESTAMPTZ NOT NULL
);
CREATE TABLE estorno (
    pedido_id BIGINT PRIMARY KEY CHECK (pedido_id > 0),
    id_cobranca VARCHAR(160) UNIQUE REFERENCES cobranca(id_transacao_externa),
    total NUMERIC(12,2) NOT NULL CHECK (total >= 0),
    situacao VARCHAR(20) NOT NULL CHECK (situacao IN ('PENDENTE','FALHOU','CONCLUIDO')),
    id_transacao_externa VARCHAR(160) UNIQUE,
    tentativas INTEGER NOT NULL CHECK (tentativas >= 0),
    ultimo_erro VARCHAR(500),
    atualizado_em TIMESTAMPTZ NOT NULL,
    CHECK (situacao <> 'CONCLUIDO' OR (id_cobranca IS NOT NULL AND id_transacao_externa IS NOT NULL
        AND length(trim(id_transacao_externa)) > 0 AND tentativas > 0 AND ultimo_erro IS NULL)),
    CHECK (situacao <> 'FALHOU' OR (ultimo_erro IS NOT NULL AND length(trim(ultimo_erro)) > 0))
);
