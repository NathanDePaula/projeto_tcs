package com.projetotcs.instagram.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "status", "codigo", "mensagem", "dados" })
public class PadraoResposta {
    private String status;
    private String codigo;
    private String mensagem;
    private Object dados;

    public PadraoResposta(String status, String codigo, String mensagem) {
        this.status = status;
        this.codigo = codigo;
        this.mensagem = mensagem;
    }
}
