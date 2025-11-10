# SSO Gov.br - Spring Boot Implementation

## O que é SSO Gov.br?

O SSO Gov.br é o serviço de Single Sign-On (Autenticação Única) do governo brasileiro, que permite aos cidadãos se autenticarem em diversos sistemas governamentais usando suas credenciais digitais (como CPF, senha e certificados digitais). Ele segue os padrões OAuth 2.0 e OpenID Connect, garantindo segurança e interoperabilidade.

Este projeto implementa uma integração pura (sem bibliotecas externas) com o SSO Gov.br, utilizando PKCE (Proof Key for Code Exchange) para maior segurança.

## Tecnologias Utilizadas

- Java 17
- Spring Boot 3.5.7
- Maven
- Lombok
- HTTPS com certificado local para desenvolvimento

## Como Executar

### Pré-requisitos

- Java 17 instalado
- Maven instalado
- Certificado PKCS12 em `src/main/resources/keystore/local-certificate.p12` (já incluído para desenvolvimento local)

### Configuração

1. Clone o repositório:
   ```
   git clone <url-do-repositorio>
   cd springboot-sso-govbr
   ```

2. Configure as propriedades em `src/main/resources/application.properties`:
   - Ajuste as URLs do Gov.br (staging ou produção)
   - Configure client_id e client_secret fornecidos pelo Gov.br
   - Defina a redirect_uri para seu domínio

3. Execute a aplicação:
   ```
   mvn spring-boot:run
   ```

A aplicação rodará em `https://cadimpacto.localhost` (porta 443) com HTTPS.

## Endpoints Principais

- `GET /`: Redireciona para `/user`
- `GET /login`: Inicia o fluxo de login, redirecionando para Gov.br
- `GET /openid`: Callback do Gov.br após autenticação
- `GET /user`: Retorna informações do usuário logado (JSON)
- `GET /logout`: Faz logout e redireciona para Gov.br
- `GET /logout/govbr`: Callback de logout do Gov.br

## Estrutura do Projeto

- `application/service/`: Lógica de autenticação
- `domain/model/`: Modelos de dados (GovBrUser)
- `infrastructure/util/`: Utilitários (PKCE)
- `presentation/controller/`: Controladores REST

## Configurações Importantes

- `govbr.auth.type=pure`: Usa implementação pura (sem Spring Security OAuth2)
- Para produção, altere as URLs para `https://sso.acesso.gov.br` e `https://api.acesso.gov.br`
- Certifique-se de que o certificado SSL está configurado corretamente

## Desenvolvimento

Para desenvolvimento local, o certificado `local-certificate.p12` está incluído. Em produção, use um certificado válido.

Para mais detalhes sobre a API Gov.br, consulte a documentação oficial em https://manual-roteiro-integracao-login-unico.servicos.gov.br/
