# Quoridor RMI

Projeto universitário de **Sistemas Distribuídos**: uma partida de Quoridor para
quatro jogadores, com **um processo servidor e quatro processos clientes Java**
em localhost. Cada cliente possui uma interface de terminal e comunica-se com
o servidor exclusivamente por Java RMI.

O trabalho demonstra chamadas remotas, serialização, identificação de sessões,
controle de concorrência e manutenção de um estado autoritativo compartilhado.

A configuração Maven atual da branch `main` utiliza a versão **2.0.0**.
As tags Git `v1.0.0` e `v2.0.0` preservam os commits publicados originalmente;
alterações posteriores no README ou no `pom.xml` não reescrevem essas tags.

## Tecnologias e requisitos

- **JDK 21**, com `java` e `javac` disponíveis no `PATH`.
- **Maven**, disponível no `PATH`.
- **Java RMI**, parte do JDK, para descoberta e chamadas remotas.
- **JUnit 5.11.4**, somente para testes, com Maven Surefire.
- PowerShell/terminal integrado do VS Code, com **80 colunas e 24 linhas ou mais**
  para o painel interativo ANSI.

Não há dependências de produção externas, banco de dados, Spring Boot ou uso
direto de `Socket`/`ServerSocket`. O transporte é gerenciado pelo Java RMI.
A primeira compilação precisa de acesso aos repositórios Maven para baixar
plugins e dependências de teste.

Confira o ambiente:

```sh
java -version
javac -version
mvn -version
```

## Compilar e executar em cinco terminais

Execute todos os comandos a partir da **raiz deste projeto**. Compile antes de
abrir os processos da demonstração:

```sh
mvn clean package
```

O comando executa os testes e gera `target/classes` e
`target/quoridor-rmi-2.0.0.jar`. Os comandos abaixo usam `target/classes`;
não é necessário configurar um classpath de dependências externas.

### Terminal 1: servidor

```sh
java -cp target/classes br.edu.quoridor.servidor.Servidor
```

O servidor publica `rmi://127.0.0.1:1099/Quoridor` e aguarda os participantes.
Não é necessário executar `rmiregistry` separadamente.

### Terminais 2, 3, 4 e 5: clientes

Abra **quatro terminais independentes** e execute em cada um:

```sh
java -cp target/classes br.edu.quoridor.cliente.Cliente
```

Cada comando cria uma JVM cliente independente. Informe um nome e pressione
Enter no campo de token para registrar um novo jogador. O nome é um apelido
local; o servidor atribui o ID de 1 a 4 e gera o token de sessão.

Guarde o próprio token, exibido no painel ou pelo comando `sessao`. Os clientes
informam quantos jogadores já se registraram. As jogadas são liberadas somente
após o quarto registro, respeitando o turno inicialmente sorteado. Um quinto
registro é recusado.

O cliente também aceita host e porta opcionais:

```sh
java -cp target/classes br.edu.quoridor.cliente.Cliente 127.0.0.1 1099
```

A demonstração prevista é em localhost; o servidor anuncia referências para
`127.0.0.1`. Informar outro host ao cliente não configura o servidor para uma LAN.

### Encerrar e reconectar

- No cliente, `sair` e fim da entrada notificam a desconexão imediatamente.
  Ctrl+C ou encerramento abrupto são detectados pela ausência de heartbeat.
  A vaga permanece reservada durante o prazo de reconexão.
- Para retornar, inicie outro cliente e informe o token original quando solicitado.
  `reconectar` recupera o mesmo ID sem criar uma quinta vaga, dentro de 180 segundos
  da detecção. O jogo fica pausado enquanto houver qualquer participante ausente.
- Para encerrar o servidor, pressione **Ctrl+C no terminal 1**. O gancho de
  encerramento remove a publicação e libera o serviço. O registro RMI é liberado
  somente se tiver sido criado por esse processo.
- Para uma nova partida, encerre e reinicie o servidor e os clientes. Não existe
  comando de reinício durante uma sessão. Partida e tokens ficam apenas em memória.

### Heartbeat, pausa e abandono

Cada cliente registrado envia `heartbeat(token)` por RMI a cada **3 segundos**,
no mesmo laço que consulta o estado. Isso funciona mesmo sem jogadas, aguardando
o turno ou com um comando parcialmente digitado. Não há nova thread de heartbeat
por cliente. Heartbeats não alteram peões, barreiras, estoques ou turno.

O servidor usa `System.nanoTime()` e um único `ScheduledExecutorService` para
verificar as conexões a cada **250 ms**, além da verificação antes das chamadas.
Dez segundos sem heartbeat marcam uma sessão registrada como desconectada.
O instante de detecção é a fronteira `último heartbeat + 10 s`, para que atrasos
do agendador não alonguem o prazo. Um `sair` recebido marca a ausência no instante
da notificação. Jogadores ainda não registrados não têm timeout.

A partida pausa preservando todo o tabuleiro, estoque e jogador da vez. Cada
sessão ausente recebe seu próprio prazo de **180 segundos**. O snapshot
`EstadoServico.presencas()` informa ID, conexão e segundos restantes calculados
no servidor (arredondados para cima); nenhum instante monotônico ou token é exposto.
A CLI mostra `PARTIDA PAUSADA`, os ausentes e os tempos em `MM:SS` sem alterar
peões para indicar desconexão. O prazo vale também para participantes já
registrados enquanto aguardam o quarto registro.

Consultas, notificações repetidas e heartbeats de uma sessão já desconectada
não reiniciam esse prazo. Para voltar, é necessário `reconectar(token)` antes
da expiração. A sessão original é preservada; quando todos os registrados estão
conectados, o jogo retoma com o mesmo turno. Com mais de um ausente, retornar
apenas um não retoma a partida. No instante exato de 180 segundos o prazo já expirou.

Quando qualquer prazo expira, o servidor encerra a partida **por abandono, sem
vencedor**, mesmo sem novas consultas dos clientes. `EstadoJogo.getSituacao()`
distingue `EM_ANDAMENTO`, `PAUSADA`, `ENCERRADA_POR_VITORIA` e
`ENCERRADA_POR_ABANDONO`. Neste último caso, `getVencedor()` continua `null` e
`getJogadorAbandono()` informa quem não voltou. Se prazos empatarem, o menor ID
é registrado. Ações, heartbeats e reconexões não reiniciam uma partida abandonada.
Após vitória, não se iniciam prazos de abandono.

O monitor e todos os métodos RMI usam o mesmo `synchronized`: detectar, reconectar,
aplicar jogadas e encerrar são decisões atômicas. Cada operação de presença usa
um único instante do relógio. O `close()` do serviço encerra o monitor e a exportação;
o gancho de encerramento do servidor já chama esse método. Não há persistência
ou substituição de jogadores.

## Comandos da CLI

| Comando | Comportamento |
| --- | --- |
| `ajuda` | Mostra as instruções. |
| `tabuleiro` | Consulta e mostra o estado atual. |
| `mover LINHA COLUNA` | Solicita movimento para uma casa, com coordenadas de **1 a 9**. |
| `barreira h L,L+1 C,C+1` | Pré-visualiza uma barreira horizontal. |
| `barreira v L,L+1 C,C+1` | Pré-visualiza uma barreira vertical. |
| `sessao` | Mostra somente o próprio token. |
| `sair` | Encerra este cliente. |

Exemplos de sintaxe, sujeitos à validação do turno e do tabuleiro no servidor:

```text
mover 2 5
barreira h 3,4 4,5
barreira v 3,4 4,5
```

Comandos aceitam maiúsculas ou minúsculas. As linhas aumentam de cima para baixo;
as colunas, da esquerda para a direita. O desenho usa `1`, `2`, `3`, `4` para
peões, `.` para casas vazias, `=` para horizontais e `|` para verticais.

### Sintaxe e prévia das barreiras

O **primeiro par indica duas linhas** e o **segundo, duas colunas**. Cada par
contém números consecutivos em ordem crescente, de 1 a 9, **sem espaços junto
à vírgula**. O primeiro número de cada par pode ir de 1 a 8.

- `barreira h 3,4 4,5`: separa as linhas **3 e 4**, ocupando as colunas **4 e 5**.
- `barreira v 3,4 4,5`: separa as colunas **4 e 5**, ocupando as linhas **3 e 4**.

Ambos os exemplos são convertidos para a âncora interna `(2,3)`, com orientações
diferentes. Cada barreira bloqueia exatamente **duas passagens entre casas**.
A sintaxe antiga sem pares foi substituída.

Antes de enviar a ação, o cliente cria um snapshot temporário e destaca a proposta
com `*`. Informa as linhas e colunas envolvidas e pergunta:

```text
Confirmar posicionamento? (S/N)
```

`S` envia uma única tentativa ao servidor. `N` cancela sem enviar jogada, gastar
barreira ou avançar o turno. Respostas diferentes mantêm a pergunta. `sair` ou
fim da entrada também descartam a proposta. A prévia não reserva o local e não
garante que a barreira seja legal: a confirmação sempre é validada no servidor.

## Arquitetura e classes principais

```text
Terminal 2: Cliente 1 ─┐
Terminal 3: Cliente 2 ─┼── Java RMI / localhost ── Terminal 1: Servidor
Terminal 4: Cliente 3 ─┤                             Registro + ServicoJogo
Terminal 5: Cliente 4 ─┘                                      │
                                                       Partida → Regras
                                                            │
                                                       EstadoJogo
```

O registro e o serviço estão no **mesmo processo servidor**. O registro é o
diretório de descoberta; o serviço recebe as chamadas referentes à partida.

```text
pom.xml                         Java 21, JUnit, Surefire e perfil de aceitação
.mvn/maven.config               Cache Maven dentro do projeto
src/main/java/br/edu/quoridor/
  modelo/                       Valores e DTOs imutáveis e serializáveis
  jogo/                         Motor das regras
  rmi/                          Contrato e serviço remoto
  servidor/                     Inicialização e encerramento do servidor
  cliente/                      Entrada, comandos e desenho do terminal
src/test/java/br/edu/quoridor/   Testes de modelos, regras, RMI, CLI e aceitação
docs/TESTES_ACEITACAO.md         Mapa da cobertura automatizada e limitações
APRESENTACAO.md                 Roteiro curto e perguntas técnicas
```

| Classe | Responsabilidade |
| --- | --- |
| `Posicao`, `Barreira`, `Orientacao`, `Objetivo`, `Jogador` | Coordenadas, barreiras e dados dos jogadores. |
| `EstadoJogo` | Snapshot de posições, objetivos, barreiras, estoques, turno e vencedor. |
| `EstadoServico` | Reúne o estado do jogo e a quantidade de registros, sem tokens. |
| `Registro` | Resultado do registro/reconexão, com ID, token individual e mensagem. |
| `ResultadoAcao` | Sucesso ou recusa de uma tentativa, com mensagem. |
| `Partida` | Aplica ações válidas, alterna turnos e registra a vitória. |
| `Regras` | Movimentos legais, passagens bloqueadas, conflitos geométricos e BFS. |
| `JogoRemoto` | Interface RMI compartilhada por servidor e clientes. |
| `ServicoJogo` | Uma partida privada, registros, autenticação e acesso sincronizado. |
| `Servidor` | Cria/obtém o registro e publica o serviço com o nome `Quoridor`. |
| `Cliente` | Conecta, identifica a sessão, envia ações e consulta o estado. |
| `ComandoCLI` | Valida sintaxe/intervalos e converte coordenadas. |
| `TabuleiroCLI`, `TerminalCLI` | Desenham snapshots e preservam a linha do prompt. |

## Funcionamento do Java RMI

1. `Servidor` configura o endereço anunciado como `127.0.0.1` e tenta
   `LocateRegistry.createRegistry(1099)`. Se a porta estiver ocupada, obtém uma
   referência ao registro local e verifica a disponibilidade com `list()`.
2. `ServicoJogo` é exportado com `UnicastRemoteObject.exportObject(this, 0)`.
   O RMI escolhe uma porta disponível para o serviço. **1099 é a porta do registro**,
   não a única porta usada pelo RMI.
3. `bind("Quoridor", servico)` publica a referência. Uma segunda instância
   não substitui um serviço de mesmo nome: informa o conflito e libera seus recursos.
4. Cada cliente usa `LocateRegistry.getRegistry(host, porta).lookup("Quoridor")`.
   Recebe um stub que implementa `JogoRemoto` e o utiliza para chamar o servidor.
5. O RMI transporta parâmetros e resultados entre JVMs. Todos os métodos remotos
   declaram `RemoteException`. Os modelos/DTOs retornados implementam `Serializable`,
   e os enums são serializáveis pelo Java. Não é necessário gerar stubs com `rmic`.

O contrato oferece `registrar()`, `reconectar(token)`, `consultarEstado()`,
`mover(token, linha, coluna)`, `colocarBarreira(token, linha, coluna, orientacao)`,
`heartbeat(token)` e `desconectar(token)`.
Os parâmetros de ações usam índices internos a partir de zero.

### Autoridade, sessões e concorrência

O servidor é a única autoridade sobre posições, barreiras, turno e vitória.
Os clientes validam a entrada e apresentam cópias; não enviam um estado completo
para substituir o oficial.

`ServicoJogo` cria sua própria `Partida`, mantém o mapa privado de sessões e gera
tokens com `UUID.randomUUID()`. A ação não recebe um ID escolhido pelo cliente:
o ID é obtido pelo token. Quatro participantes precisam estar registrados para
agir. A posse do token representa a identidade da sessão; não há senha adicional.

Todos os métodos remotos do serviço são `synchronized` no mesmo objeto.
O monitor periódico também usa esse bloqueio. Autenticar, verificar os registros
e aplicar a ação acontecem na mesma seção
crítica. Consultas e registros usam esse mesmo monitor. A proteção concorrente
fica no serviço: `Partida` não é oferecida diretamente aos clientes.

`Partida` só troca o estado após validar a ação inteira. Ações recusadas mantêm
o estado anterior. `EstadoJogo` copia listas com `List.copyOf`; listas e elementos
são imutáveis. O RMI entrega cópias independentes, e snapshots antigos permanecem
inalterados depois de novas jogadas.

### Atualizações dos clientes

Cada cliente consulta o servidor aproximadamente a cada segundo, sem callbacks.
Um laço único faz RPC e escreve na tela; uma thread daemon lê comandos e os
coloca numa fila. A espera na fila tem prazo até a próxima consulta ou heartbeat, permitindo
atualizações mesmo sem pressionar Enter. Comandos podem provocar consultas adicionais.

O conteúdo é comparado ao desenho anterior para evitar redesenhos periódicos
sem mudanças. No terminal integrado do VS Code, o cliente detecta o emulador mesmo
quando `System.console()` retorna `null`, e atualiza o painel em vez de imprimir
várias cópias do tabuleiro. O painel usa 23 linhas e o prompt fica na linha 24;
sequências ANSI salvam/restauram o cursor. Amplie o terminal para pelo menos
80 colunas e 24 linhas; áreas menores podem provocar rolagem e prejudicar o desenho.

Para desativar sequências ANSI (por exemplo, ao redirecionar a saída para um log),
execute o cliente com:

```powershell
java -Dquoridor.terminal=texto -cp target/classes br.edu.quoridor.cliente.Cliente
```

Para forçar o painel caso a detecção automática do terminal falhe, execute:

```powershell
java -Dquoridor.terminal=painel -cp target/classes br.edu.quoridor.cliente.Cliente
```

A opção padrão é `auto`.
O modo texto é apropriado a logs e testes e registra uma nova cópia do tabuleiro
cada vez que o estado muda. Após vitória ou abandono, consultas e heartbeats periódicos param; comandos de consulta
e saída continuam disponíveis. A saída do cliente encerra o laço de atualização.

## Regras e turnos

O tabuleiro possui **9×9 casas**, quatro peões e cinco barreiras por jogador.
Internamente, casas usam índices de 0 a 8 e âncoras de barreiras, de 0 a 7.

| Jogador | Posição interna inicial | Posição na CLI | Objetivo interno | Objetivo na CLI |
| --- | --- | --- | --- | --- |
| 1 | (0,4) | (1,5) | Linha 8 | Linha 9 |
| 2 | (4,8) | (5,9) | Coluna 0 | Coluna 1 |
| 3 | (8,4) | (9,5) | Linha 0 | Linha 1 |
| 4 | (4,0) | (5,1) | Coluna 8 | Coluna 9 |

O jogador inicial é sorteado por `Partida`; nos testes, a origem do sorteio pode
ser controlada. A ordem é circular: **1 → 2 → 3 → 4 → 1**, começando no sorteado.
Cada turno aceita um movimento ou uma colocação válida de barreira. Uma tentativa
inválida não avança o turno. Cancelar a prévia também não constitui jogada.

- Movimento normal: uma casa ortogonal, sem atravessar barreiras ou ocupar outro peão.
- Peão adjacente acessível: pode ser ultrapassado por salto direto se a casa atrás
  estiver dentro do tabuleiro, vazia e acessível.
- Quando esse salto é impedido por barreira, borda ou outro peão, podem ser usadas
  as casas laterais ao adversário, se estiverem livres e acessíveis. Essas são
  as diagonais permitidas; não há saltos em cadeia sobre dois peões.
- Barreiras bloqueiam dois segmentos. São proibidos sobreposição total/parcial,
  cruzamento e eliminação de caminhos. Extremidades podem se encostar, inclusive
  em encontros em T sem conflito. O estoque diminui apenas após uma ação aceita.
- Vence o primeiro jogador a alcançar qualquer casa do lado oposto. O vencedor
  fica registrado, a partida termina e novas ações são recusadas. Não há novo
  turno após a vitória.

### Busca em largura — BFS

Antes de confirmar uma nova barreira, `Regras.podeColocarBarreira` verifica a
geometria e cria uma lista temporária com a proposta. Para cada jogador:

1. Coloca sua posição em uma fila `ArrayDeque` e marca a casa como visitada.
2. Retira a próxima casa e verifica se ela pertence ao objetivo do jogador.
3. Adiciona vizinhas ortogonais dentro do tabuleiro, ainda não visitadas e sem
   barreira no acesso. Usa a mesma verificação de passagens da movimentação.
4. Retorna sucesso ao alcançar o objetivo; retorna falha se a fila ficar vazia.

A BFS **ignora outros peões**, pois sua ocupação é temporária. Se algum dos quatro
jogadores ficar sem caminho, a barreira é rejeitada antes de alterar estoque,
barreiras ou turno. A visita é limitada às 81 casas; a implementação percorre
a lista de barreiras ao verificar cada passagem.

## Tratamento de erros

Comandos desconhecidos, formato incorreto, pares não consecutivos e coordenadas
fora dos limites produzem mensagens locais em português. O servidor recusa
tokens inválidos, partida incompleta, ações fora do turno, movimentos ilegais,
barreiras conflitantes, falta de estoque e ações após o término.

Recusas de jogo retornam `ResultadoAcao(false, mensagem)`. Registro recusado
retorna `Registro` com sucesso falso, ID 0 e token nulo. Falhas de comunicação
usam `RemoteException`. O cliente trata servidor ausente, nome remoto não publicado
e perda de conexão, orientando a reiniciar e reconectar com o token. Não reenvia
ações automaticamente, pois uma resposta perdida não prova que a ação falhou.

O cliente define limites de cinco segundos para resposta e handshake do RMI do
JDK, respeitando valores fornecidos com `-Dsun.rmi.transport.tcp.responseTimeout`
e `-Dsun.rmi.transport.tcp.handshakeTimeout`. O servidor também trata falha na
publicação e porta 1099 ocupada por algo que não seja um registro RMI válido.

## Testes automatizados

Validação completa, sem interação humana:

```sh
mvn clean verify
```

Seleção de aceitação e integração:

```sh
mvn -Paceitacao clean verify
```

Na última validação completa informada, a suíte aprovou **279 casos**. O perfil
`aceitacao` executa um subconjunto desses testes; confira o total exato no
relatório do Maven após cada execução.
Incluem modelos, regras, BFS, concorrência, cópias serializadas, sintaxe de
barreiras, prévia/confirmar/cancelar, partidas completas, reconexão e quatro JVMs
clientes reais. O perfil reutiliza testes existentes e os três roteiros de
`AceitacaoJogoTest`; não adiciona funcionalidades de teste à aplicação.

Os testes rodam pelo Surefire na fase `test`; `verify` também passa pelo
empacotamento. Os relatórios ficam em `target/surefire-reports/`. O segundo
comando com `clean` substitui os relatórios da execução anterior.

Os testes novos simulam os limites de 10 e 180 segundos por relógio monotônico
controlado, sem aguardar três minutos. Incluem dois ausentes, reconexões concorrentes,
novos DTOs por RMI real, heartbeat com digitação pendente e encerramento do monitor.

Há timeouts JUnit e RMI, espera por eventos com limite e limpeza de serviços,
registros e processos. Os testes não exigem que o servidor de demonstração
esteja aberto e não disputam sua porta 1099. Consulte a
[análise detalhada da cobertura](docs/TESTES_ACEITACAO.md).

### Cache Maven e arquivos de entrega

`.mvn/maven.config` configura `.m2/repository` como cache local, dentro do projeto.
Execute o Maven na raiz. `.m2/` e `target/` estão no `.gitignore`, assim como arquivos
Java gerados, logs e configurações locais de IDE. Não precisam acompanhar a entrega
do código-fonte; serão recriados pelo Maven. Não há temporários versionados.

Neste ambiente de desenvolvimento, foi necessário preparar `.m2/cacerts` com uma
cópia do armazenamento de certificados do Java e o certificado do antivírus já
confiado pelo Windows. Esse arquivo é local e ignorado pelo Git. Caso exista e
seja necessário para novos downloads nesta máquina, pode ser informado assim:

```sh
mvn "-Djavax.net.ssl.trustStore=.m2/cacerts" clean verify
```

Não é requisito geral do projeto. Em outra máquina, use os comandos normais e
os certificados confiáveis daquele ambiente; não foi alterada a instalação global.

## Limitações e demonstração final

- Uma única partida em memória, sem persistência, recuperação de token perdido,
  substituição de participantes, reinício pelo cliente ou tolerância a falha do servidor.
- Sair não libera a vaga: há 180 segundos para reconectar após a detecção.
  Expirado qualquer prazo, termina a partida inteira sem vencedor. Quem conhece
  um token pode usar aquela sessão; não há TLS configurado nem autenticação adicional.
- Atualização por polling, com atraso aproximado de um segundo; não há envio
  instantâneo de eventos, ordenação por versão ou repetição idempotente de ações.
- O nome digitado não é distribuído aos demais jogadores; os peões usam os IDs.
- Painel ANSI com tamanho mínimo fixo; redimensionamento e edição da linha no
  PowerShell precisam de uma conferência visual na máquina da apresentação.
- Os testes de processos usam pipes e hospedam registro/serviço na JVM de testes.
  A exibição interativa e os cinco terminais da demonstração merecem um ensaio curto.
- A suíte cobre cenários determinísticos, não todas as partidas possíveis nem
  carga prolongada ou execução em máquinas distintas.

O [roteiro de apresentação](APRESENTACAO.md) propõe uma demonstração curta da
arquitetura, dos cinco processos, de movimentos, barreiras, BFS e concorrência,
com perguntas técnicas e respostas baseadas neste código.
