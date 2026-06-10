package com.projetotcs.instagram.dto;

public record PostSchema(
    String id,
    String conteudo,
    String imagem,
    String curtidas,
    Boolean curtido
) {
}
