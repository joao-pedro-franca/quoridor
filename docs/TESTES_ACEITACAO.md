# Validação automática do Quoridor RMI

## Execução

Na raiz do projeto, com JDK 21 e Maven:

```sh
mvn clean verify
```

Esse é o comando recomendado antes de apresentar ou entregar: executa **264
testes** e gera o JAR. Os testes rodam na fase `test`, pelo Surefire já existente;
não foi necessário adicionar bibliotecas ou outro executor de testes.

Para executar apenas a seleção de aceitação:

```sh
mvn -Paceitacao clean verify
```

O perfil seleciona **75 testes** de `AceitacaoJogoTest`, `ClienteIntegracaoTest`,
`ClientesProcessosTest`, `ServicoJogoTest`, `IntegracaoRmiTest` e
`ConcorrenciaRmiTest`, `HeartbeatTest` e `ConexoesCLITest`. Ele reaproveita a cobertura existente. Não substitui a
verificação completa das regras, comandos e modelos do comando sem perfil.

Os relatórios ficam em `target/surefire-reports/`. Cada execução com `clean`
remove os relatórios anteriores. O ajuste de certificado local, se necessário,
continua documentado no README.

## Análise dos 229 testes existentes

Foram examinadas todas as classes de teste e seus auxiliares `Cenarios` e
`ApoioRmi` antes de implementar os roteiros novos.

| Classes existentes | Casos | Cobertura já disponível |
| --- | ---: | --- |
| `PosicaoTest`, `JogadorTest`, `BarreiraTest`, `EstadoJogoTest` | 19 | Limites, identidade, estoque, coleções defensivas e encapsulamento. |
| `PartidaTest` | 5 | Posições iniciais, objetivos, estoque e estado inicial. |
| `MovimentosTest` | 39 | Movimentos, limites, barreiras, saltos e diagonais, inclusive vários peões. |
| `ControlePartidaTest` | 20 | Sorteio controlado, turnos, ações inválidas, quatro objetivos e término. |
| `BarreirasTest` | 25 | H/V, dois segmentos, extremos, sobreposição parcial/total, cruzamento, encontros permitidos e esgotamento do estoque. |
| `CaminhosTest` | 10 | BFS, desvios, peões ignorados, preservação dos quatro caminhos e cercamento individual de cada jogador. |
| `ComandoCLITest` | 51 | Sintaxe, pares consecutivos, conversão de todas as âncoras e coordenadas inválidas. |
| `TabuleiroCLITest`, `TerminalCLITest` | 18 | Peões, barreiras, prévias destacadas, estoques, vencedor, redesenho e sequências de preservação do cursor. |
| `ClienteIntegracaoTest` | 15 | Partida completa via CLI/RMI, H/V com a sintaxe nova, confirmação, cancelamento sem RPC, estoque/turno preservados, EOF, reconexão e erros. |
| `ServicoJogoTest` | 17 | Quatro registros, quinto recusado, tokens inválidos, espera, turno, estoque, snapshots e partida completa com rejeição após vitória. |
| `IntegracaoRmiTest` | 4 | Registro RMI real, lookup por stub, cópias independentes, ações visíveis nas consultas, reconexão e concorrência remota. |
| `ConcorrenciaRmiTest` | 3 | Registro simultâneo, uma ação por turno e consistência entre barreiras, estoque e turno durante consultas. |
| `ClientesProcessosTest` | 3 | Quatro JVMs clientes, atualização sem digitação, confirmação de barreira, quinto recusado, reconexão e saída após desconexão. |

Havia duas partidas completas, mas sem barreiras no roteiro. O cercamento era
verificado com cenários controlados do motor; faltava construí-lo desde o início
por comandos normais e confirmar a recusa através de RMI. Também faltava uma
proposta que se tornasse inválida entre a prévia e a confirmação.

## Três roteiros novos

Todos estão em `src/test/java/br/edu/quoridor/cliente/AceitacaoJogoTest.java`.
Cada teste cria seu próprio serviço e registro, faz quatro registros reais,
obtém quatro referências por lookup e conecta instâncias da CLI às sessões.
O turno inicial é controlado pela origem de aleatoriedade já disponível no
serviço. Não se alteram peões, turnos, barreiras ou estoques por atalhos de teste.

### Partida completa com barreiras e vitória do jogador 2

1. O jogador 1 cancela uma prévia e depois confirma `barreira h 3,4 4,5`,
   verificando a âncora interna `(2,3)` e o consumo de uma barreira.
2. O jogador 2 inicia sua rota pela linha 4 da CLI.
3. O jogador 3 tenta sobrepor e cruzar a horizontal com os exemplos da sintaxe
   nova. As ações são recusadas e o estado completo é comparado antes e depois.
4. O jogador 3 confirma uma vertical em outro local; o jogador 4 move seu peão.
5. Os jogadores se alternam em jogadas legais. O jogador 2 chega à coluna 1 da
   CLI, equivalente à coluna interna 0. Os demais alternam casas sem passar
   turnos artificialmente.
6. Os quatro acessos remotos recebem estados com posições, objetivos, barreiras,
   estoques, turno e vencedor consistentes. O total disponível mais o total
   colocado continua igual a 20 barreiras.
7. Os quatro clientes exibem o vencedor e tentam mover ou confirmar barreiras
   depois do término; todas essas ações são recusadas sem alterar o estado final.

### Cercamento via comandos legais e RMI

A borda superior e uma horizontal delimitam duas casas próximas ao peão 1.
Uma vertical fecha um lado. A tentativa de fechar o outro lado é rejeitada
porque eliminaria a saída para o objetivo. Não há cruzamento entre essas
barreiras. O teste verifica posições, estoque, turno, vencedor e barreiras
antes e depois da tentativa, depois executa outra ação do mesmo jogador e
confirma que o peão 1 ainda consegue escapar pela passagem preservada.

### Confirmação de uma prévia desatualizada

O jogador 1 visualiza uma horizontal sem enviá-la. Outra conexão autenticada
da mesma sessão move legalmente seu peão; o jogador 2 ocupa o local da proposta.
Os jogadores 3 e 4 jogam e devolvem o turno ao jogador 1. A confirmação antiga
é então recusada por sobreposição, mesmo estando no turno correto. A prévia
não reserva o local nem substitui a validação do servidor. Uma nova ação legal
continua possível após a rejeição.

Esses roteiros combinam funcionalidades em sequências mais longas; não recriam
as dezenas de testes isolados de sintaxe, geometria, saltos e autenticação.
Não foram acrescentados outros processos Java: a cobertura de quatro JVMs e
do polling já existe e é reutilizada pelo perfil.

## Timeouts e limpeza

- Não há sleeps nos roteiros. As chamadas são verificadas pela resposta e pelos
  snapshots; os testes de processos esperam mensagens observáveis com prazo máximo.
- O Surefire define timeout JUnit padrão de 30 segundos. Classes que já têm
  limites explícitos os mantêm, como os 45 segundos dos testes de processos.
- O RMI do JDK recebe limites de cinco segundos para resposta e handshake
  durante os testes, impedindo espera indefinida nessas etapas de uma chamada.
- Os roteiros novos usam a porta 0; o RMI escolhe uma porta livre. Os testes de
  processos continuam usando uma porta disponível diferente de 1099.
- Cada cenário libera serviço e registro em `@AfterEach`. A limpeza dos processos
  tenta todos os clientes mesmo se algum encerramento falhar, e força a destruição
  com espera limitada se EOF ou interrupção não bastarem. Só os processos criados
  pelo próprio teste são gerenciados.
- Os processos são aguardados até terminar. Os testes não usam portas de
  demonstração, arquivos externos ou comandos de trapaça na aplicação.

## Limites da automação e conferências manuais

Não é mais necessário jogar uma partida inteira manualmente para verificar
vitória, turnos, barreiras, confirmação ou comunicação local.

Ainda convém uma conferência curta no terminal usado na apresentação:

- Aparência real do painel ANSI, acentuação, cursor e legibilidade com pelo
  menos 80 colunas e 24 linhas, incluindo digitar enquanto chega uma atualização.
  Os testes de processos usam pipes e não emulam o editor de linha do PowerShell.
- Comportamento visual ao redimensionar o terminal e ao usar Ctrl+C.
- Permissões e configuração de rede/firewall específicas da máquina da apresentação.

Os testes não são uma prova de todas as partidas possíveis nem um ensaio de
carga prolongado. Também não incluem uma partida inteira com o `main` do servidor
em uma quinta JVM: o serviço e o registro RMI reais são hospedados pela JVM de
testes, enquanto os quatro processos clientes existentes continuam independentes.
Isso é separado da validação funcional das regras e das chamadas remotas.

## Ampliação: heartbeat e prazo de reconexão

Os 232 testes anteriores foram preservados. Foram acrescentados 32 casos:
21 em `HeartbeatTest`, oito em `ConexoesCLITest`, dois em `EstadoJogoTest` e
um em `IntegracaoRmiTest`. O perfil de aceitação inclui os dois novos conjuntos
de conexão e a chamada RMI real, totalizando 75 casos. A suíte completa tem 264.

Os limites de ausência (10 segundos) e reconexão (180 segundos) são simulados
por um `LongSupplier` monotônico controlado. O monitor continua automático,
encerra mesmo sem consultas e é desligado na limpeza. Há testes de operações
concorrentes junto ao limite, múltiplos ausentes, preservação de jogo e turno,
abandono sem vencedor e prévia durante a pausa. O teste de digitação espera
sinais de dois heartbeats reais, sem depender de uma pausa exata.
