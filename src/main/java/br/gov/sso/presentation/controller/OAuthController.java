package br.gov.sso.presentation.controller;

import br.gov.sso.application.service.contract.IGovBrAuthService;
import br.gov.sso.domain.model.GovBrUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

/**
 * Controlador responsável por gerenciar as rotas de autenticação OAuth 2.0 com Gov.br.
 * Utiliza um serviço de autenticação injetado para processar login, callback, logout e recuperação de usuário.
 */
@Controller
public class OAuthController {

    private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);

    private final IGovBrAuthService authService;

    /**
     * Construtor que injeta o serviço de autenticação.
     *
     * @param authService Serviço de autenticação Gov.br (pure ou lib).
     */
    public OAuthController(IGovBrAuthService authService) {
        this.authService = authService;
    }

    /**
     * Redireciona para a página de informações do usuário.
     *
     * @return RedirectView para /user.
     */
    @GetMapping("/")
    public RedirectView index() {
        return new RedirectView("/user");
    }

    /**
     * Retorna as informações do usuário logado ou uma mensagem de erro se não logado.
     *
     * @param session Sessão HTTP do usuário.
     * @return ResponseEntity com dados do usuário ou erro.
     */
    @GetMapping("/user")
    public ResponseEntity<Object> user(HttpSession session) {
        GovBrUser user = authService.getUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of(
                            "error", "Usuário não logado",
                            "code", 401
                    )
            );
        }
        return ResponseEntity.ok(user);
    }

    /**
     * Inicia o processo de login, redirecionando para a URL de autorização do Gov.br.
     *
     * @param session Sessão HTTP do usuário.
     * @return RedirectView para a URL de login.
     * @throws Exception Se ocorrer erro na geração da URL.
     */
    @GetMapping("/login")
    public RedirectView login(HttpSession session) throws Exception {
        String loginUrl = authService.getLoginUrl(session);
        return new RedirectView(loginUrl);
    }

    /**
     * Processa o callback do Gov.br após a autenticação.
     * Pode retornar um erro ou redirecionar para a página inicial.
     *
     * @param request Requisição HTTP com parâmetros do callback.
     * @param response Resposta HTTP.
     * @param session Sessão HTTP do usuário.
     * @return ResponseEntity com erro ou RedirectView.
     * @throws Exception Se ocorrer erro no processamento.
     */
    @GetMapping("/openid")
    public Object callback(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
        Object result = authService.handleCallback(request, session);
        if (result instanceof ResponseEntity) {
            return result;
        } else if (result instanceof String) {
            return new RedirectView((String) result);
        }
        return new RedirectView("/");
    }

    /**
     * Realiza o logout da sessão do usuário e redireciona para a URL de logout do Gov.br.
     *
     * @param session Sessão HTTP do usuário.
     * @return RedirectView para a URL de logout.
     */
    @GetMapping("/logout")
    public RedirectView logout(HttpSession session) {
        String logoutUrl = authService.logout(session);
        return new RedirectView(logoutUrl);
    }

    /**
     * Realiza o logout do usuário e redireciona para a página inicial.
     *
     * @param session Sessão HTTP do usuário.
     * @return RedirectView para /.
     */
    @GetMapping("/logout/govbr")
    public RedirectView logoutGovBrCallback(HttpSession session) {
        return new RedirectView("/");
    }
}
