# 🔑 Como Configurar a API do Google Gemini

## Passo 1: Obter sua API Key

1. Acesse **[Google AI Studio](https://aistudio.google.com/app/apikey)**
2. Faça login com sua conta Google
3. Clique em **"Create API Key"** ou **"Get API Key"**
4. Copie a chave gerada (ela começa com `AIza...`)

## Passo 2: Configurar a Variável de Ambiente

### Windows (PowerShell)

```powershell
# Temporário (apenas para a sessão atual)
$env:GEMINI_API_KEY="AIzaSyA8LudXiWqgTEJ6sBWbPQDOTz5XShnpe_c"

# Permanente (recomendado)
[System.Environment]::SetEnvironmentVariable('GEMINI_API_KEY', 'AIzaSyA8LudXiWqgTEJ6sBWbPQDOTz5XShnpe_c', 'User')
```

### Windows (CMD)

```cmd
# Temporário
set GEMINI_API_KEY=AIza...SuaChaveAqui

# Permanente
setx GEMINI_API_KEY "AIza...SuaChaveAqui"
```

### Linux/Mac

```bash
# Temporário
export GEMINI_API_KEY="AIza...SuaChaveAqui"

# Permanente (adicione ao ~/.bashrc ou ~/.zshrc)
echo 'export GEMINI_API_KEY="AIza...SuaChaveAqui"' >> ~/.bashrc
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
2. No chat do Gemini, digite uma mensagem
3. Se a API key estiver correta, o Gemini responderá normalmente
4. Se não estiver configurada, você verá uma mensagem de erro com instruções

## ⚠️ Importante

- **Nunca compartilhe** sua API key publicamente
- **Não commit** a API key no Git
- A chave já está configurada para ser lida da variável de ambiente `GEMINI_API_KEY`
- É gratuito até um certo limite de requisições por mês

## 🆘 Problemas?

### Erro: "API key not valid"

- Verifique se você copiou a chave completa
- Gere uma nova chave no Google AI Studio
- Certifique-se de reiniciar a aplicação após configurar

### Erro: "GEMINI_API_KEY not found"

- A variável de ambiente não foi configurada
- Reinicie o terminal/PowerShell após configurar
- Use o comando `echo $env:GEMINI_API_KEY` (PowerShell) para verificar

### Chat não responde

- Verifique se o backend está rodando
- Veja os logs do servidor para mensagens de erro
- Tente recarregar a página (F5)
