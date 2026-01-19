# Guia de Publicação no Maven Central

Este guia ensina como publicar o J-Obs no Maven Central Repository para que outros desenvolvedores possam usar como dependência.

## Visão Geral do Processo

```
┌─────────────────────────────────────────────────────────────────┐
│                    Fluxo de Publicação                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Criar conta    2. Configurar     3. Assinar      4. Deploy  │
│     Sonatype          pom.xml           GPG            Maven    │
│        │                 │                │               │     │
│        ▼                 ▼                ▼               ▼     │
│  ┌──────────┐      ┌──────────┐     ┌──────────┐    ┌─────────┐│
│  │ Central  │ ───▶ │ Metadata │ ──▶ │  Sign    │ ─▶ │ Publish ││
│  │ Portal   │      │ + SCM    │     │ Artifacts│    │ Release ││
│  └──────────┘      └──────────┘     └──────────┘    └─────────┘│
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Passo 1: Criar Conta no Sonatype Central Portal

### 1.1 Registrar no Central Portal

1. Acesse: https://central.sonatype.com/
2. Clique em **"Sign In"** → **"Sign up"**
3. Pode usar conta GitHub, Google ou email

### 1.2 Verificar Namespace (GroupId)

O `groupId` precisa ser um domínio que você controla. Opções:

| Tipo | Exemplo | Verificação |
|------|---------|-------------|
| **GitHub** | `io.github.seu-usuario` | Automática via GitHub |
| **Domínio próprio** | `com.seusite` | TXT record no DNS |
| **GitLab** | `io.gitlab.seu-usuario` | Automática via GitLab |

#### Para usar `io.github.seu-usuario`:

1. No Central Portal, vá em **Namespaces**
2. Clique **"Add Namespace"**
3. Selecione **"GitHub"**
4. Autorize o acesso ao GitHub
5. O namespace `io.github.seu-usuario` será verificado automaticamente

#### Para domínio próprio:

1. Adicione um registro TXT no seu DNS:
   ```
   TXT  @  "sonatype-verification=seu-codigo-aqui"
   ```
2. Aguarde propagação (até 24h)
3. Clique em "Verify" no portal

---

## Passo 2: Gerar Chave GPG

Todos os artefatos precisam ser assinados com GPG.

### 2.1 Instalar GPG

**Windows (com Chocolatey):**
```bash
choco install gnupg
```

**Windows (download manual):**
- Baixe de: https://www.gnupg.org/download/

**macOS:**
```bash
brew install gnupg
```

**Linux:**
```bash
sudo apt install gnupg
```

### 2.2 Gerar Par de Chaves

```bash
gpg --full-generate-key
```

Escolha:
- Tipo: `RSA and RSA`
- Tamanho: `4096`
- Validade: `0` (não expira) ou `2y` (2 anos)
- Nome: `Seu Nome`
- Email: `seu-email@exemplo.com`
- Passphrase: **guarde com segurança!**

### 2.3 Listar e Exportar Chave

```bash
# Listar chaves
gpg --list-keys

# Output exemplo:
# pub   rsa4096 2024-01-15 [SC]
#       ABCD1234EFGH5678IJKL9012MNOP3456QRST7890
# uid           [ultimate] Seu Nome <seu-email@exemplo.com>
```

O ID da chave são os últimos 8 caracteres: `QRST7890`

### 2.4 Publicar Chave Pública

```bash
# Enviar para servidor de chaves
gpg --keyserver keyserver.ubuntu.com --send-keys QRST7890
gpg --keyserver keys.openpgp.org --send-keys QRST7890
```

### 2.5 Exportar Chave Privada (para CI/CD)

```bash
# Exportar em base64 para usar em secrets
gpg --export-secret-keys QRST7890 | base64 > private-key.txt
```

---

## Passo 3: Configurar o Projeto Maven

### 3.1 pom.xml Completo

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- ===== COORDENADAS (OBRIGATÓRIO) ===== -->
    <groupId>io.github.seu-usuario</groupId>
    <artifactId>j-obs-spring-boot-starter</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <!-- ===== METADADOS (OBRIGATÓRIO) ===== -->
    <name>J-Obs Spring Boot Starter</name>
    <description>
        Biblioteca de observabilidade para Spring Boot com dashboard
        integrado para logs, traces e métricas em tempo real.
    </description>
    <url>https://github.com/seu-usuario/j-obs</url>

    <!-- ===== LICENÇA (OBRIGATÓRIO) ===== -->
    <licenses>
        <license>
            <name>Apache License, Version 2.0</name>
            <url>https://www.apache.org/licenses/LICENSE-2.0</url>
            <distribution>repo</distribution>
        </license>
    </licenses>

    <!-- ===== DESENVOLVEDORES (OBRIGATÓRIO) ===== -->
    <developers>
        <developer>
            <id>seu-usuario</id>
            <name>Seu Nome Completo</name>
            <email>seu-email@exemplo.com</email>
            <url>https://github.com/seu-usuario</url>
            <roles>
                <role>developer</role>
                <role>maintainer</role>
            </roles>
            <timezone>America/Sao_Paulo</timezone>
        </developer>
    </developers>

    <!-- ===== SCM - SOURCE CONTROL (OBRIGATÓRIO) ===== -->
    <scm>
        <connection>scm:git:git://github.com/seu-usuario/j-obs.git</connection>
        <developerConnection>scm:git:ssh://github.com:seu-usuario/j-obs.git</developerConnection>
        <url>https://github.com/seu-usuario/j-obs</url>
        <tag>HEAD</tag>
    </scm>

    <!-- ===== ISSUE TRACKING (OPCIONAL MAS RECOMENDADO) ===== -->
    <issueManagement>
        <system>GitHub Issues</system>
        <url>https://github.com/seu-usuario/j-obs/issues</url>
    </issueManagement>

    <!-- ===== PROPRIEDADES ===== -->
    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Versões -->
        <spring-boot.version>3.2.0</spring-boot.version>
        <opentelemetry.version>1.32.0</opentelemetry.version>
    </properties>

    <!-- ===== DEPENDÊNCIAS ===== -->
    <dependencies>
        <!-- Suas dependências aqui -->
    </dependencies>

    <!-- ===== BUILD ===== -->
    <build>
        <plugins>
            <!-- Compiler -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>

            <!-- Source JAR (OBRIGATÓRIO para Central) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
                <version>3.3.0</version>
                <executions>
                    <execution>
                        <id>attach-sources</id>
                        <goals>
                            <goal>jar-no-fork</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <!-- Javadoc JAR (OBRIGATÓRIO para Central) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-javadoc-plugin</artifactId>
                <version>3.6.2</version>
                <executions>
                    <execution>
                        <id>attach-javadocs</id>
                        <goals>
                            <goal>jar</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <doclint>none</doclint>
                </configuration>
            </plugin>

            <!-- GPG Signing (OBRIGATÓRIO para Central) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-gpg-plugin</artifactId>
                <version>3.1.0</version>
                <executions>
                    <execution>
                        <id>sign-artifacts</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>sign</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <!-- Central Publishing Plugin (NOVO - 2024+) -->
            <plugin>
                <groupId>org.sonatype.central</groupId>
                <artifactId>central-publishing-maven-plugin</artifactId>
                <version>0.4.0</version>
                <extensions>true</extensions>
                <configuration>
                    <publishingServerId>central</publishingServerId>
                    <tokenAuth>true</tokenAuth>
                    <autoPublish>true</autoPublish>
                    <waitUntil>published</waitUntil>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <!-- ===== PROFILE PARA RELEASE ===== -->
    <profiles>
        <profile>
            <id>release</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-gpg-plugin</artifactId>
                        <executions>
                            <execution>
                                <id>sign-artifacts</id>
                                <phase>verify</phase>
                                <goals>
                                    <goal>sign</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
</project>
```

### 3.2 Configurar settings.xml

Crie/edite o arquivo `~/.m2/settings.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0
          https://maven.apache.org/xsd/settings-1.2.0.xsd">

    <servers>
        <!-- Token do Central Portal -->
        <server>
            <id>central</id>
            <username><!-- Token username do Central Portal --></username>
            <password><!-- Token password do Central Portal --></password>
        </server>
    </servers>

    <profiles>
        <profile>
            <id>gpg</id>
            <properties>
                <gpg.executable>gpg</gpg.executable>
                <gpg.keyname>QRST7890</gpg.keyname>
                <!-- Passphrase (ou deixe vazio para prompt interativo) -->
                <gpg.passphrase>sua-passphrase-aqui</gpg.passphrase>
            </properties>
        </profile>
    </profiles>

    <activeProfiles>
        <activeProfile>gpg</activeProfile>
    </activeProfiles>
</settings>
```

### 3.3 Gerar Token no Central Portal

1. Acesse https://central.sonatype.com/
2. Vá em **Account** → **Generate User Token**
3. Copie o `username` e `password` gerados
4. Cole no `settings.xml`

---

## Passo 4: Publicar

### 4.1 Build e Deploy Local (Teste)

```bash
# Limpar e compilar
mvn clean install

# Verificar se todos os JARs foram gerados
ls target/
# Deve ter:
# - j-obs-spring-boot-starter-1.0.0.jar
# - j-obs-spring-boot-starter-1.0.0-sources.jar
# - j-obs-spring-boot-starter-1.0.0-javadoc.jar
```

### 4.2 Deploy para Central

```bash
# Deploy com o novo Central Publishing Plugin
mvn clean deploy -Prelease
```

### 4.3 Verificar Publicação

1. Acesse https://central.sonatype.com/
2. Vá em **Deployments**
3. Você verá seu artefato em processamento
4. Após validação, clique em **Publish**
5. Em ~30 minutos estará disponível no Maven Central

---

## Passo 5: Configurar GitHub Actions (CI/CD)

### 5.1 Criar Secrets no GitHub

Vá em **Settings** → **Secrets** → **Actions** e adicione:

| Secret | Valor |
|--------|-------|
| `MAVEN_CENTRAL_USERNAME` | Token username do Central |
| `MAVEN_CENTRAL_PASSWORD` | Token password do Central |
| `GPG_PRIVATE_KEY` | Conteúdo do `private-key.txt` (base64) |
| `GPG_PASSPHRASE` | Passphrase da chave GPG |

### 5.2 Workflow de Release

Crie `.github/workflows/release.yml`:

```yaml
name: Release to Maven Central

on:
  release:
    types: [published]

jobs:
  publish:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Import GPG Key
        run: |
          echo "${{ secrets.GPG_PRIVATE_KEY }}" | base64 -d | gpg --batch --import
          gpg --list-secret-keys --keyid-format LONG

      - name: Configure Maven settings.xml
        run: |
          mkdir -p ~/.m2
          cat > ~/.m2/settings.xml << 'EOF'
          <settings>
            <servers>
              <server>
                <id>central</id>
                <username>${{ secrets.MAVEN_CENTRAL_USERNAME }}</username>
                <password>${{ secrets.MAVEN_CENTRAL_PASSWORD }}</password>
              </server>
            </servers>
            <profiles>
              <profile>
                <id>gpg</id>
                <properties>
                  <gpg.executable>gpg</gpg.executable>
                  <gpg.passphrase>${{ secrets.GPG_PASSPHRASE }}</gpg.passphrase>
                </properties>
              </profile>
            </profiles>
            <activeProfiles>
              <activeProfile>gpg</activeProfile>
            </activeProfiles>
          </settings>
          EOF

      - name: Publish to Maven Central
        run: mvn clean deploy -Prelease -DskipTests

      - name: Summary
        run: |
          echo "### Published to Maven Central! :rocket:" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "Artifact: \`io.github.seu-usuario:j-obs-spring-boot-starter:${{ github.event.release.tag_name }}\`" >> $GITHUB_STEP_SUMMARY
```

### 5.3 Como Fazer Release

```bash
# 1. Atualizar versão no pom.xml (remover -SNAPSHOT)
# 2. Commit e tag
git add pom.xml
git commit -m "Release 1.0.0"
git tag v1.0.0
git push origin main --tags

# 3. Criar release no GitHub
# Vá em Releases → Create new release → Selecione a tag
# O workflow será executado automaticamente
```

---

## Checklist de Publicação

```
□ Conta criada no Central Portal (central.sonatype.com)
□ Namespace verificado (io.github.seu-usuario)
□ Chave GPG gerada e publicada
□ pom.xml com todos os metadados obrigatórios:
  □ groupId, artifactId, version
  □ name, description, url
  □ licenses
  □ developers
  □ scm
□ Plugins configurados:
  □ maven-source-plugin
  □ maven-javadoc-plugin
  □ maven-gpg-plugin
  □ central-publishing-maven-plugin
□ settings.xml com credenciais
□ GitHub Secrets configurados (para CI/CD)
□ Workflow de release criado
```

---

## Estrutura de Artefatos Publicados

Após publicação, os seguintes arquivos estarão disponíveis:

```
io/github/seu-usuario/j-obs-spring-boot-starter/1.0.0/
├── j-obs-spring-boot-starter-1.0.0.pom
├── j-obs-spring-boot-starter-1.0.0.pom.asc        (assinatura GPG)
├── j-obs-spring-boot-starter-1.0.0.jar
├── j-obs-spring-boot-starter-1.0.0.jar.asc
├── j-obs-spring-boot-starter-1.0.0-sources.jar
├── j-obs-spring-boot-starter-1.0.0-sources.jar.asc
├── j-obs-spring-boot-starter-1.0.0-javadoc.jar
├── j-obs-spring-boot-starter-1.0.0-javadoc.jar.asc
└── checksums (md5, sha1, sha256, sha512)
```

---

## Troubleshooting

### Erro: "Invalid signature"
```bash
# Verificar se a chave foi publicada
gpg --keyserver keyserver.ubuntu.com --recv-keys QRST7890
```

### Erro: "Missing required metadata"
- Verifique se `name`, `description`, `url`, `licenses`, `developers`, `scm` estão no pom.xml

### Erro: "Invalid POM"
- Valide o XML: `mvn validate`
- Verifique encoding UTF-8

### Erro: "401 Unauthorized"
- Token expirado? Gere um novo no Central Portal
- Verifique se o `<id>central</id>` no settings.xml bate com o plugin

### Demora para aparecer no Maven Central
- Após "Published" no portal, aguarde ~30 minutos
- Search pode demorar até 4 horas para indexar

---

## Links Úteis

- **Central Portal**: https://central.sonatype.com/
- **Documentação Oficial**: https://central.sonatype.org/publish/publish-portal-maven/
- **Status do Maven Central**: https://status.maven.org/
- **Buscar Artefatos**: https://search.maven.org/
