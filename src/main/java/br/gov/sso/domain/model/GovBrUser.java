package br.gov.sso.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modelo de dados para representar um usuário autenticado via Gov.br.
 * Contém informações básicas do cidadão obtidas do endpoint /userinfo.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GovBrUser {

    /**
     * CPF do usuário autenticado.
     */
    private String sub;

    /**
     * Nome completo do usuário.
     */
    private String name;

    /**
     * URL do perfil do usuário no Gov.br.
     */
    private String profile;

    /**
     * URL da foto do usuário (protegida, requer access_token).
     */
    private String picture;

    /**
     * Endereço de e-mail do usuário.
     */
    private String email;

    /**
     * Indica se o e-mail foi verificado.
     */
    @JsonProperty("email_verified")
    private boolean emailVerified;

    /**
     * Número de telefone do usuário.
     */
    @JsonProperty("phone_number")
    private String phoneNumber;

    /**
     * Indica se o telefone foi verificado.
     */
    @JsonProperty("phone_number_verified")
    private boolean phoneNumberVerified;
}
