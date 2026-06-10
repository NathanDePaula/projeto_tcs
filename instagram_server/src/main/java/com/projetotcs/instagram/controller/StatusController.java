package com.projetotcs.instagram.controller;

import com.projetotcs.instagram.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class StatusController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping(value = "/ativos", produces = "text/html;charset=UTF-8")
    public String showActiveUsers() {
        Map<String, String> activeUsers = usuarioService.getUsuariosAtivos();
        
        StringBuilder listItems = new StringBuilder();
        if (activeUsers.isEmpty()) {
            listItems.append("<li style='padding: 16px; text-align: center; color: #8e8e8e; font-style: italic;'>Nenhum usuário ativo no momento.</li>");
        } else {
            activeUsers.forEach((user, ip) -> {
                listItems.append("<li style='padding: 12px 18px; border-bottom: 1px solid #efefef; display: flex; justify-content: space-between; align-items: center;'>")
                    .append("<div style='display: flex; align-items: center; gap: 8px;'>")
                    .append("<span style='width: 8px; height: 8px; background-color: #00f018; border-radius: 50%; display: inline-block;'></span>")
                    .append("<strong style='color: #262626; font-size: 0.95rem;'>").append(user).append("</strong>")
                    .append("</div>")
                    .append("<span style='color: #8e8e8e; font-family: monospace; font-size: 0.85rem;'>IP: ").append(ip).append("</span>")
                    .append("</li>");
            });
        }

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>Painel do Servidor - Usuários Ativos</title>\n" +
                "    <meta http-equiv=\"refresh\" content=\"3\">\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif;\n" +
                "            background-color: #fafafa;\n" +
                "            color: #262626;\n" +
                "            padding: 40px;\n" +
                "            margin: 0;\n" +
                "            display: flex;\n" +
                "            justify-content: center;\n" +
                "            align-items: center;\n" +
                "            min-height: 100vh;\n" +
                "            box-sizing: border-box;\n" +
                "        }\n" +
                "        .card {\n" +
                "            background: white;\n" +
                "            border: 1px solid #dbdbdb;\n" +
                "            border-radius: 12px;\n" +
                "            width: 100%;\n" +
                "            max-width: 450px;\n" +
                "            padding: 30px;\n" +
                "            box-shadow: 0 8px 24px rgba(0,0,0,0.08);\n" +
                "            box-sizing: border-box;\n" +
                "        }\n" +
                "        h2 {\n" +
                "            margin-top: 0;\n" +
                "            margin-bottom: 8px;\n" +
                "            text-align: center;\n" +
                "            color: #262626;\n" +
                "            font-size: 1.6rem;\n" +
                "            font-weight: 700;\n" +
                "            letter-spacing: -0.5px;\n" +
                "        }\n" +
                "        .subtitle {\n" +
                "            font-size: 0.8rem;\n" +
                "            color: #8e8e8e;\n" +
                "            text-align: center;\n" +
                "            margin-bottom: 25px;\n" +
                "        }\n" +
                "        ul {\n" +
                "            list-style: none;\n" +
                "            padding: 0;\n" +
                "            margin: 0;\n" +
                "            border: 1px solid #dbdbdb;\n" +
                "            border-radius: 8px;\n" +
                "            overflow: hidden;\n" +
                "            background-color: #fff;\n" +
                "        }\n" +
                "        ul li:last-child {\n" +
                "            border-bottom: none;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"card\">\n" +
                "        <h2>Servidor Instagram</h2>\n" +
                "        <div class=\"subtitle\">Usuários ativos atualizados a cada 3s</div>\n" +
                "        <ul>\n" +
                "            " + listItems.toString() + "\n" +
                "        </ul>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}
