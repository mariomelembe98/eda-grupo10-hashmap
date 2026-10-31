# Sistema de Gestão do Parque de Estacionamento — EDA Grupo 10

Projecto académico da disciplina de Estrutura de Dados e Algoritmos. O tema central é a implementação e utilização de um TAD `HashMap` genérico próprio num sistema de gestão do parque de estacionamento da Faculdade de Ciências e Tecnologia.

## Objectivos

- Implementar uma tabela hash genérica com tratamento de colisões e redimensionamento.
- Aplicar o TAD à gestão de utilizadores, veículos, lugares e estacionamentos.
- Demonstrar regras de negócio, testes automatizados e persistência local.

## Arquitectura

- `mz.edu.eda.tad`: interface `Mapa<K,V>` e implementação `MeuHashMap<K,V>`.
- `mz.edu.eda.modelo`: estudantes, docentes/funcionários, veículos, lugares, estacionamentos e tipos enumerados.
- `mz.edu.eda.servico`: regras de negócio e índices de ocupação em `ParqueService`.
- `mz.edu.eda.persistencia`: serialização de estado com `ObjectOutputStream` e `ObjectInputStream`.
- `mz.edu.eda.ui`: menu de consola com entrada e saída injectáveis.
- `mz.edu.eda`: arranque da aplicação (`Main`) e demonstração isolada do TAD (`DemonstracaoHashMap`).

O serviço utiliza `MeuHashMap` como estrutura principal. O TAD resolve colisões por encadeamento separado, inicia com capacidade 16 e redimensiona ao exceder factor de carga 0,75. Operações básicas têm custo esperado O(1) com distribuição adequada dos hashes e O(n) no pior caso de colisões.

## Requisitos

- Java 21
- Maven 3.9.x

Não são necessárias dependências de execução externas. JUnit 5 é usado nos testes.

## Compilar e executar

Executar a suite de testes:

```bash
mvn clean test
```

Compilar a aplicação e iniciar o menu:

```bash
mvn clean package
java -cp target/classes mz.edu.eda.Main
```

Executar a demonstração isolada do TAD:

```bash
java -cp target/classes mz.edu.eda.DemonstracaoHashMap
```

## Operações do menu

O menu permite:

1. Cadastrar estudantes e docentes/funcionários.
2. Cadastrar veículos associados a utilizadores existentes.
3. Cadastrar lugares por tipo.
4. Actualizar utilizadores, veículos e lugares.
5. Listar veículos por categoria de proprietário e matrícula.
6. Registar entradas e saídas.
7. Listar veículos estacionados e consultar contagens de lugares.
8. Consultar histórico completo ou por utilizador.
9. Consultar utilizadores, veículos e lugares por identificador.
10. Reconciliar o serviço com o ficheiro persistido.

As opções são numeradas no menu; `0` regressa ou encerra, conforme o ecrã.

## Persistência e falhas

As alterações são guardadas automaticamente em `dados/parque.dat`. O repositório escreve primeiro num temporário e tenta substituir o ficheiro principal por movimento atómico. Utilizadores, veículos, lugares, histórico e sequência de estacionamentos são persistidos; os índices derivados são reconstruídos ao carregar.

Se uma falha durante a substituição deixar o resultado indeterminado, o serviço bloqueia novas mutações. Use a opção **14 — Reconciliar persistência** para reler e validar o ficheiro antes de continuar. Um ficheiro ausente inicia um estado vazio; ficheiros corrompidos ou inconsistentes são rejeitados, sem reinicialização automática.

## Testes

Os testes JUnit cobrem o TAD, o modelo, as regras do serviço, a serialização e recuperação após reinício, falhas de persistência e navegação do menu. Execute-os com `mvn clean test`.

## Limitações conhecidas

- A persistência é local e baseada na serialização Java; não é uma base de dados nem um formato de intercâmbio.
- A aplicação não oferece interface gráfica ou acesso concorrente seguro.
- As consultas e os identificadores de texto usam comparação sensível a maiúsculas/minúsculas.
- O custo do pior caso do TAD é linear quando muitas chaves colidem.
