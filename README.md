# GigU Clone (Motorista Copiloto)

Projeto em **Kotlin** e **Jetpack Compose** reproduzindo de forma limpa, eficiente e autoral as funcionalidades centrais do aplicativo **GigU** (`co.gigu.app`), voltado para motoristas de aplicativos (Uber e 99).

---

## 🚀 Funcionalidades Principais Implementadas

1. **Semáforo Inteligente de Rentabilidade:**
   - Cálculo instantâneo do custo por km rodado com base no consumo do veículo e preço do combustível.
   - Apuração do Lucro Líquido Real (descontando despesas operacionais da tarifa bruta).
   - Classificação instantânea em **Verde** (Excelente), **Amarelo** (Mediano/Alerta) e **Vermelho** (Prejuízo/Recusar).

2. **Serviço de Acessibilidade (`AccessibilityService`):**
   - Monitoramento passivo dos eventos de tela dos aplicativos oficiais da Uber (`com.ubercab.driver`) e 99 (`com.taxis99`).
   - Leitura de nós de texto na árvore de UI (`AccessibilityNodeInfo`) para extração automática de valor da chamada (`R$`), distâncias até embarque/destino (`km`) e tempos estimados (`min`).

3. **Janela Flutuante de Sobreposição (`SYSTEM_ALERT_WINDOW`):**
   - Cartão flutuante dinâmico exibido sobre a tela da Uber/99 no momento em que a chamada toca.
   - Suporte a arrastar pela tela (drag & drop) e fechamento manual ou automático.

4. **Notificações em Áudio (`TextToSpeech`):**
   - Leitura em português (pt-BR) dos dados vitais da corrida para que o motorista decida sem tirar os olhos do trânsito.

5. **Simulador Integrado:**
   - Permite testar o comportamento do semáforo e das métricas sem precisar de uma corrida real ativa.

---

## 🛠️ Como Executar e Compilar o Projeto

### Requisitos:
- **Android Studio Iguana | Jellyfish ou superior**
- **JDK 17**
- **Dispositivo Físico Android ou Emulador com Google Play Services (Android 8.0 - API 26+)**

### Passos de Execução:
1. Abra este diretório no **Android Studio**.
2. Aguarde a sincronização do Gradle (`Sync Project with Gradle Files`).
3. Conecte seu dispositivo Android com a Depuração USB ativada.
4. Execute o app (`Run 'app'`).
5. No app, conceda:
   - Permissão de **Sobreposição de Tela** (Janela Flutuante).
   - Ativação do **Serviço de Acessibilidade** nas configurações de Acessibilidade do Android.

---

## 🔍 Engenharia Reversa e REA (JADX)

Para análise adicional de bytecode caso deseje inspecionar diretamente uma versão compilada:
- O relatório técnico completo está documentado em [`ANALISE_GIGU.md`](file:///c:/Users/alecs/Desktop/EMPRESAS/2%20-%20AUZA%20NEURAL/1%20-%20PROJETOS/GIGU/ANALISE_GIGU.md).
- Para ativar o provedor JADX no REA:
  1. Baixe o executável/jar do JADX (`jadx-headless-mcp-0.7.1.jar`).
  2. Defina a variável de ambiente: `set REA_JADX_MCP_JAR=C:\caminho\para\jadx-headless-mcp-0.7.1.jar`.
  3. Execute `rea doctor --provider jadx --json` para confirmar a prontidão.

