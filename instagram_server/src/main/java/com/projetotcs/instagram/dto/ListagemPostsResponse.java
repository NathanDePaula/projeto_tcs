package com.projetotcs.instagram.dto;

import java.util.List;

public record ListagemPostsResponse(
    String status,
    String codigo,
    String mensagem,
    List<PostSchema> posts
) {
}
