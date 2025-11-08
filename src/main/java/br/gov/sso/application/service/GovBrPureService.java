package br.gov.sso.application.service;

import br.gov.sso.application.service.contract.IGovBrAuthService;
import br.gov.sso.domain.model.GovBrUser;
import br.gov.sso.infrastructure.util.PkceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Implementação pura (sem bibliotecas externas) do serviço de autenticação Gov.br.
 * Utiliza OAuth 2.0 com OpenID Connect e PKCE para autenticação segura.
 * Ativada quando a propriedade govbr.auth.type é definida como "pure".
 */
@Service
@ConditionalOnProperty(name = "govbr.auth.type", havingValue = "pure")
public class GovBrPureService implements IGovBrAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GovBrPureService.class);

    @Value("${govbr.url-provider}")
    private String urlProvider;

    @Value("${govbr.url-service}")
    private String urlService;

    @Value("${govbr.redirect-uri}")
    private String redirectUri;

    @Value("${govbr.scopes}")
    private String scopes;

    @Value("${govbr.client-id}")
    private String clientId;

    @Value("${govbr.client-secret}")
    private String clientSecret;

    @Value("${govbr.logout-uri}")
    private String logoutUri;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getLoginUrl(HttpSession session) throws Exception {
        // Gera parâmetros de segurança: state, nonce e PKCE
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        String codeVerifier = PkceUtil.generateCodeVerifier();
        String codeChallenge = PkceUtil.generateCodeChallenge(codeVerifier);

        // Armazena na sessão para validação posterior
        session.setAttribute("oauth_state", state);
        session.setAttribute("oauth_nonce", nonce);
        session.setAttribute("code_verifier", codeVerifier);

        // Constrói a URL de autorização com todos os parâmetros necessários
        String authorizeUrl = urlProvider + "/authorize?" +
                "response_type=code&" +
                "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) + "&" +
                "scope=" + URLEncoder.encode(scopes, StandardCharsets.UTF_8) + "&" +
                "redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) + "&" +
                "nonce=" + URLEncoder.encode(nonce, StandardCharsets.UTF_8) + "&" +
                "state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) + "&" +
                "code_challenge=" + URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8) + "&" +
                "code_challenge_method=S256";

        logger.info("Redirecting to authorize: {}", authorizeUrl);
        return authorizeUrl;
    }

    @Override
    public Object handleCallback(HttpServletRequest request, HttpSession session) throws Exception {
        String code = request.getParameter("code");
        String state = request.getParameter("state");
        String error = request.getParameter("error");
        String errorDescription = request.getParameter("error_description");

        // Verifica se houve erro no callback
        if (error != null) {
            Map<String, Object> errorResponse = Map.of(
                    "error", error,
                    "error_description", errorDescription,
                    "state", state
            );
            logger.error("OAuth error: {}", errorResponse);
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Valida o state para prevenir ataques CSRF
        String sessionState = (String) session.getAttribute("oauth_state");
        if (!state.equals(sessionState)) {
            throw new RuntimeException("Invalid state");
        }

        String codeVerifier = (String) session.getAttribute("code_verifier");

        // Troca o código de autorização por tokens de acesso
        String tokenUrl = urlProvider + "/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Codifica as credenciais em Base64 para o cabeçalho Authorization
        String clientIdAndSecret = clientId + ":" + clientSecret;
        String encodedClientIdAndSecret = Base64.getEncoder().encodeToString(clientIdAndSecret.getBytes(StandardCharsets.UTF_8));

        headers.set("Authorization", "Basic " + encodedClientIdAndSecret);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("code_verifier", codeVerifier);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        logger.info("Exchanging code for token: {}", body);
        ResponseEntity<String> tokenResponse = restTemplate.postForEntity(tokenUrl, entity, String.class);
        logger.info("Token response: {}", tokenResponse.getBody());

        // Extrai o access_token da resposta JSON
        JsonNode tokenJson = objectMapper.readTree(tokenResponse.getBody());
        String accessToken = tokenJson.get("access_token").asText();

        // Obtém informações do usuário usando o access_token
        String userInfoUrl = urlProvider + "/userinfo";
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> userEntity = new HttpEntity<>(userHeaders);
        ResponseEntity<String> userResponse = restTemplate.exchange(userInfoUrl, HttpMethod.GET, userEntity, String.class);
        logger.info("User info response: {}", userResponse.getBody());

        // Converte a resposta JSON para GovBrUser e armazena na sessão
        GovBrUser userInfo = objectMapper.readValue(userResponse.getBody(), GovBrUser.class);
        String userJson = objectMapper.writeValueAsString(userInfo);
        session.setAttribute("user", userJson);

        return "/";
    }

    @Override
    public String logout(HttpSession session) {
        // Invalida a sessão do usuário
        session.invalidate();
        return urlProvider + "/logout?post_logout_redirect_uri=" +
                URLEncoder.encode(logoutUri, StandardCharsets.UTF_8);
    }

    /**
     * Recupera os dados do usuário da sessão, lidando com problemas de classloader do devtools.
     *
     * @param session A sessão HTTP atual.
     * @return Os dados do usuário como um objeto GovBrUser, ou null se não houver dados do usuário na sessão.
     */
    @Override
    public GovBrUser getUser(HttpSession session) {
        // Recupera o objeto armazenado na sessão com a chave "user"
        Object obj = session.getAttribute("user");
        if (obj == null) {
            // Se não há dados na sessão, retorna null
            return null;
        }
        try {
            // Verifica se o objeto é uma String (JSON serializado)
            if (obj instanceof String) {
                // Desserializa a String JSON para GovBrUser
                return objectMapper.readValue((String) obj, GovBrUser.class);
            } else if (obj instanceof Map) {
                // Se for um Map (devido a problemas de classloader), converte para GovBrUser
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                return objectMapper.convertValue(map, GovBrUser.class);
            } else if (obj instanceof GovBrUser) {
                // Se já for um GovBrUser, retorna diretamente
                return (GovBrUser) obj;
            }
        } catch (Exception e) {
            // Loga o erro em caso de falha na desserialização
            logger.error("Error deserializing user from session", e);
        }
        // Retorna null se não conseguir recuperar os dados
        return null;
    }
}
