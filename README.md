# 🧠 NeuroTask

> **Seu assistente cognitivo e painel de produtividade inteligente.** Gerencie seu tempo, não apenas suas tarefas.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.7-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-ES6-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![LangChain4j](https://img.shields.io/badge/AI-LangChain4j-000000?style=for-the-badge)
![Google Gemini](https://img.shields.io/badge/Google_Gemini-1.5_Flash-4285F4?style=for-the-badge&logo=google&logoColor=white)

---

## 📖 Sobre o Projeto

O **NeuroTask** é uma plataforma avançada de gestão de tarefas com foco em *time-blocking* projetada para otimizar sua rotina cognitiva. Ao contrário dos gerenciadores convencionais que apenas listam o que precisa ser feito em infinitos *backlogs*, o NeuroTask integra o planejamento espacial do seu dia em um calendário interativo, ajudando você a alocar energia visualmente onde realmente importa e evitando conflitos de horários.

Nascido da necessidade de reduzir a sobrecarga mental e os entraves da procrastinação, o sistema não apenas organiza seus afazeres, mas age como um parceiro ativo e inteligente. Ele analisa suas métricas de desempenho, compreende os domínios de prioridade das tarefas e oferece feedbacks e _insights_ precisos por meio de uma inteligência artificial incorporada. É a solução definitiva para profissionais e estudantes que buscam sair do ciclo puramente reativo para um estado de foco produtivo profundo.

---

## ✨ Funcionalidades Principais

- 📅 **Calendário Interativo & Time-Blocking**: Motor robusto de _drag & drop_ que permite puxar as tarefas da fila de pendências diretamente para horários específicos no dia, contando com validação anti-conflito em tempo real.
- 🌓 **Interface Fluida (Light/Dark Mode)**: Design moderno e enxuto com suporte nativo a variação de temas, centrado numa refinada navegação por um **Dock lateral** (menu) que responde rapidamente às suas escolhas de tela.
- 🧠 **Neuro IA (Assistente Inteligente)**: O coração lógico do sistema na obtenção de insights de produtividade. O assistente de inteligência artificial analisa seus hábitos, gera _scores_ de saúde de planejamento e recomenda as próximas ações mais assertivas via chat ou estatística.
- 📊 **Dashboards e Analytics**: Análise profunda sobre performance do dia, distribuição de prioridades, status e eficiência em tempo médio de ciclo.

---

## 🏗️ Arquitetura e Integrações

O NeuroTask combina alta eficiência em uma separação clara de papéis arquiteturais utilizando os melhores padrões contemporâneos:
- **Frontend:** Desenvolvido nativamente em **Vanilla JavaScript (ES6 Modules)**, suportado por uma estrutura sólida em HTML5 e CSS flexível, garantindo fluidez e velocidade de renderização, sem o peso excessivo de frameworks SPAs de terceiros.
- **Backend:** Fornecido por uma API RESTful escalável modelada em **Spring Boot** (Java 21), orquestrando as manipulações de entidades transacionais junto com um banco de dados **MySQL**.

### 🔌 Integrações de API

A inteligência da aplicação é maximizada pela comunicação constante com serviços externos altamente escaláveis:
- **APIs de Inteligência Artificial (Google Gemini):** Através da infraestrutura do `LangChain4j`, a base de _analytics_ da aplicação é alicerçada no **Gemini 1.5 Flash**. Isso garante _parsing_ rápido de dados contextuais e processamento de linguagem natural com alta confiabilidade para gerar os diagnósticos de produtividade das jornadas do usuário.
- **APIs Públicas Transacionais (ex: BrasilAPI):** A plataforma faz também consumo de serviços abertos de contexto logístico e feriados nacionais, complementando de maneira integral dados cruciais do sistema para certificar entregas baseadas em restrições de rotinas locais ou datas úteis, reforçando os fundamentos estipulados para a entrega intermediária do projeto.

---

## ⚙️ Processo de Engenharia e Qualidade

Nosso desenvolvimento segue processos cuidadosos de qualidade e gestão de demanda, assegurando a robustez da evolução do software:
- 🎯 **Rastreamento via GitHub Issues:** Todas as tarefas de back-end, implementações visuais ou correções são modeladas, documentadas e rastreadas pelas _issues_ do repositório, garantindo transparência no histórico evolutivo.
- 🌿 **Estratégia de Branching Segura:** O versionamento do código-fonte gira centralmente ao redor do conceito de isolamento de *features*. Um grande exemplo prático é a estruturação e avanço via branch `entrega-intermediaria` para encapsular a evolução segura de novas integrações de API sem fragmentar a base operacional em produção.
- 🔀 **Pull Requests (PRs):** Garantia de revisão de código, qualidade semântica e rastreabilidade nas aprovações de *merge*.
- 🧪 **Testes de Integração:** Mecanismos de validação rigorosa que disparam execuções conjuntas, validando especialmente se as rotas da nossa REST API e fluxos de injeção externa (`Gemini` e públicas) se mantém intactos nas esteiras do desenvolvimento.

---

## 🚀 Como Executar (Setup Local)

Para inicializar a arquitetura e rodar a aplicação em sua máquina local para novos desenvolvimentos:

### Pré-requisitos
- JDK 21 instalado
- MySQL em execução local `(localhost:3306)`
- Chave de API ativada proveniente do Google AI Studio.

### Passos

1. **Clone o repositório na sua máquina:**
```bash
git clone https://github.com/seu-usuario/myday-productivity.git
cd myday-productivity
```

2. **Configure a Variável de Ambiente (API Key IA):**
A IA precisa da chave do Groq de modo seguro. Em seu terminal, execute o export de ambiente:

**Windows (CMD/PowerShell)**
```bash
set GROQ_API_KEY=sua_hash_do_groq_aqui
```
**Linux / MacOS**
```bash
export GROQ_API_KEY="sua_hash_do_groq_aqui"
```

3. **Inicie o serviço de Banco de Dados:**
A aplicação autoconfigurará as tabelas do _schema_ (como `neurotask`) mediante os dados cadastrados em `src/main/resources/application.properties` (usuário base root, ajuste caso possua senha local específica diferentemente de root).

4. **Inicie o servidor de dependências Spring via Maven Wrapper:**
Na raiz do projeto execute (ele baixará tudo sozinho):
```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

5. **Acesso à Aplicação:**
Uma vez que o banner de log for sinalizado ativo com sucesso pelo Netty, a interface do NeuroTask será montada estaticamente usando o *embedded server*. Abra seu navegador em:
```
http://localhost:8080
```

---

## 🌐 Deploy

Acompanhe as evoluções testáveis sob ambiente de produção estável no link hospedado.

🔗 https://neurotask-tthk.onrender.com


---
