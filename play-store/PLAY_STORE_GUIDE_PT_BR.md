# Guia Completo de Publicação na Google Play Store — Escuta e Grava

Este guia reúne tudo o que você precisa para publicar o **Escuta e Grava** na Google Play Store com o modelo **Modo Teste gratuito + Pro (R$ 25,00/ano ou R$ 50,00 vitalício)**.

---

## 1. Custos e regras atuais da Google Play Console

| Item | Valor / Regra |
|---|---|
| **Taxa de cadastro de desenvolvedor** | **US$ 25,00** (pagamento único, vitalício — aprox. R$ 135 a R$ 150 dependendo do dólar/IOF) |
| **Comissão do Google Play sobre vendas** | **15%** nos primeiros US$ 1 milhão/ano (na prática: em R$ 25,00 você recebe ~R$ 21,25; em R$ 50,00 você recebe ~R$ 42,50) |
| **Conta Pessoal (CPF)** | Exige **teste fechado com mínimo de 12 testadores** participantes contínuos por **14 dias** antes de liberar produção |
| **Conta Empresarial (CNPJ + D-U-N-S)** | Não exige os 12 testadores por 14 dias, mas exige número D-U-N-S gratuito e verificação empresarial |
| **Formato exigido para envio** | **AAB** (Android App Bundle assinado), e não APK |

---

## 2. Imagens já prontas neste projeto (`play-store/assets/`)

Todos os arquivos abaixo já foram gerados e estão salvos na pasta `play-store/assets/`:

1. `icon_512x512.png` — **Ícone do app (512 × 512 px)** — obrigatório na Play Console.
2. `feature_graphic_1024x500.png` — **Recurso gráfico / Banner (1024 × 500 px)** — obrigatório na Play Console.
3. `01_teleprompter_auditivo.jpg` — Arte promocional vertical 1 (Teleprompter auditivo no fone).
4. `02_voz_narrador_retorno.jpg` — Arte promocional vertical 2 (Voz estilo narrador e volume do retorno).
5. `03_camera_pro_lentes.jpg` — Arte promocional vertical 3 (Controles de câmera, lentes e abertura).

> **Dica de ouro:** além dessas 3 artes promocionais, tire **3 a 4 capturas de tela reais do app rodando no seu celular** (Tela de Roteiro, Aba Voz e Áudio, Estúdio de Câmera e Tela de Ajustes de Lente) para subir junto na galeria de screenshots — a Play Store valoriza capturas reais da interface.

---

## 3. Textos prontos para copiar e colar na Play Console

### Nome do app (máx. 30 caracteres — 14 usados)
```text
Escuta e Grava
```

### Descrição curta (máx. 80 caracteres — 77 usados)
```text
Teleprompter no fone Bluetooth para gravar vídeos naturais olhando pra lente.
```

### Descrição completa
```text
Grave vídeos com naturalidade, olhando direto para a lente da câmera — sem precisar decorar textos nem ficar com o olhar preso lendo a tela.

O Escuta e Grava funciona como um teleprompter auditivo profissional no seu celular: você escreve ou cola seu roteiro, conecta seu fone Bluetooth e ouve frase por frase no ouvido enquanto grava com a câmera do aparelho.

PRINCIPAIS RECURSOS
• Teleprompter auditivo em Português (pt-BR): ouça o roteiro frase por frase no fone sem vazar no vídeo.
• Divisão inteligente de frases: respeita pontuação, abreviações, números e referências bíblicas, com edição, união e divisão manual.
• Voz estilo narrador: perfis prontos (Narrador fluido, Locutor grave, Conversa leve e Didático), ajuste de tom (grave/agudo), velocidade, pausa entre frases e prosódia natural.
• Volume do retorno ao vivo: aumente, diminua ou silencie o som no fone diretamente na tela da câmera, mesmo durante a gravação.
• Proteção de fone Bluetooth: se o fone desconectar, a leitura pausa imediatamente para evitar som no alto-falante.
• Controles avançados de câmera: escolha entre as lentes do aparelho, veja/ajuste abertura (f/), zoom rápido, toque para focar, exposição (EV), ISO e obturador manual (conforme suporte do aparelho), balanço de branco, resolução, FPS, estabilização e lanterna.
• 100% privado e offline: seus roteiros e vídeos ficam apenas no seu celular.

MODO TESTE GRATUITO E PLANO PRO
Baixe grátis e experimente todas as funções: crie roteiros, ajuste a voz narrador, conecte seu fone Bluetooth, ensaie na câmera e teste todas as lentes e ajustes profissionais sem custo.

Quando quiser gravar e salvar seus vídeos ilimitados na galeria do celular (sem marca d'água e sem anúncios), ative o Escuta e Grava Pro:
• Plano Anual: R$ 25,00/ano (renovação anual pelo Google Play; cancele quando quiser).
• Acesso Vitalício: R$ 50,00 (pagamento único).
```

### Notas da versão (1.0)
```text
Lançamento oficial do Escuta e Grava: teleprompter auditivo em pt-BR, voz estilo narrador com volume de retorno ao vivo, controles avançados de câmera e modo teste gratuito com opção Pro (anual ou vitalício).
```

---

## 4. Produtos para criar na Google Play Console (EXATOS!)

No painel da Play Console, em **Monetizar com o Google Play → Produtos**, crie exatamente estes dois IDs (o código do app já busca esses IDs pelo Google Play Billing 8.0.0):

### Produto 1 — Assinatura Anual (`SUBS`)
- **Onde criar:** *Assinaturas → Criar assinatura*
- **ID do produto:** `escuta_grava_pro_anual`
- **Nome:** `Escuta e Grava Pro (Anual)`
- **Plano básico (Base plan ID):** `anual`
- **Período de faturamento:** `Anual (1 ano)`
- **Preço no Brasil:** `R$ 25,00`
- **Status:** Ativar plano básico.

### Produto 2 — Acesso Vitalício (`INAPP`)
- **Onde criar:** *Produtos únicos (ou Produtos no app) → Criar produto*
- **ID do produto:** `escuta_grava_pro_vitalicio`
- **Nome:** `Escuta e Grava Pro (Vitalício)`
- **Descrição:** `Libera gravação e salvamento ilimitado de vídeos para sempre.`
- **Preço no Brasil:** `R$ 50,00`
- **Status:** Ativar produto.

---

## 5. Política de Privacidade e Segurança de Dados (Data Safety)

1. **URL da Política de Privacidade:**
   - O arquivo `play-store/PRIVACY_POLICY.md` já está pronto no seu repositório GitHub.
   - Você pode ativar o **GitHub Pages** no repositório (Settings → Pages → Branch `main`) ou usar o link público do arquivo no GitHub para informar na Play Console.
2. **Formulário Segurança de Dados (Data Safety):**
   - O app coleta ou compartilha dados pessoais do usuário com servidores próprios? **Não** (roteiros e vídeos ficam 100% no dispositivo; pagamentos são processados apenas pelo Google Play).
   - O app contém anúncios? **Não**.
   - Público-alvo: **13+ / Público geral** (não direcionado a crianças).
   - **Declaração de compras no app:** marcar que o app oferece compras digitais pelo Google Play.

---

## 6. Passo a passo para gerar o AAB e subir na Play Console

1. **No AI Studio:** clique no menu de configurações/exportação e gere o **Android App Bundle (.aab)** assinado (ou use a chave de upload de release no GitHub/computador).
   - *Importante:* na compilação de **Release (`.aab`)**, o botão de atualização pelo GitHub e a chave de simulação Debug são removidos automaticamente do app para cumprir 100% as políticas da Google Play.
2. **Na Google Play Console:**
   - Crie o app com o título **Escuta e Grava** (Idioma padrão: Português Brasil).
   - Preencha **Ficha da loja principal** usando os textos da seção 3 e as imagens de `play-store/assets/`.
   - Cadastre os 2 produtos da seção 4 (`escuta_grava_pro_anual` e `escuta_grava_pro_vitalicio`).
   - Envie o `.aab` primeiro em **Teste interno** (para testar as compras com sua própria conta sem pagar nada, cadastrando seu e-mail em *Testadores de licença*).
   - Depois avance para **Teste fechado** (12 testadores por 14 dias em conta pessoal) e finalmente **Produção**!
