# 🚀 Guia de Configuração do NeuroTask com Google Gemini AI

## 📋 Pré-requisitos

1. **Java 21** instalado
2. **PostgreSQL 16** rodando na porta 5432
3. **Google Gemini API Key** (grátis)

---

## 🔑 Obter API Key do Google Gemini

### Passo 1: Acessar o Google AI Studio

Acesse: **https://aistudio.google.com/app/apikey**

### Passo 2: Criar API Key

1. Clique em **"Create API Key"**
2. Selecione um projeto do Google Cloud (ou crie um novo)
3. Copie a API key gerada (formato: `AIzaSy...`)

### Passo 3: Configurar no Projeto

#### **Opção A: Variável de Ambiente (Recomendado)**

```cmd
# Windows (CMD)
set GEMINI_API_KEY=SuaAPIKeyAqui

# Windows (PowerShell)
$env:GEMINI_API_KEY="SuaAPIKeyAqui"

# Linux/Mac
export GEMINI_API_KEY=SuaAPIKeyAqui
```

#### **Opção B: application.properties (Desenvolvimento)**

Edite `src/main/resources/application.properties`:

```properties
gemini.api.key=SuaAPIKeyAqui
```

⚠️ **NUNCA commite a API key no Git!**

---

## 🗄️ Configurar Banco de Dados PostgreSQL

### Criar Database

```sql
CREATE DATABASE neurotask_db;
```

### Configurar Credenciais

Edite `application.properties` se necessário:

```properties
spring.datasource.username=postgres
spring.datasource.password=postgres
```

---

## ▶️ Executar o Projeto

### 1. Instalar Dependências

```cmd
cd myday-productivity
mvnw clean install
```

### 2. Iniciar Backend

```cmd
mvnw spring-boot:run
```

O servidor iniciará em: **http://localhost:8080**

### 3. Acessar Interface

Abra no navegador: **http://localhost:8080**

---

## 🤖 Testar Funcionalidades de IA

### 1. Criar algumas tarefas no Backlog

### 2. Clicar em "Analisar com IA"

- **📊 Produtividade**: Análise geral
- **🔍 Padrões**: Identifica comportamentos
- **💡 Dicas**: Recomendações personalizadas

### 3. Usar o Chat

Digite perguntas como:

- "Como posso melhorar minha produtividade?"
- "Tenho muitas tarefas atrasadas, o que fazer?"
- "Qual o melhor horário para tarefas de alta prioridade?"

---

## 🎯 Endpoints da API

### Tarefas

- `GET /api/tasks` - Listar todas
- `POST /api/tasks` - Criar tarefa
- `PATCH /api/tasks/{id}/move` - Mover para calendário
- `GET /api/tasks/stats` - Estatísticas

### IA (Requer API Key)

- `POST /api/ai/analyze` - Análise de produtividade
- `POST /api/ai/chat` - Chat com coach cognitivo

### Tags

- `GET /api/tags` - Listar tags
- `POST /api/tags` - Criar tag

---

## 🐛 Troubleshooting

### Erro: "API key não configurada"

✅ Verifique se a variável `GEMINI_API_KEY` está definida
✅ Reinicie o terminal após configurar a variável

### Erro: "Erro ao conectar com PostgreSQL"

✅ Verifique se o PostgreSQL está rodando
✅ Confirme que o database `neurotask_db` existe
✅ Verifique usuário/senha em `application.properties`

### Frontend não conecta com Backend

✅ Verifique se o backend está rodando na porta 8080
✅ Abra o console do navegador (F12) para ver erros

---

## 📊 Funcionalidades Implementadas

✅ **Time-Blocking Drag & Drop**: Arraste tarefas para o calendário  
✅ **IA com Google Gemini**: Análises cognitivas reais  
✅ **Chat Interativo**: Converse com o coach de produtividade  
✅ **Gráficos em Tempo Real**: Visualize sua produtividade  
✅ **Sistema de Tags**: Organize tarefas por contexto  
✅ **Estatísticas Avançadas**: Taxa de conclusão, padrões, etc.

---

## 🌟 Modelo Usado

**Gemini 1.5 Flash**

- Rápido e eficiente
- Até 1 milhão de tokens de contexto
- Gratuito até 15 requisições/minuto

Para produção, considere:

- **Gemini 1.5 Pro**: Análises mais profundas
- **Gemini 2.0 Flash**: Mais recente (experimental)

---

## 📝 Próximos Passos

1. Adicionar autenticação de usuários
2. Implementar notificações em tempo real
3. Criar relatórios PDF com IA
4. Integrar calendário Google/Outlook
5. App mobile React Native

---

**Desenvolvido com ❤️ usando Spring Boot 3.5.7 + Google Gemini AI + LangChain4j**
