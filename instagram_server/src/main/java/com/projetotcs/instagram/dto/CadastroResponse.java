package com.projetotcs.instagram.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CadastroResponse extends PadraoResposta {

    public CadastroResponse() {
        super.setStatus("sucesso");
        super.setCodigo("USUARIO_CRIADO");
        super.setMensagem("Usuário cadastrado com sucesso");
    }
    
}
