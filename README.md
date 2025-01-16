# Dependency Manager for Maven Applications

Este projeto fornece um gerenciador de dependências para aplicações Maven, eliminando a necessidade de frameworks externos para injeção de dependências. Ele é projetado para ser simples, modular e eficiente, permitindo que você configure suas dependências facilmente.

## Funcionalidades

- **Injeção Automática**: Descoberta e inicialização automática de classes anotadas.
- **Suporte a Estratégias**: Escolha entre `Singleton` ou instâncias criadas sob demanda.
- **Anotações Personalizadas**:
  - `@Inject`: Para injeção de dependências em campos e construtores.
  - `@Injectable`: Marca classes como disponíveis para injeção.
  - `@Bootable`: Define a classe principal para inicialização.

## Estrutura do Projeto

- **`ApplicationManagerRunner`**: Gerencia a execução principal da aplicação, encontrando e inicializando a classe anotada com `@Bootable`.
- **`DependencyManagerApplication`**: Implementa o gerenciamento de dependências, identificando classes anotadas com `@Injectable` e injetando dependências automaticamente.
- **`DependencyManagerCreator`**: Define a lógica de criação de dependências.

## Como Usar

### 1. Configuração da Aplicação

Anote suas classes com @Bootable para torná-las disponíveis para o gerenciador de inicialização:

```java
@Bootable(methodName = "initialize") // ou apenas @Bootable por padrão vem com initialize
public class Main {
    public void initialize(){ 
        testeService.teste();
    }
}
```


### 2. Configuração Inicial de injecão

Anote suas classes com @Injectable para torná-las disponíveis para o gerenciador de dependências:

```java
@Injectable
public class MyService {
    public void execute() {
        System.out.println("Serviço executado!");
    }
}
```
