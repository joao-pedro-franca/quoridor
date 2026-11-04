# Quoridor RMI

Projeto universitário de Sistemas Distribuídos. O objetivo final é um Quoridor
para quatro jogadores, com um processo servidor e quatro processos clientes
em localhost, usando Java RMI e interface de terminal.

## Etapa atual: 1 — modelos e estado inicial

Implementado:

- Projeto Maven com Java 21 e JUnit 5.
- Modelos de posição, jogador, objetivo, orientação, barreira e estado do jogo.
- Classe `Partida`, que disponibiliza o estado inicial.
- Testes de inicialização, limites de coordenadas e encapsulamento.

Não há ainda aplicação executável, servidor RMI, clientes, movimentos, saltos,
diagonais, colocação de barreiras, BFS ou verificação de vitória.

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
problema de certificado. Nenhuma instalação global foi alterada. A validação
executou **18 testes**, todos aprovados.

## Organização

```text
src/main/java/br/edu/quoridor/
  modelo/    Posicao, Jogador, Objetivo, Orientacao, Barreira e EstadoJogo
  jogo/      Partida
  rmi/       reservado para JogoRemoto e ServicoJogo
  servidor/  reservado para Servidor
  cliente/   reservado para Cliente e TabuleiroCLI
src/test/java/br/edu/quoridor/
  modelo/    testes dos modelos
  jogo/      testes do estado inicial da partida
```

Os pacotes reservados contêm somente `package-info.java`. `Regras`, `Registro`
e `ResultadoAcao` serão acrescentados quando suas funcionalidades forem necessárias.

## Coordenadas e configuração inicial

As posições usam `(linha, coluna)`, com índices de **0 a 8**. A linha aumenta
de cima para baixo; a coluna aumenta da esquerda para a direita.

| Jogador | Posição inicial | Objetivo | Barreiras disponíveis |
| --- | --- | --- | --- |
| 1 | (0, 4) | Linha 8 | 5 |
| 2 | (4, 8) | Coluna 0 | 5 |
| 3 | (8, 4) | Linha 0 | 5 |
| 4 | (4, 0) | Coluna 8 | 5 |

O jogador 1 começa. Não há barreiras colocadas no tabuleiro inicialmente.

Uma barreira possui âncora `(linha, coluna)` com ambos os índices de **0 a 7**:

- **Horizontal:** separa as linhas `linha` e `linha + 1` nas colunas `coluna`
  e `coluna + 1`, cobrindo dois segmentos.
- **Vertical:** separa as colunas `coluna` e `coluna + 1` nas linhas `linha`
  e `linha + 1`, cobrindo dois segmentos.

O modelo valida apenas os limites da âncora e a orientação obrigatória.
Sobreposição, cruzamento e preservação de caminhos pertencem à próxima etapa
de regras e ainda não são verificados.

## Decisões de implementação

`Posicao` e `Barreira` são records: valores imutáveis com comparação por conteúdo
e acessores como `linha()` e `coluna()`. As demais classes têm campos privados
e finais, sem setters. As listas de `EstadoJogo` são criadas com `List.of`,
impedindo alterações externas; seus elementos também são imutáveis.

Os objetivos são valores de um enum, sem implementar a detecção de vitória.
Apenas o JUnit é usado como dependência, no escopo de testes. Não são usados
Spring Boot, banco de dados, Socket ou ServerSocket.
