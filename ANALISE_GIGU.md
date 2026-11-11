# Relatório Técnico de Engenharia Reversa e Arquitetura: GigU (co.gigu.app)

## 1. Visão Geral e Propósito do Aplicativo
O **GigU** (`co.gigu.app`) atua como um copiloto em segundo plano para motoristas e entregadores de plataformas de transporte por aplicativo (notadamente **Uber Driver**, **99 Motorista**, **iFood Entregas**, **inDrive**, **KeeTa** e **Urbano Norte**).

O aplicativo resolve o principal gargalo operacional do motorista: a tomada de decisão em frações de segundo durante uma chamada de corrida, calculando instantaneamente o lucro líquido real, ganho por km, ganho por hora e margem percentual de lucro antes de o motorista aceitar ou recusar a oferta.

---

## 2. Telas e Recursos Analisados via Engenharia Reversa

A análise visual e funcional dos fluxos do aplicativo revelou as seguintes diretrizes de design e arquitetura:

### 2.1 Cartão Flutuante do Semáforo (Telas 1 e 2)
- **Design:** Cartão branco arredondado com borda e barras coloridas refletindo o status do Semáforo (Verde, Amarelo, Vermelho ou Roxo).
- **As 4 Métricas Essenciais:**
  1. `R$/km` (ex: `2,25`)
  2. `R$/hora` (ex: `48,77`)
  3. `Lucro` em reais (ex: `18,90`)
  4. `Lucro %` (ex: `75%`)
- **Rodapé da Oferta:**
  - Ícone e identificador da plataforma (Uber, 99, iFood, etc.)
  - Formato de tempo e distância: `0h31m • 11,2km`
  - Tarifa Dinâmica: Identificação de multiplicador de alta demanda (ex: `⚡ x1.4` ou `Tarifa mais alta`).
- **Opções de Layout:** Modo Horizontal (Completo) ou Vertical (Compacto).

### 2.2 Régua Dupla de Critérios do Semáforo (Tela 4)
Em vez de um único corte linear, o GigU utiliza dois marcadores ajustáveis para definir 3 zonas claras:
- **Faixa Vermelha (Ruim / Prejuízo):** Abaixo de $R\$\ 1,85/\text{km}$ ou $R\$\ 35,00/\text{hora}$.
- **Faixa Amarela (Média / Alerta):** Entre os limites mínimo e ideal.
- **Faixa Verde (Boa / Excelente):** Acima de $R\$\ 2,35/\text{km}$ e $R\$\ 45,00/\text{hora}$.

### 2.3 Simulação de Ganhos Maiores & Metas (Tela 3)
- Visão agregada por período: `[ DIA | SEMANA | MÊS ]`.
- Projeção de lucro mensal: $\text{Faturamento Bruto} - \text{Custos do Carro} = \text{Lucro Líquido Real}$.
- Ferramenta "Simule Ganhos Maiores" (`+0%` a `+50%`) com aplicação direta nos limites do semáforo.

### 2.4 Ajustes e Resumo por Voz Inteligente (Tela 6)
- **Filtro de Leitura:** O motorista escolhe quais tipos de corrida deseja que o celular leia em voz alta (`[ 🟢 Boa ]`, `[ 🟡 Média ]`, `[ 🔴 Ruim ]`).
- **Velocidade de Fala:** Slider de $0.8\text{x}$ a $2.0\text{x}$.
- **Sons de Alerta:** Tons distintos (`GigU`, `Sino`, `Plim`).
- **Duração do Cartão:** Temporário (some após tempo pré-determinado) ou Persistente.
- **Estilo de Emojis:** Bolinhas (`🟢 🟡 🔴`) ou Ícones (`✅ ⚠️ ❌`).
- **Captura de Tela Automática:** Screenshot automático para fins de auditoria financeira.

### 2.5 Aplicativos Integrados Suportados (Tela 7)
1. **Uber Driver** (`com.ubercab.driver`)
2. **99 Motorista** (`com.taxis99`)
3. **iFood Entregas** (`com.ifood.driver`)
4. **inDrive** (`sinet.startup.inDriver`, `com.indrive`)
5. **KeeTa Courier** (`com.keeta.courier`, `com.keeta.driver`)
6. **Urbano Norte** (`com.urbanonorte.driver`, `br.com.urbanonorte.motorista`)

---

## 3. Arquitetura Modular e Fluxos do Código Limpo

```
app/src/main/java/com/auza/giguclone/
├── data/
│   ├── db/
│   │   └── RideDatabaseHelper.kt    # SQLite para histórico diário e estatísticas
│   └── parser/
│       └── RideOfferParser.kt       # Parser dos 6 apps (dinâmico, notas, km, R$)
├── domain/
│   ├── model/
│   │   └── Models.kt                # Modelos de domínio puros sem dependências de UI
│   └── calculator/
│       └── ProfitabilityCalculator.kt # Motor de cálculo da régua dupla e lucro %
├── service/
│   ├── RideAccessibilityService.kt  # Serviço de acessibilidade + auto-rejeição
│   ├── FloatingOverlayService.kt    # Janela flutuante no estilo do GigU
│   ├── SecretCameraService.kt       # Câmera discreta de segurança
│   └── TtsManager.kt                # Síntese de voz com filtros e tons
└── ui/
    ├── MainActivity.kt              # Gerenciador de ciclo de vida e permissões
    ├── DashboardScreen.kt           # Dashboard principal com seletor de combustível
    ├── HistoryScreen.kt             # Histórico com design fiel das corridas
    ├── SettingsScreen.kt            # Régua dupla, metas, voz e segurança
    └── theme/Theme.kt               # Paleta de cores moderna de alto contraste
```
