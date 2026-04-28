package com.projetotcs.instagram.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SucessoResponse extends PadraoResposta {

    public SucessoResponse(String codigo, String mensagem) {
        super.setStatus("sucesso");
        super.setCodigo(codigo);
        super.setMensagem(mensagem);
    }
    
}
