# Instruções para Google AI Studio — teleprompter auditivo Android

## Como usar este arquivo

No Google AI Studio, abra Build e selecione a plataforma Android. Anexe este arquivo ou cole seu conteúdo e peça: “Implemente o protótipo descrito neste documento”. Se a opção Android não estiver disponível na conta, informe essa limitação; não substitua silenciosamente por um aplicativo web.

## Missão

Implemente um aplicativo Android nativo funcional, com nome provisório **Escuta e Grava**, para Rogério Wilbert. Não entregue apenas um plano, telas demonstrativas ou botões sem implementação. Construa, compile e disponibilize a instalação de teste pelos recursos efetivamente disponíveis no AI Studio. Não publique nem ative serviços pagos automaticamente.

O usuário grava vídeos olhando para a lente. Um fone Bluetooth reproduz o roteiro como guia de fala. Se perder uma frase, o usuário toca nela na tela, ouve novamente e refaz a fala, sem interromper a gravação. Ele corta as repetições posteriormente em um editor externo.

## Decisões já tomadas

- Plataforma Android nativa; Kotlin, Jetpack Compose e CameraX.
- Câmera integrada no próprio aplicativo.
- Texto e controles sobre a prévia da câmera, apenas na interface. Nunca incorporar esses elementos ao arquivo de vídeo.
- Não implementar janela sobre outros aplicativos nesta versão. Não pedir permissão de sobreposição nem acessibilidade.
- Sem login, servidor, Firebase, anúncios, pagamentos, analytics, API Gemini ou outra API de IA nesta versão.
- Usar o motor TextToSpeech disponível no Android. Não presumir voz específica instalada ou funcionamento offline de qualquer voz.
- Não inserir chaves de API ou credenciais no projeto.
- Usar versões estáveis e compatíveis das dependências disponíveis no ambiente; não inventar números de versão ou APIs.
- Preparar um protótipo para testes pessoais. Comercialização e publicação pública ficam para outra etapa.

## Fluxo de uso obrigatório

1. O usuário cola um roteiro em português brasileiro, informa um título e salva localmente.
2. O aplicativo divide o roteiro em frases, preservando palavras, acentos e ordem. Permitir corrigir a divisão manualmente antes de gravar, especialmente em abreviações, números e referências bíblicas.
3. O usuário conecta seu fone Bluetooth pelas configurações do Android.
4. Seleciona uma voz disponível em pt-BR, ouve uma amostra e ajusta velocidade e pausa.
5. Abre a câmera frontal; a câmera traseira pode ser selecionada antes da gravação.
6. Faz um teste curto para confirmar a saída no fone e o áudio gravado pelo microfone.
7. Toca em Gravar. Somente após a confirmação de início da gravação começa a contagem visual de três segundos; então começa a leitura.
8. O texto destaca a frase em reprodução e acompanha automaticamente a seleção.
9. Ao tocar em qualquer frase, o áudio atual para e essa frase começa do início. A gravação continua no mesmo arquivo.
10. Ao terminar a leitura, o vídeo continua gravando até o usuário tocar em Finalizar gravação.
11. O vídeo finalizado fica disponível na galeria, com ação de abrir ou compartilhar pelo seletor padrão do Android.

## Tela de roteiro e ajustes

- Campo de título e campo de texto multilinha para colar e editar.
- Lista de frases editáveis, com ações para unir ou dividir trechos antes da gravação.
- Salvar, reabrir e excluir roteiros locais. Confirmar exclusões.
- Seletor das vozes pt-BR retornadas pelo motor TTS, com identificação das que exigem rede quando a API fornecer essa informação.
- Se pt-BR estiver indisponível, oferecer o fluxo de instalação dos dados de voz ou seleção de outro motor. Não fingir que uma voz foi instalada.
- Amostra de voz antes da gravação.
- Velocidade inicial de 0,9x, ajustável entre 0,6x e 1,2x.
- Intervalo fixo entre frases de 0 a 5 segundos, em passos de 0,5 segundo, padrão de 1 segundo.
- Persistir roteiros, divisão manual e preferências em armazenamento local simples, adequado ao tamanho dos dados. Não adicionar banco ou arquitetura complexa sem necessidade.

## Tela de gravação

- Prévia da câmera ocupando a maior parte da tela, com tratamento correto da orientação.
- Primeira versão em retrato, visando vídeos verticais. Solicitar 1080p quando suportado e aplicar fallback real para 720p ou outra qualidade disponível, informando a qualidade efetiva.
- Frontal por padrão. O resultado salvo deve manter orientação correta e texto físico na cena legível; conferir espelhamento da prévia e do arquivo separadamente.
- Painel de roteiro semitransparente, recolhível, com fonte legível e altura ajustável.
- Frases com áreas de toque confortáveis, mostrando frase atual e trechos vizinhos.
- Botões separados: Gravar/Finalizar gravação; Iniciar/Pausar leitura; Repetir frase; Frase anterior; Próxima frase.
- Indicador inequívoco de gravação e duração do vídeo.
- Manter a tela acesa durante a sessão. Não ativar gravação em segundo plano.
- Impedir edição do roteiro, troca de câmera e alteração de configurações que possam invalidar a sessão enquanto grava.
- Uma única gravação contínua: tocar em uma frase e pausar a leitura nunca devem chamar pause, stop ou restart da gravação.

## Reprodução e repetição: comportamento exato

- Dois estados independentes: estado da gravação e estado da leitura.
- Iniciar leitura é permitido antes de gravar para ensaio. Ao iniciar a gravação, cancelar o ensaio e iniciar pela frase selecionada após a contagem.
- Reproduzir uma frase por vez, usando identificador exclusivo por execução e callbacks de conclusão do TTS.
- Agendar a próxima frase somente após o callback de término e o intervalo configurado. Não estimar término apenas pelo número de palavras.
- No modo contínuo, tocar numa frase cancela áudio e avanço pendentes, seleciona essa frase e a reproduz. Ao terminar, continua da frase seguinte.
- Se a leitura estava pausada, tocar numa frase reproduz somente essa frase e retorna ao estado pausado.
- Pausar leitura interrompe a fala atual e cancela o próximo avanço. Retomar reinicia a frase selecionada, pois o TTS não oferece pausa exata universal no meio da fala.
- Repetir frase reinicia a frase selecionada. Anterior e Próxima usam a mesma regra de seleção e reprodução.
- Toques rápidos sucessivos devem respeitar o último comando, sem fila de frases antigas. Invalidar callbacks e tarefas de sessões anteriores.
- Ao concluir o roteiro, indicar “Leitura concluída” e não finalizar a câmera.
- Ao finalizar a gravação, cancelar leitura e tarefas pendentes e aguardar a finalização do arquivo antes de informar que foi salvo.

## Áudio, Bluetooth e microfone

Este é o principal risco técnico do projeto e exige validação em aparelho físico.

- Objetivo: reproduzir a voz guia no fone Bluetooth e registrar a fala do usuário pelo microfone escolhido, preferencialmente o microfone do celular no protótipo.
- Não mixar digitalmente o TTS no vídeo. O áudio do vídeo deve vir da captura do microfone. Isso não garante ausência de vazamento acústico do fone.
- Verificar a rota disponível antes de começar e usar APIs públicas adequadas à versão do Android. Exibir rota identificada; se não puder confirmar, dizer explicitamente que não foi confirmada.
- Não ativar modo de chamada/SCO automaticamente apenas para reproduzir o guia; isso pode alterar microfone e qualidade. Não garantir controle de rota que o dispositivo não suporte.
- Se o fone desconectar, pausar imediatamente a leitura e avisar visualmente. Não continuar o guia no alto-falante de propósito. A gravação deve continuar se a captura permitir; se ocorrer erro de captura, informar o estado verdadeiro.
- Lidar com foco de áudio, chamadas e interrupções. Não prometer que o sistema nunca interromperá a sessão.
- Permitir gravar um teste curto e ouvir o resultado para avaliar microfone, voz guia e eventuais vazamentos.
- Não criar seletor de microfone fictício. Se o controle de entrada não estiver disponível de forma confiável na implementação, mostrar entrada padrão e documentar essa limitação.
- Não prometer latência zero. Testar repetição por toque com o fone real.
- Validar um roteiro curto em modo avião usando uma voz instalada que não exija rede; caso não exista voz offline, explicar a dependência da voz selecionada.

## Permissões, arquivos e falhas

- Solicitar câmera e microfone no contexto do uso, com explicação curta em português. Tratar negação e opção de abrir configurações sem loop de pedidos.
- Solicitar apenas permissões adicionais efetivamente exigidas pelas APIs usadas, incluindo Bluetooth quando necessário para consulta/monitoramento da rota. Não pedir localização sem necessidade.
- Salvar vídeo via MediaStore nas versões compatíveis, sem exigir acesso amplo a todos os arquivos.
- Tratar falta de espaço, câmera indisponível, erro do TTS, idioma ausente, finalização malsucedida e saída inesperada da tela.
- Não declarar arquivo salvo se a finalização falhar. Não descartar vídeos existentes ao reabrir o app.
- Liberar câmera, TTS, foco de áudio e tarefas ao sair da sessão. Pedir confirmação antes de uma saída voluntária que encerre a gravação.
- Mostrar mensagens úteis, sem stack traces na interface. Logs técnicos não devem conter o texto integral dos roteiros.

## Fora do escopo

Não implementar clonagem de voz, voz premium, nuvem, pagamento, login, legendas automáticas, edição de vídeo, cortes automáticos, detecção da fala do usuário ou sincronização por reconhecimento de voz. A leitura avança por conclusão do TTS e intervalo escolhido, sem tentar adivinhar se o usuário terminou de falar.

Uma evolução futura pode adicionar áudios premium previamente gerados e armazenados por frase. Não implementar essa integração nem cobrar serviços nesta etapa. Não copiar código, marca ou elementos exclusivos do produto citado como inspiração.

## Ordem de execução e economia

1. Criar o projeto Android mínimo e compilável com editor, divisão de frases, TTS e persistência local.
2. Integrar CameraX, prévia e gravação com áudio.
3. Integrar leitura simultânea, repetição por toque e cancelamento correto.
4. Compilar, corrigir erros e disponibilizar instalação no aparelho pelos recursos existentes no AI Studio.
5. Apresentar checklist curto para testes físicos e limitações reais.

Execute essas etapas no mesmo projeto, sem pedir confirmação a cada função. Faça alterações incrementais e preserve o que já funciona. Não reescreva tudo a cada ajuste, não gere vários frameworks e não produza explicações longas no lugar do código. Se um recurso do ambiente estiver indisponível, informe o bloqueio e a menor alternativa viável.

## Critérios de aceite

| Teste | Resultado exigido |
| --- | --- |
| Compilação | Build real concluído; não basta o código parecer correto |
| Instalação | App abre em aparelho Android físico |
| Roteiro | Texto e divisões manuais sobrevivem ao fechamento e reabertura |
| Permissões | Negar câmera ou microfone não causa travamento |
| Voz | Amostra pt-BR compreensível no fone do usuário |
| Gravação | Vídeo vertical com voz do usuário e orientação correta |
| Repetição | Tocar numa frase anterior repete o trecho sem interromper o vídeo |
| Toques rápidos | Só a última seleção é executada; não há sobreposição de vozes |
| Pausa de leitura | Vídeo continua; retomar reinicia a frase selecionada |
| Fim do roteiro | Leitura termina; vídeo continua até comando explícito |
| Fone desconectado | Leitura para e a interface informa a desconexão |
| Arquivo salvo | Finalização confirmada, vídeo acessível na galeria e compartilhável |
| Interface | Texto e botões não aparecem no vídeo salvo |
| Interrupções | Estado real de gravação e erros são informados sem alegar sucesso |

## Entrega exigida do AI Studio

- Projeto completo, sem pseudocódigo, TODOs essenciais ou respostas simuladas.
- Resultado verdadeiro da compilação e lista de pendências.
- Instruções exatas para instalar no celular com os recursos presentes na interface atual. Usar instalação USB pelo navegador se disponível; explicar depuração e autorização no aparelho. Não inventar botão de download de APK.
- APK de teste se o ambiente permitir fornecê-lo; caso ofereça apenas instalação direta ou outro fluxo, informar claramente o que está disponível.
- Exportação do código-fonte pela opção disponível, para evitar dependência do ambiente.
- Separar “compilado”, “testado no emulador” e “testado no aparelho”. Nunca afirmar validação de Bluetooth ou qualidade de microfone apenas com emulador.
- Não publicar automaticamente em Google Play. Instalação pessoal, teste interno e publicação comercial são etapas diferentes. Após aprovação no aparelho, explicar a geração/assinatura do AAB e os requisitos de publicação vigentes, sem prometer aprovação.

## Referências oficiais para consultar quando necessário

- AI Studio Build: https://ai.google.dev/gemini-api/docs/aistudio-build-mode
- Android no AI Studio: https://ai.google.dev/gemini-api/docs/aistudio-android
- CameraX VideoCapture: https://developer.android.com/media/camera/camerax/video-capture
- Android TextToSpeech: https://developer.android.com/reference/android/speech/tts/TextToSpeech
- Verificação e testes por ADB: https://developer.android.com/developer-verification/guides/faq

Use a documentação vigente para resolver detalhes de API. Este documento especifica o produto; a disponibilidade do ambiente e o funcionamento no aparelho precisam ser verificados, não presumidos.
