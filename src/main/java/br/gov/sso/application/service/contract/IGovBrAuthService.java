package br.gov.sso.application.service.contract;

import br.gov.sso.domain.model.GovBrUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Interface para serviços de autenticação Gov.br.
 * Define os métodos necessários para implementar diferentes estratégias de autenticação.
 */
public interface IGovBrAuthService {

    /**
     * Gera a URL de login para redirecionar o usuário ao provedor Gov.br.
     *
     * @param session Sessão HTTP do usuário para armazenar estado e nonce.
     * @return URL completa para o endpoint de autorização do Gov.br.
     * @throws Exception Se ocorrer erro na geração da URL ou PKCE.
     */
    String getLoginUrl(HttpSession session) throws Exception;

    /**
     * Processa o callback do provedor após a autenticação.
     * Troca o código de autorização por tokens e obtém informações do usuário.
     *
     * @param request Requisição HTTP contendo parâmetros do callback.
     * @param session Sessão HTTP do usuário.
     * @return ResponseEntity com erro ou String com URL de redirecionamento.
     * @throws Exception Se ocorrer erro no processamento.
     */
    Object handleCallback(HttpServletRequest request, HttpSession session) throws Exception;

    /**
     * Realiza o logout do usuário, invalidando a sessão.
     *
     * @param session Sessão HTTP do usuário a ser invalidada.
     */
    String logout(HttpSession session);

    /**
     * Recupera as informações do usuário logado da sessão.
     *
     * @param session Sessão HTTP do usuário.
     * @return GovBrUser com dados do usuário ou null se não logado.
     */
    GovBrUser getUser(HttpSession session);
}
