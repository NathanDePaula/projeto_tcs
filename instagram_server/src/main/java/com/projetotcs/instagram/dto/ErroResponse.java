package com.projetotcs.instagram.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ErroResponse extends PadraoResposta {

    public ErroResponse(String codigo, String mensagem) {
        super.setStatus("erro");
        super.setCodigo(codigo);
        super.setMensagem(mensagem);
    }
    
}
