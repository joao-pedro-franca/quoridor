# Quoridor RMI

Projeto universitário de Sistemas Distribuídos. O objetivo final é um Quoridor
para quatro jogadores, com um processo servidor e quatro processos clientes
em localhost, usando Java RMI e interface de terminal.

## Etapa atual: 4 — clientes de terminal

Implementado:

- Projeto Maven com Java 21 e JUnit 5.
- Modelos de posição, jogador, objetivo, orientação, barreira e estado do jogo.
- Movimentos ortogonais, saltos e diagonais para quatro jogadores.
- Barreiras com validação de sobreposição, cruzamento e caminhos por BFS.
- Sorteio do jogador inicial, alternância circular de turnos e controle de estoque.
- Identificação do vencedor e rejeição de ações após o término.
- Estados imutáveis e ações rejeitadas sem efeitos sobre a partida.
- Testes dos modelos, das regras e do controle da partida.
- Serviço RMI com uma única partida autoritativa e acesso sincronizado.
- Registro de quatro jogadores, tokens de sessão e reconexão.
- Servidor executável em localhost e testes de integração com RMI real.
- Clientes CLI independentes, tabuleiro ASCII, comandos e reconexão por token.
- Consultas periódicas e testes automatizados com quatro JVMs clientes simultâneas.

A etapa final ainda não foi iniciada.

## Requisitos e testes

Instale o JDK 21 e o Maven e confira se ambos estão no `PATH`.
Na pasta do projeto, execute:

```sh
mvn clean test
```

Para a validação completa, incluindo o empacotamento, use `mvn clean verify`.
Para repetir somente os cenários de aceitação e integração já existentes junto
dos novos roteiros, use `mvn -Paceitacao clean verify`.
O [mapa de cobertura e os roteiros de aceitação](docs/TESTES_ACEITACAO.md)
explicam o que cada conjunto verifica e quais conferências ainda são manuais.

O Maven baixa os plugins e o JUnit na primeira execução. Os relatórios dos testes
ficam em `target/surefire-reports/`.
O arquivo `.mvn/maven.config` mantém o cache de dependências em `.m2/repository`
dentro do projeto, pois o cache padrão deste ambiente não tem permissão de escrita.
Esse cache é ignorado pelo Git; execute os comandos na pasta raiz do projeto.

Nesta máquina, o antivírus inspeciona conexões HTTPS e seu certificado não está
no armazenamento padrão do Java. Para baixar as dependências na validação inicial,
foi usada uma cópia local desse armazenamento em `.m2/cacerts`, acrescida do
certificado do antivírus já confiado pelo Windows:

```sh
mvn "-Djavax.net.ssl.trustStore=.m2/cacerts" clean test
```

Esse arquivo é local, ignorado pelo Git e não é necessário em ambientes sem esse
problema de certificado. Nenhuma instalação global foi alterada.

Após a revisão da cobertura, `mvn clean verify` executou **232 testes**, sem
falhas, erros ou testes ignorados. Os 229 casos existentes foram preservados e
acrescentados três roteiros de aceitação combinando CLI e RMI. O perfil
`aceitacao` seleciona 45 testes, reutilizando os testes de integração e processos.

## Organização

```text
src/main/java/br/edu/quoridor/
  modelo/    modelos do jogo, ResultadoAcao, Registro e EstadoServico
  jogo/      Partida e Regras
  rmi/       JogoRemoto e ServicoJogo
  servidor/  Servidor
  cliente/   Cliente, TabuleiroCLI e auxiliares ComandoCLI e TerminalCLI
src/test/java/br/edu/quoridor/
  modelo/    testes dos modelos
  jogo/      testes de movimentos, barreiras, BFS, turnos e vitória
  rmi/       testes do serviço, concorrência e integração real
  cliente/   testes dos comandos, tabuleiro, terminal e clientes em processos reais
```

O motor de regras e o serviço RMI foram reutilizados sem mudanças na Etapa 4.

## Iniciar e encerrar o servidor

Na raiz do projeto:

```sh
mvn clean package
java -cp target/classes br.edu.quoridor.servidor.Servidor
```

O servidor anuncia `rmi://127.0.0.1:1099/Quoridor` e permanece aguardando jogadores.
Para encerrá-lo, pressione **Ctrl+C** no mesmo terminal. Não é preciso iniciar
`rmiregistry` separadamente. O comando de execução usa somente o JDK, sem
dependências adicionais de produção nem mudanças no `pom.xml`.

Se for necessário baixar novos plugins nesta máquina e aparecer erro de
certificado, use o armazenamento local descrito acima:

```sh
mvn "-Djavax.net.ssl.trustStore=.m2/cacerts" clean package
```

`LocateRegistry.createRegistry(1099)` cria o registro que associa nomes a
referências remotas. Se a porta estiver ocupada, o servidor tenta usar um registro
RMI existente em localhost e verifica sua disponibilidade com `list()`.
Se não houver registro válido, informa a falha. A publicação usa `bind("Quoridor", ...)`:
uma segunda instância informa o conflito e não substitui o serviço existente.

O serviço é exportado com `UnicastRemoteObject.exportObject(this, 0)`.
O RMI escolhe uma porta livre para suas chamadas; **1099 é a porta de descoberta
do registro**, não a única porta usada pelo RMI. As referências anunciadas pelo
servidor usam `127.0.0.1`. Toda comunicação é feita pelas APIs RMI.

Ao encerrar normalmente com Ctrl+C, um gancho remove a publicação e libera
o serviço exportado. O registro é encerrado somente se tiver sido criado por
este processo; um registro externo é preservado. Os registros dos jogadores e
a partida ficam somente em memória e são perdidos ao encerrar o servidor.

## Iniciar os quatro clientes

Mantenha o servidor em um terminal do VS Code. Abra **outros quatro terminais
PowerShell**, todos na raiz do projeto, e execute em cada um:

```sh
java -cp target/classes br.edu.quoridor.cliente.Cliente
```

Cada execução é um processo Java independente. Por padrão, o cliente conecta ao
registro em `127.0.0.1:1099` e faz `lookup("Quoridor")`. Opcionalmente, pode-se informar
host e porta, sem alterar a configuração padrão do servidor:

```sh
java -cp target/classes br.edu.quoridor.cliente.Cliente 127.0.0.1 1099
```

Informe seu nome e pressione Enter no campo de token para registrar um novo
jogador. O nome é um apelido local, pois a API do servidor identifica jogadores
por ID e token. O cliente informa o ID atribuído pelo servidor e mostra apenas
o próprio token. Guarde-o; o comando `sessao` permite consultá-lo novamente.

Até quatro registros, o tabuleiro informa quantos participantes estão presentes.
Quando os quatro se registram, o jogo libera as ações no turno sorteado pelo
servidor. Um quinto registro é recusado. Cada cliente mostra seu ID, o turno,
os estoques e se é sua vez. A vitória é anunciada nos quatro terminais.

Para sair, digite `sair`, pressione Ctrl+C ou encerre a entrada. A vaga não é
liberada: para retornar, execute o cliente novamente e informe seu token quando
solicitado. Reconectar preserva o ID e não consome uma nova vaga. Token inválido
é recusado, sem registrar outro jogador automaticamente.

## Comandos e coordenadas da CLI

| Comando | Função |
| --- | --- |
| `ajuda` | Mostra os comandos e a convenção das âncoras. |
| `tabuleiro` | Consulta e mostra o estado atual. |
| `mover LINHA COLUNA` | Solicita movimento para uma casa, usando números de **1 a 9**. |
| `barreira H L,L+1 C,C+1` | Pré-visualiza uma barreira horizontal entre as linhas do primeiro par. |
| `barreira V L,L+1 C,C+1` | Pré-visualiza uma barreira vertical entre as colunas do segundo par. |
| `sessao` | Mostra somente o próprio token para reconexão. |
| `sair` | Encerra o cliente. |

Exemplos:

```text
mover 2 5
barreira h 3,4 4,5
barreira v 3,4 4,5
```

Cada par deve conter dois números consecutivos em ordem crescente, de **1 a 9**,
sem espaços ao redor da vírgula. O primeiro par indica linhas; o segundo, colunas.
A âncora interna é calculada subtraindo 1 do primeiro número de cada par:

- **H:** separa as linhas `L` e `L+1`, nas colunas `C` e `C+1`.
- **V:** separa as colunas `C` e `C+1`, nas linhas `L` e `L+1`.

Assim, `barreira h 3,4 4,5` separa as linhas 3 e 4 nas colunas 4 e 5.
`barreira v 3,4 4,5` separa as colunas 4 e 5 nas linhas 3 e 4.
Ambos usam a âncora interna `(2,3)`, com orientações diferentes.
O desenho usa `=` para barreiras horizontais e `|` para verticais, estendendo
cada barreira pelos dois segmentos. Peões são `1`, `2`, `3`, `4`; casas vazias são `.`.

Antes de enviar a ação, o cliente desenha um snapshot temporário com a proposta
destacada por `*`, informa as linhas e colunas envolvidas e pergunta
**Confirmar posicionamento? (S/N)**. `S` envia uma única tentativa ao servidor;
`N` cancela sem enviar jogada, consumir estoque ou avançar turno. Uma resposta
diferente mantém a pergunta. A atualização periódica preserva a prévia até a
resposta; `sair` ou o fim da entrada também descartam a proposta.
Essa prévia é somente visual: a legalidade continua sendo verificada pelo servidor
após a confirmação. A sintaxe antiga sem pares foi substituída.

O cliente subtrai 1 das coordenadas antes de enviar as ações. As seções sobre
modelos e regras abaixo continuam usando os **índices internos a partir de 0**.
Comandos não diferenciam maiúsculas de minúsculas. O cliente valida somente a
sintaxe e os intervalos; turno, estoque, saltos, barreiras, caminhos e vitória
continuam sendo decididos exclusivamente pelo servidor. As mensagens de recusa
do serviço são exibidas em português.

## Atualizações e leitura do terminal

O cliente faz uma consulta aproximadamente a cada **um segundo**, usando somente
RMI, sem callbacks. Um laço único processa comandos, realiza as chamadas e escreve
na tela. Uma thread daemon lê linhas de entrada e as coloca em uma fila. O laço
espera nessa fila apenas até a próxima consulta; assim, a atualização não depende
de o usuário pressionar Enter. Não há consultas simultâneas ou tarefas periódicas
acumuladas dentro do mesmo cliente.

Depois de cada consulta, compara-se o conteúdo visual do novo snapshot com o
anterior. O tabuleiro só é redesenhado quando há mudanças ou uma solicitação
explícita do usuário. Ao terminar a partida, as consultas periódicas cessam,
mas os comandos `ajuda`, `tabuleiro`, `sessao` e `sair` continuam disponíveis.

No PowerShell interativo, use um terminal com pelo menos **80 colunas e 24 linhas**.
O painel ocupa as primeiras 23 linhas; a linha 24 é reservada ao comando. As
sequências ANSI salvam e restauram o cursor durante atualizações do painel,
preservando a linha em edição. Elas são usadas somente quando há console real.
Com entrada ou saída redirecionada, o cliente usa texto simples, adequado a pipes
e aos testes automatizados. Não é usada biblioteca externa de terminal.

Falhas de comunicação encerram o laço com uma mensagem e instrução de reconexão;
ações não são reenviadas automaticamente. No RMI do JDK, o cliente configura
limites de cinco segundos para resposta e handshake, respeitando valores
explicitamente fornecidos com `-Dsun.rmi.transport.tcp.responseTimeout` e
`-Dsun.rmi.transport.tcp.handshakeTimeout`. Ao sair, a leitura é interrompida;
como é daemon, uma leitura nativa pendente não impede o encerramento da JVM.
Não há executor de atualização que permaneça ativo depois da saída.

## Testes dos clientes

- `ComandoCLITest`: comandos, todas as conversões de coordenadas e entradas inválidas.
- `TabuleiroCLITest`: peões, eixos, barreiras de dois segmentos, limites, estoques e vencedor.
- `TerminalCLITest`: supressão de redesenhos e preservação do cursor do prompt.
- `ClienteIntegracaoTest`: comandos enviados por RMI, erros, reconexão, snapshots,
  saída por EOF e uma partida completa conduzida por quatro instâncias de cliente.
- `ClientesProcessosTest`: quatro JVMs executando o `main` real, atualização sem
  novas entradas, movimento, barreira, recusa do quinto processo, reconexão e
  encerramento após perda da conexão enquanto a leitura está pendente.

Os testes de processos usam um registro RMI em uma porta disponível diferente
de 1099 e controlam os clientes pelos pipes de entrada e saída. Esperam eventos
observáveis com limite de tempo, sem exigir temporizações exatas. Processos e
recursos RMI criados pelos testes são liberados ao terminar.

## Interface remota e sessões

`JogoRemoto` estende `Remote`; todos os métodos declaram `RemoteException`.
Os parâmetros de ações usam coordenadas inteiras, para que o servidor possa
recusar coordenadas inválidas com `ResultadoAcao`.

| Método | Retorno e comportamento |
| --- | --- |
| `registrar()` | `Registro` com sucesso, ID de 1 a 4, token e mensagem; recusa o quinto registro. |
| `reconectar(token)` | Recupera a mesma sessão, sem ocupar nova vaga nem alterar o jogo. |
| `consultarEstado()` | `EstadoServico`, contendo `jogo()` e `jogadoresRegistrados()`. |
| `mover(token, linha, coluna)` | `ResultadoAcao` da movimentação do jogador associado ao token. |
| `colocarBarreira(token, linha, coluna, orientacao)` | `ResultadoAcao` da tentativa de colocar uma barreira. |

Em `Registro`, uma recusa tem `sucesso() == false`, `jogadorId() == 0` e
`token() == null`. Em ações, erros de sessão, espera por participantes e jogadas
inválidas retornam `ResultadoAcao(false, mensagem)`. `RemoteException` indica
falha na comunicação, não uma jogada recusada.

O servidor atribui os IDs pela ordem dos registros e gera tokens com
`UUID.randomUUID()`, independentemente da origem de aleatoriedade dos turnos.
As chamadas de ação não possuem parâmetro de ID: o serviço o encontra no mapa
privado de sessões. Tokens inválidos são recusados e nenhuma jogada é aceita
antes do quarto registro. A validação de turno e das regras é delegada a `Partida`.

A reconexão exige guardar e reenviar o token original. Ela funciona inclusive
com as quatro vagas ocupadas e preserva a identidade durante a vida desse
servidor. Não há recuperação de token perdido nem persistência entre reinícios.

`EstadoServico` reúne a quantidade de participantes e um `EstadoJogo`, que já
contém posições, objetivos, estoques, barreiras, turno e vencedor. Nenhum dos dois
contém tokens. Os DTOs e os modelos retornados implementam `Serializable`;
os enums são serializáveis pelo próprio Java. O RMI entrega cópias independentes
ao cliente, mantendo as listas e seus elementos imutáveis.

## Proteção do estado compartilhado

`ServicoJogo` mantém uma `Partida` privada criada por ele, sem expô-la nem aceitar
uma instância externa mutável. Todos os métodos de registro, reconexão, consulta
e ação são `synchronized` no mesmo objeto. Assim, autenticar, verificar os quatro
registros e executar uma jogada ocorrem na mesma seção crítica. O monitor também
protege o mapa de sessões, a contagem de jogadores e as consultas.

O motor de regras da Etapa 2 permanece inalterado. Ele só substitui seu estado
depois de validar a ação inteira. O serviço consulta o estado e a contagem de
registros sob o mesmo bloqueio, formando um retrato consistente. Depois de
retornado, esse retrato não muda, mesmo que outra chamada aplique uma jogada.

## Testes do serviço RMI

- `ServicoJogoTest`: registro, limite de vagas, tokens, espera por participantes,
  turno, ações válidas e inválidas, reconexão, snapshots e término da partida.
- `ConcorrenciaRmiTest`: registros e ações simultâneas, com consultas que verificam
  a consistência entre estoque, barreiras e turno.
- `IntegracaoRmiTest`: quatro testes com registro RMI real, `lookup` através de
  um stub, DTOs serializados e chamadas remotas, incluindo concorrência e encerramento.
- `ApoioRmi`: auxiliar de testes para controlar o turno inicial e iniciar chamadas
  concorrentes com `CountDownLatch`, sem depender de pausas arbitrárias.

Os testes usam a porta **0**, deixando o RMI escolher portas temporárias sem
disputar a 1099 do servidor de demonstração. Todos os serviços e registros
criados pelos testes são desexportados ao terminar.

## Coordenadas e configuração inicial

As posições usam `(linha, coluna)`, com índices de **0 a 8**. A linha aumenta
de cima para baixo; a coluna aumenta da esquerda para a direita.

| Jogador | Posição inicial | Objetivo | Barreiras disponíveis |
| --- | --- | --- | --- |
| 1 | (0, 4) | Linha 8 | 5 |
| 2 | (4, 8) | Coluna 0 | 5 |
| 3 | (8, 4) | Linha 0 | 5 |
| 4 | (4, 0) | Coluna 8 | 5 |

`Partida()` sorteia o primeiro jogador entre 1 e 4. `Partida(RandomGenerator)`
permite controlar a origem do sorteio, inclusive usando `new Random(semente)`.
Após ações válidas, a ordem é circular: 1 → 2 → 3 → 4 → 1.
Não há barreiras colocadas no tabuleiro inicialmente.

O construtor `EstadoJogo()` mantém o retrato inicial com turno 1 da Etapa 1;
o sorteio é responsabilidade de `Partida`, que cria o retrato com o turno sorteado.

Uma barreira possui âncora `(linha, coluna)` com ambos os índices de **0 a 7**:

- **Horizontal:** separa as linhas `linha` e `linha + 1` nas colunas `coluna`
  e `coluna + 1`, cobrindo dois segmentos.
- **Vertical:** separa as colunas `coluna` e `coluna + 1` nas linhas `linha`
  e `linha + 1`, cobrindo dois segmentos.

O modelo valida os limites da âncora e a orientação obrigatória. O motor valida
sobreposição total ou parcial, cruzamento e preservação de caminhos. Barreiras
na mesma direção podem se encostar pelas extremidades. Um encontro em T é
permitido quando não há sobreposição de segmentos nem cruzamento no centro.

## Como o motor funciona

`Regras.movimentosValidos` examina as quatro casas ortogonais ao peão. Uma casa
vazia e sem barreira no acesso permite movimento normal. Se houver um adversário
adjacente e acessível, examina a casa imediatamente atrás dele. Se essa casa
estiver livre e acessível, permite o salto direto e não as diagonais por esse peão.

Se o salto direto estiver impedido por barreira, borda ou outro peão, examina
as duas casas laterais ao adversário. A diagonal exige uma casa lateral vazia
e uma passagem livre entre o adversário e essa casa. Nenhuma jogada salta dois
peões, ocupa a casa de outro jogador ou atravessa uma barreira.

`Regras.passagemBloqueada` representa os dois segmentos de cada barreira e é
compartilhada pela movimentação e pela BFS. Para uma nova barreira, primeiro
o motor verifica conflitos geométricos. Em seguida, adiciona a barreira a uma
lista temporária e executa BFS a partir da posição de cada jogador até alguma
casa de seu objetivo. A BFS ignora a ocupação por peões. Se qualquer jogador
perder seu caminho, a barreira é rejeitada.

`Partida` valida o término, o autor e o turno. Colocar barreira também exige
estoque disponível. Somente após todas as validações cria um novo `EstadoJogo`;
assim, uma ação inválida mantém exatamente o mesmo estado, sem gastar barreira
nem avançar o turno. Um movimento que alcança o objetivo registra o vencedor
e encerra a partida, mantendo o turno no vencedor. Todas as ações seguintes
são rejeitadas.

As regras são baseadas no [manual publicado pela Gigamic](https://pakufuda.store/wp-content/uploads/2022/08/Quoridor_rules_eng.pdf),
incluindo a proibição de saltar mais de um peão na variante para quatro jogadores.

## Uso do motor em Java

```java
Partida partida = new Partida();
EstadoJogo antes = partida.getEstado();
int jogador = antes.getJogadorDaVez();
var destinos = Regras.movimentosValidos(antes, jogador);

// O conjunto não tem uma ordem definida; cada elemento é um destino permitido.
if (!destinos.isEmpty()) {
    Posicao destino = destinos.iterator().next();
    ResultadoAcao resultado = partida.mover(jogador, destino);
    boolean aceita = resultado.sucesso();
    String explicacao = resultado.mensagem();
}
```

As classes do exemplo estão nos pacotes `br.edu.quoridor.jogo` e
`br.edu.quoridor.modelo`. Também estão disponíveis:

- `mover(jogadorId, linha, coluna)`;
- `colocarBarreira(jogadorId, barreira)`;
- `colocarBarreira(jogadorId, linha, coluna, orientacao)`;
- `getEstado().isFinalizada()` e `getEstado().getVencedor()`.

Os métodos de ação retornam `ResultadoAcao`, sem lançar exceção para tentativas
inválidas. As sobrecargas com coordenadas rejeitam valores fora dos limites;
construir diretamente um modelo inválido continua lançando exceção. O vencedor
é `null` enquanto a partida está em andamento.

## Cenários de teste

`Cenarios`, somente em `src/test`, monta retratos imutáveis de situações difíceis.
O construtor `Partida(EstadoJogo)` tem acesso de pacote e permite usá-los nos
testes, sem reflexão, setters ou métodos públicos para reposicionar jogadores.
Os construtores públicos de `Partida` sempre iniciam uma partida normal.

A suíte inclui cercamentos individuais dos quatro jogadores, desvios por uma
única passagem, corredores ocupados por peões, saltos nas quatro direções,
diagonais bloqueadas, consumo das cinco barreiras em turnos reais e vitória
por movimento normal, salto e diagonal.

## Decisões de implementação

`Posicao`, `Barreira` e `ResultadoAcao` são records: valores imutáveis com comparação
por conteúdo. `Jogador` e `EstadoJogo` possuem campos privados e finais, sem setters.
`EstadoJogo` copia as listas recebidas com `List.copyOf`, impedindo alterações
externas; seus elementos também são imutáveis. Retratos obtidos antes de uma
jogada permanecem inalterados depois dela. `Partida` mantém a referência ao
retrato atual e só a substitui após uma ação aceita.

Os objetivos são valores de um enum; a interpretação deles pertence a `Regras`.
Apenas o JUnit é usado como dependência, no escopo de testes. Não são usados
Spring Boot, banco de dados, Socket ou ServerSocket.
