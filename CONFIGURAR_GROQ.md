# 🔑 Como Configurar a API da Groq

## Passo 1: Obter sua API Key

1. Acesse **[Groq Console](https://console.groq.com/keys)**
2. Faça login com sua conta
3. Clique em **"Create API Key"**
4. Copie a chave gerada (ela começa com `gsk_`)

## Passo 2: Configurar a Variável de Ambiente

### Windows (CMD)

```cmd
# Temporário
set OPENAI_API_KEY=gsk_SuaChaveAqui

# Permanente
setx OPENAI_API_KEY "gsk_SuaChaveAqui"
```

### Linux/Mac

```bash
# Temporário
export OPENAI_API_KEY="gsk_SuaChaveAqui"

# Permanente (adicione ao ~/.bashrc ou ~/.zshrc)
echo 'export OPENAI_API_KEY="gsk_SuaChaveAqui"' >> ~/.bashrc
source ~/.bashrc
```

## Passo 3: Reiniciar a Aplicação

Após configurar a variável de ambiente, **reinicie a aplicação**:

```cmd
# Pare a aplicação (Ctrl+C no terminal)
# Depois execute novamente:
.\mvnw.cmd spring-boot:run
```

## Verificar se está funcionando

1. Acesse http://localhost:8080
2. No chat da IA, digite uma mensagem
3. Se a API key estiver correta, a Groq responderá rapidamente
4. Se não estiver configurada, você verá uma mensagem de erro

## ⚠️ Importante

- **Nunca compartilhe** sua API key publicamente
- **Não commit** a API key no Git
- A chave já está configurada para ser lida da variável de ambiente `OPENAI_API_KEY` (usando compatibilidade com OpenAI API)

## 🆘 Problemas?

### Erro: "Invalid API Key"

- Verifique se você copiou a chave completa
- Gere uma nova chave no Groq Console
- Certifique-se de reiniciar a aplicação após configurar

### Erro: "OPENAI_API_KEY not found" ou chave inválida

- A variável de ambiente não foi configurada
- Reinicie o terminal/PowerShell após configurar
- Use o comando `echo $env:OPENAI_API_KEY` (PowerShell) para verificar

### Chat não responde

- Verifique se o backend está rodando
- Veja os logs do servidor para mensagens de erro
- Tente recarregar a página (F5)
