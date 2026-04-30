# Projeto Mock Instagram

Aplicação web e servidor mock do Instagram para estudo de comunicação RESTful e HTTP.

## Arquitetura

O projeto utiliza comunicação baseada em contrato definida no arquivo `instagram-api.json` (Swagger).

### Componentes
- **Cliente**: Frontend web (HTML/CSS) + backend em JavaScript. Envia requisições para o servidor.
- **Servidor**: Aplicação Java Spring Boot. Gerencia a lógica e processa as requisições.
- **Banco de Dados**: Banco SQL via XAMPP.

### Segurança
- **Spring Security**: Gerencia acesso e autenticação.
- **JWT**: Gerenciamento de tokens para autenticação stateless.
- **Blacklist**: Logout invalida tokens via armazenamento de JTI no banco de dados. Tokens expirados são removidos a cada 10 min.

## Tecnologias
- **Frontend**: HTML5, CSS3, JavaScript (Vanilla).
- **Backend**: Java 21, Spring Boot, Spring Security, JWT (auth0).
- **Banco de Dados**: MySQL.
- **Documentação**: Swagger / OpenAPI.

---

# Mock Instagram Project

Mock Instagram web application and server for studying RESTful and HTTP communication.

## Architecture

Project use contract-first communication based on `instagram-api.json` (Swagger).

### Components
- **Client**: Web frontend (HTML/CSS) + JavaScript backend. Send requests to server.
- **Server**: Java Spring Boot application. Handle requests, manage logic.
- **Database**: SQL database via XAMPP.

### Security
- **Spring Security**: Manage access and authentication.
- **JWT**: Token management for stateless auth.
- **Blacklist**: Logout invalidate tokens via JTI storage in database. Expired tokens purged every 10 min.

## Tech Stack
- **Frontend**: HTML5, CSS3, JavaScript (Vanilla).
- **Backend**: Java 21, Spring Boot, Spring Security, JWT (auth0).
- **Database**: MySQL.
- **Documentation**: Swagger / OpenAPI.
