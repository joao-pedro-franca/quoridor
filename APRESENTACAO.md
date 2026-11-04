# Quoridor RMI — roteiro de apresentação

Roteiro sugerido de **6 a 8 minutos**, sem precisar jogar uma partida inteira.
Os exemplos abaixo pressupõem uma partida recém-iniciada.

## Preparação

Na raiz, antes de abrir servidor e clientes:

```sh
mvn clean package
```

Abra cinco terminais PowerShell independentes no VS Code. Garanta pelo menos
80 colunas e 24 linhas em cada terminal usado como cliente.

## 1. Arquitetura e Java RMI — 1 minuto

Explique: “Temos cinco JVMs: um servidor e quatro clientes. O servidor mantém
a única partida oficial; os clientes enviam intenções de ação e mostram snapshots.”

Mostre os pacotes `modelo`, `jogo`, `rmi`, `servidor` e `cliente`. Aponte:

- `JogoRemoto`: contrato com métodos que declaram `RemoteException`.
- `ServicoJogo`: exporta o objeto e delega as regras à sua `Partida` privada.
- `Servidor`: publica o nome `Quoridor` no registro de localhost, porta 1099.
- `Cliente`: faz lookup, recebe um stub e chama a interface remota.

O registro e o serviço estão na mesma JVM. A porta 1099 serve à descoberta;
o serviço exportado usa uma porta escolhida pelo RMI. O projeto não programa
`Socket` ou `ServerSocket` diretamente.

## 2. Cinco processos e quatro registros — 1 minuto

No terminal 1:

```sh
java -cp target/classes br.edu.quoridor.servidor.Servidor
```

Nos terminais 2, 3, 4 e 5, execute uma vez em cada um:

```sh
java -cp target/classes br.edu.quoridor.cliente.Cliente
```

Informe nomes e deixe o token vazio para novos registros. Mostre a contagem
enquanto os jogadores entram e os IDs atribuídos. O início é sorteado; **não
presuma que o jogador 1 começa**. Cada jogador tem cinco barreiras.

Explique que o nome é local e que o token identifica a sessão no servidor.
`sessao` mostra apenas o próprio token; ele permite reconectar sem ocupar outra vaga.

## 3. Movimento e sincronização — 1 minuto

No cliente que está com o turno, faça o primeiro movimento conforme a tabela:

| Jogador sorteado | Comando inicial |
| --- | --- |
| 1 | `mover 2 5` |
| 2 | `mover 5 8` |
| 3 | `mover 8 5` |
| 4 | `mover 5 2` |

Observe a posição e a mudança de turno nos outros terminais, sem digitar neles.
Explique: “Cada cliente consulta o servidor aproximadamente a cada segundo;
o painel só é redesenhado quando muda. Não usamos callbacks.”

Opcionalmente, no jogador que acabou de mover, tente repetir seu movimento:
o servidor deve recusar porque o turno passou. Não avance o roteiro se houver
uma mensagem inesperada; consulte `tabuleiro` e confirme o jogador da vez.

## 4. Barreira com prévia — 1 minuto

No cliente que agora está com o turno:

```text
barreira h 3,4 4,5
```

Mostre o `*` da proposta e explique: “A horizontal separa as linhas 3 e 4 nas
colunas 4 e 5. Os pares são consecutivos; internamente usamos a âncora `(2,3)`.”
Responda **N**: estoque e turno permanecem. Repita e responda **S**: a barreira
vira `=`, o estoque do autor cai para quatro e o turno passa.

Para mostrar também a vertical, no próximo jogador use:

```text
barreira v 6,7 6,7
```

Confirme com **S**. Ela separa as colunas 6 e 7 nas linhas 6 e 7, usando dois
segmentos. Essas duas propostas são permitidas na demonstração recém-iniciada.

## 5. BFS e concorrência — 1 minuto

Abra `Regras.temCaminho` e `Regras.podeColocarBarreira`:

“Simulamos a barreira numa lista temporária. Para cada jogador, a BFS percorre
casas vizinhas com uma fila e uma matriz de visitadas até encontrar seu objetivo.
Ignoramos outros peões porque são obstáculos temporários. Se algum jogador
ficar sem caminho, a tentativa é recusada sem consumir barreira ou turno.”

Abra `ServicoJogo` e destaque `synchronized`:

“O mesmo monitor protege registros, consultas, autenticação e aplicação de ações.
A verificação e a alteração são atômicas. `Partida` só publica um novo estado
após validar a jogada. As listas são defensivas e os DTOs imutáveis; uma cópia
recebida por RMI não permite alterar o servidor.”

## 6. Testes e encerramento — 1 a 2 minutos

Encerre os quatro clientes com `sair` e o servidor com **Ctrl+C**. Depois execute:

```sh
mvn clean verify
mvn -Paceitacao clean verify
```

Na verificação final desta entrega, o primeiro comando aprovou **232 testes** e
o segundo, **45**, sem falhas ou erros. Na apresentação, mostre os resultados
reais que aparecerem; esses números são casos, não número de classes.

Explique que o perfil reutiliza testes de integração, concorrência e processos.
Mostre `AceitacaoJogoTest`: há uma partida determinística completa com barreiras,
vitória do jogador 2, rejeição pós-vitória, cercamento por BFS e prévia desatualizada.
`ClientesProcessosTest` inicia quatro clientes em JVMs reais, espera eventos com
timeout e encerra os recursos ao terminar. Não são necessárias quatro pessoas.

## Perguntas técnicas possíveis

| Pergunta | Resposta baseada na implementação |
| --- | --- |
| Onde fica o estado oficial? | Na `Partida` privada de `ServicoJogo`, na JVM do servidor. Os clientes recebem snapshots. |
| Por que isso é distribuído em localhost? | São processos/JVMs independentes, comunicando-se por chamadas RMI e serialização, embora na mesma máquina. |
| O registry executa o jogo? | Não. Ele associa o nome `Quoridor` à referência remota; o serviço executa as chamadas do jogo. |
| Como um cliente identifica o jogador? | O servidor gera ID e token UUID. A ação recebe o token e o serviço busca o ID no mapa privado. O cliente não escolhe o ID. |
| O token é uma senha forte ou autenticação completa? | Ele é uma credencial de posse. Quem o conhece pode usar a sessão. Não há senha adicional nem TLS configurado. |
| E se dois clientes jogarem ao mesmo tempo? | O monitor de `ServicoJogo` serializa o acesso. Cada ação é validada contra o estado atual; depois de uma aceita, o turno mudou. |
| `Partida` é thread-safe sozinha? | A garantia de concorrência da aplicação está no serviço, que mantém a partida privada e acessa tudo sob o mesmo monitor. |
| Como o cliente recebe atualizações? | Polling RMI aproximadamente a cada segundo. Uma thread daemon lê linhas; um laço único faz RPC e escreve na tela. |
| Por que serializar os DTOs? | Para o RMI transportá-los por valor entre JVMs. O stub representa o serviço remoto; estado e resultados são cópias independentes. |
| Por que a BFS não considera os peões? | A validação exige um caminho geométrico permanente até o objetivo; a ocupação dos peões muda durante a partida. |
| Como são tratadas sobreposição e cruzamento? | Horizontais conflitam na mesma linha com âncoras sobrepostas; verticais, na mesma coluna. Orientações diferentes cruzam na mesma âncora. Depois verifica-se BFS. |
| A prévia garante que a barreira será aceita? | Não. Ela é local, temporária e não reserva posição nem turno. Ao confirmar, o servidor verifica novamente tudo. |
| E se a resposta de uma ação se perder? | O cliente informa a falha e não repete a ação automaticamente. Não há identificadores de requisição para garantir repetição idempotente. |
| O que acontece se um jogador sair? | Sua vaga permanece reservada. Pode reconectar com o token enquanto o servidor continuar vivo. Não há substituição automática ou prazo de turno. |
| E se o servidor cair? | A partida e as sessões são perdidas. Não há persistência nem réplica. Reiniciar cria outra partida. |
| O jogo termina como? | Ao alcançar o lado oposto, `Partida` registra o vencedor e mantém o término; novas ações são recusadas. |
| Os testes provam todos os comportamentos? | Cobrem os cenários documentados. A apresentação visual ANSI, edição de linha e Ctrl+C merecem ensaio manual; não há prova exaustiva nem teste de carga prolongado. |

Detalhes de execução e limitações: [README](README.md).
Cobertura e roteiros: [Testes de aceitação](docs/TESTES_ACEITACAO.md).
