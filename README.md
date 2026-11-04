# Quoridor RMI

Projeto universitário de Sistemas Distribuídos. O objetivo final é um Quoridor
para quatro jogadores, com um processo servidor e quatro processos clientes
em localhost, usando Java RMI e interface de terminal.

## Etapa atual: 2 — motor de regras

Implementado:

- Projeto Maven com Java 21 e JUnit 5.
- Modelos de posição, jogador, objetivo, orientação, barreira e estado do jogo.
- Movimentos ortogonais, saltos e diagonais para quatro jogadores.
- Barreiras com validação de sobreposição, cruzamento e caminhos por BFS.
- Sorteio do jogador inicial, alternância circular de turnos e controle de estoque.
- Identificação do vencedor e rejeição de ações após o término.
- Estados imutáveis e ações rejeitadas sem efeitos sobre a partida.
- Testes dos modelos, das regras e do controle da partida.

Não há ainda aplicação executável, servidor RMI, clientes ou interface CLI.

## Requisitos e testes

Instale o JDK 21 e o Maven e confira se ambos estão no `PATH`.
Na pasta do projeto, execute:

```sh
mvn clean test
```

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

Na Etapa 2, `mvn clean test` executou **118 testes**, todos aprovados: 18 testes
da etapa anterior e 100 novos casos. O teste anterior que exige o turno inicial
do jogador 1 usa agora uma partida controlada, pois partidas comuns sorteiam o início.

## Organização

```text
src/main/java/br/edu/quoridor/
  modelo/    Posicao, Jogador, Objetivo, Orientacao, Barreira, EstadoJogo e ResultadoAcao
  jogo/      Partida e Regras
  rmi/       reservado para JogoRemoto e ServicoJogo
  servidor/  reservado para Servidor
  cliente/   reservado para Cliente e TabuleiroCLI
src/test/java/br/edu/quoridor/
  modelo/    testes dos modelos
  jogo/      testes de movimentos, barreiras, BFS, turnos e vitória
```

Os pacotes reservados contêm somente `package-info.java`. `Registro` será
acrescentado na etapa de comunicação, quando sua funcionalidade for necessária.

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
