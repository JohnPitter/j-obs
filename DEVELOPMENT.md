# Guia de Desenvolvimento Local

Este guia explica como desenvolver e testar o J-Obs localmente antes de publicar no Maven Central.

---

## Estrutura de Desenvolvimento

```
seu-workspace/
├── j-obs/                      # Biblioteca (este projeto)
│   ├── j-obs-core/
│   ├── j-obs-spring-boot-starter/
│   └── pom.xml
│
└── minha-api-teste/            # Projeto Spring Boot para testar
    ├── src/
    └── pom.xml
```

---

## Passo 1: Configurar Versão SNAPSHOT

No `pom.xml` do J-Obs, use versão SNAPSHOT durante desenvolvimento:

```xml
<groupId>io.github.seu-usuario</groupId>
<artifactId>j-obs-spring-boot-starter</artifactId>
<version>1.0.0-SNAPSHOT</version>
```

> **Por que SNAPSHOT?** O Maven sempre busca a versão mais recente de SNAPSHOTs, então você não precisa mudar a versão a cada alteração.

---

## Passo 2: Instalar no Repositório Local

Após qualquer alteração no J-Obs:

```bash
cd j-obs
mvn clean install
```

O artefato será instalado em:
```
~/.m2/repository/io/github/seu-usuario/j-obs-spring-boot-starter/1.0.0-SNAPSHOT/
```

### Verificar Instalação

```bash
# Listar arquivos instalados
ls ~/.m2/repository/io/github/seu-usuario/j-obs-spring-boot-starter/1.0.0-SNAPSHOT/

# Deve mostrar:
# j-obs-spring-boot-starter-1.0.0-SNAPSHOT.jar
# j-obs-spring-boot-starter-1.0.0-SNAPSHOT.pom
# maven-metadata-local.xml
```

---

## Passo 3: Usar em Projeto de Teste

### 3.1 Criar Projeto Spring Boot de Teste

```bash
# Via Spring Initializr CLI
curl https://start.spring.io/starter.zip \
  -d dependencies=web,actuator \
  -d name=j-obs-test \
  -d packageName=com.example.jobstest \
  -o j-obs-test.zip

unzip j-obs-test.zip -d minha-api-teste
```

Ou crie pelo https://start.spring.io/

### 3.2 Adicionar Dependência do J-Obs

No `pom.xml` do projeto de teste:

```xml
<dependencies>
    <!-- Dependências do Spring -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- J-Obs (do repositório local) -->
    <dependency>
        <groupId>io.github.seu-usuario</groupId>
        <artifactId>j-obs-spring-boot-starter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### 3.3 Rodar e Testar

```bash
cd minha-api-teste
mvn spring-boot:run
```

Acesse:
- API: http://localhost:8080
- J-Obs Dashboard: http://localhost:8080/j-obs

---

## Fluxo de Desenvolvimento

```
┌─────────────────────────────────────────────────────────────┐
│                   Ciclo de Desenvolvimento                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   ┌──────────┐     ┌──────────┐     ┌──────────────────┐    │
│   │  Editar  │ ──▶ │ Install  │ ──▶ │ Testar na API    │    │
│   │  J-Obs   │     │  Local   │     │ Spring Boot      │    │
│   └──────────┘     └──────────┘     └──────────────────┘    │
│        ▲                                     │               │
│        │                                     │               │
│        └─────────── Repetir ─────────────────┘               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Comandos Rápidos

```bash
# Terminal 1: J-Obs (reinstalar após mudanças)
cd j-obs
mvn clean install -DskipTests   # Pular testes para ser mais rápido

# Terminal 2: API de teste (reiniciar para pegar mudanças)
cd minha-api-teste
mvn spring-boot:run
```

---

## Desenvolvimento com Hot Reload

Para não precisar reiniciar a API a cada mudança:

### Opção 1: Spring DevTools

Adicione no projeto de teste:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### Opção 2: JRebel (pago)

Permite hot reload de classes sem reiniciar.

### Opção 3: Script de Watch

Crie `dev.sh` para automatizar:

```bash
#!/bin/bash

# Instalar J-Obs e reiniciar API automaticamente
cd ../j-obs && mvn clean install -DskipTests && cd ../minha-api-teste && mvn spring-boot:run
```

---

## Testes Automatizados

### Rodar Testes do J-Obs

```bash
cd j-obs

# Todos os testes
mvn test

# Testes específicos
mvn test -Dtest=DependencyCheckerTest

# Com coverage
mvn test jacoco:report
# Relatório em: target/site/jacoco/index.html
```

### Testes de Integração

```bash
# Rodar testes de integração
mvn verify

# Ou apenas integration tests
mvn failsafe:integration-test
```

---

## Debug

### Debug do J-Obs na API de Teste

1. No IntelliJ/VS Code, adicione breakpoints no código do J-Obs
2. Rode a API de teste em modo debug:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

3. Conecte o debugger na porta 5005

### Logs Detalhados

No `application.yml` do projeto de teste:

```yaml
logging:
  level:
    io.github.jobs: DEBUG
    org.springframework: INFO

# Ver queries SQL
spring:
  jpa:
    show-sql: true
```

---

## Projeto de Teste Exemplo

### Estrutura Mínima

```
minha-api-teste/
├── src/main/java/com/example/
│   ├── Application.java
│   └── controller/
│       └── TestController.java
├── src/main/resources/
│   └── application.yml
└── pom.xml
```

### Application.java

```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### TestController.java

```java
package com.example.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    @GetMapping("/hello")
    public String hello() {
        log.info("Requisição recebida em /api/hello");
        return "Hello, J-Obs!";
    }

    @GetMapping("/slow")
    public String slow() throws InterruptedException {
        log.warn("Iniciando operação lenta...");
        Thread.sleep(2000); // Simula latência
        log.info("Operação lenta concluída");
        return "Slow response";
    }

    @GetMapping("/error")
    public String error() {
        log.error("Simulando erro!");
        throw new RuntimeException("Erro simulado para testar logs");
    }

    @GetMapping("/trace")
    public String trace() {
        log.info("Passo 1: Iniciando trace");
        log.info("Passo 2: Processando dados");
        log.info("Passo 3: Finalizando trace");
        return "Trace completo";
    }
}
```

### application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: j-obs-test

# Configuração do J-Obs
j-obs:
  enabled: true
  path: /j-obs

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always

# Logs
logging:
  level:
    com.example: DEBUG
    io.github.jobs: DEBUG
```

### pom.xml Completo

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>j-obs-test</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- Observabilidade (requisitos do J-Obs) -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry.instrumentation</groupId>
            <artifactId>opentelemetry-spring-boot-starter</artifactId>
            <version>2.1.0-alpha</version>
        </dependency>

        <!-- J-Obs (repositório local) -->
        <dependency>
            <groupId>io.github.seu-usuario</groupId>
            <artifactId>j-obs-spring-boot-starter</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

        <!-- Testes -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Endpoints para Testar

Após rodar a API de teste, acesse:

| URL | Descrição |
|-----|-----------|
| http://localhost:8080/api/hello | Request simples |
| http://localhost:8080/api/slow | Request com latência (2s) |
| http://localhost:8080/api/error | Gera erro para testar logs |
| http://localhost:8080/api/trace | Múltiplos logs para testar trace |
| http://localhost:8080/j-obs | Dashboard do J-Obs |
| http://localhost:8080/actuator/health | Health check |
| http://localhost:8080/actuator/prometheus | Métricas Prometheus |

---

## Troubleshooting

### Dependência não encontrada

```
Could not find artifact io.github.seu-usuario:j-obs-spring-boot-starter:1.0.0-SNAPSHOT
```

**Solução:** Rodar `mvn clean install` no projeto J-Obs primeiro.

### Mudanças não refletem

**Solução:**
1. Reinstalar: `mvn clean install` no J-Obs
2. Reiniciar a API de teste
3. Limpar cache do Maven: `mvn dependency:purge-local-repository`

### Conflito de versões

```bash
# Ver árvore de dependências
mvn dependency:tree

# Verificar conflitos
mvn dependency:analyze
```

### Forçar atualização de SNAPSHOT

```bash
mvn clean install -U
```

O `-U` força o Maven a verificar atualizações de SNAPSHOTs.
