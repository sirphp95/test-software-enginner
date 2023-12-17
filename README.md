# Desafio Engenharia Software

## Projeto de integração de microsserviços

- [Introdução](#introdução)
- [Instalação](#instalaçao)
- [Arquitetura](#arquitetura)
- [Modelagem de dados](#modelagem-de-dados)
- [Casos de uso](#casos-de-uso)
- [Observabilidade](#observability)
- [Implatação na AWS](#implantaçao)

### Introdução

Esse projeto teve sua origem a partir de um processo seletivo sendo reaproveitado para a construção de um artigo-tutorial
para auxiliar a comunidade de desenvolvedores e engenheiros de software em um dos temas mais populares a atualidade no 
assunto de programação distribuída.

No código se encontra bons exemplos práticos de implantação de comunicações síncronas e assíncronas que são feitas a
partir de pods em um Cluster Kubernetes.

Além disso se encontra implementações de bancos de dados orientado a documento (DocumentDB/MongoDB) e baseados em Cachê
(Redis).

No repositório se encontram arquivos de deployments e services para a implantação das imagens geradas a partir dos Dockerfiles
dentro das pastas do Microsserviço.

Alguns links úteis:


SWAGGER funcional das APIs: http://187.104.145.57:8081

Grafana com dashboard de monitoramento das aplicações: http://187.104.145.57:3000



### Instalaçao

Apesar do projeto ter como objetivo ser implantado na Núvem, para efeitos didáticos foi-se criado um arquivo com comandos
shellscript para auxiliar na instalaçao da máquina local do usuário/desenvolvedor. Mas para isso é necessário alguns requisitos:

 * Docker(v4.30.1) e Kubernetes(1.24.0) instalados (versão mínima)
 * Possuir suporte para execução de shellscript (em sistemas baseados em Unix são nativos, para OS Windows é interessante instalar gitbash)

Com todos os requisitos atendidos, configure as variáveis de ambiente {MONGODB_HOST} para os arquivos *deployment.yml* dos microsserviços,
é importante setar um IP/PORTA para um MongoDB/DocumentDB com as mesmas credenciais. Caso necessário existe um script abaixo que baixa a imagem
do MongoDB e implementa em um container. E então basta indicar o IP externo da sua rede física local.

Em seguida abra o terminal na pasta raíz do projeto  e execute comando 

   ```sh
   ./deploy.sh
   ```

Após o término da execução do comando, consulte os pods gerados (total 7 pods) através do comando

   ```sh
   kubectl get pod
   ```
Depois de todos os pods estarem saudãveis, abra um port-forward apontando para o pod da aplicação *ms-bff* expondo sua porta desejada para a porta 8080

   ```sh
   kubectl port-forward <NOME_DO_POD> <SUA_PORTA>:8080
   ```

E finalmente, basta acessar o swagger da aplicação em http://127.0.0.1:8081/swagger-ui.html 

### Arquitetura

Nesse projeto foi adotada a arquitetura baseada em microsserviços, com comunicação feita tanto com gRPC e mensageria com
RabbitMQ, atraves de duas filas entre os domínios de pagamento e pedido.

Existem no total 4 dominios (cliente, pedido, pagamento e produto) e para cada domínio, um banco de dados inserido no 
cluster do Amazon DocumentDB

Segue abaixo o diagrama de arquitetura implantada no Cluster Kubernetes:
![Architecture](https://github.com/sirphp95/test-software-enginner/assets/70244618/e403aea5-9861-4fe1-b338-718b2f1ac1a9)


### Modelagem de dados
 
 * Domínio Cliente


![Customer](https://github.com/sirphp95/test-software-enginner/assets/70244618/3f4e22da-c3d3-4eaa-a6be-564fd6dc4ca6)


 * Domínio Pedido

![Order](https://github.com/sirphp95/test-software-enginner/assets/70244618/07e3f588-17e4-4f49-89a9-1a085ac4349e)

 * Domínio Pagamento

![payment](https://github.com/sirphp95/test-software-enginner/assets/70244618/560b6f49-0555-4401-b62d-39147f134393)


* Domínio Produto

![Product](https://github.com/sirphp95/test-software-enginner/assets/70244618/0d1905dc-9f7c-49c1-9a72-7f0e2409c24a)



### Casos de uso 

 * Desenho técnico

![use-case](https://github.com/sirphp95/test-software-enginner/assets/70244618/1f0d238c-ef0d-4471-9c62-6958ca1c31ea)



 * Evidências de teste

    * Cenário 1: Cria Pagamento, Postar compra e Consultar top 5

Com um usuário e 2 produtos previamente cadastrados nos controllers Customer e Product, é montado o seguinte payload para
criar um novo pedido, nele são inseridos os IDs do cliente e IDs dos produtos, em caso de compra de mais de uma unidade 
do mesmo item, o ID deve ser inserido N vezes.

![Request CreateOrder](https://github.com/sirphp95/test-software-enginner/assets/70244618/61f72efd-3c80-4e9e-b806-0e89c0a6737e)


Após a criação do pedido, é retornado no corpo da resposta o objeto resultante e persistido na base de dados, note que jã
são inseridos as informações que provêm dos IDs inseridos e como quantidade de itens, somatório total do pedido(Domínio produto)
e endereço de entrega (Domínio Cliente).

![Result CreateOrder](https://github.com/sirphp95/test-software-enginner/assets/70244618/96c93bf9-704a-4dba-8e7d-941431c5d903)


Também é gerado um registro na base do domínio de Pagamentos com o ID do pedido (orderId), que foi estimulado por recebimento de uma fila no RabbitMQ.


![Request CreateOrder](https://github.com/sirphp95/test-software-enginner/assets/70244618/8f7fe1c6-83ac-4cb9-8f36-2e6041d9ac53)


Já no domínio de produto, a quantidade dos produtos solicitados continua inalteradas até a postagem do pagamento.


![Result FindAllProducts](https://github.com/sirphp95/test-software-enginner/assets/70244618/e6dea4d8-1723-4869-88e0-03566a855f2a)



Para postar um pagamento, é necessário informar o ID do pagamento, o método (PIX / CREDIT_CARD / BANKSLIP) e o Status que será
trasacionado (PENDING / REFUSED / CONFIRMED / CANCELLED)


![Request PostPayment2](https://github.com/sirphp95/test-software-enginner/assets/70244618/f049ae0e-3e55-49cc-93d0-f8e13b0246f4)


E em seguida é retornado o pagamento com status realizado


![Result PostPayment](https://github.com/sirphp95/test-software-enginner/assets/70244618/1f2120b4-c1de-49db-89d9-7b817316e206)


Também são alterados as quantidades disponíveis em estoque dos produtos comprados de acordo com a quantidade do pedido


![Result FindAllProducts post Payment](https://github.com/sirphp95/test-software-enginner/assets/70244618/9947ea8e-bba6-472d-9960-fe4e1e340625)


Além de atualizar o registro do pedido, que foi estimulado por outra mensagem no RabbitMQ para atualizá-lo do status de pagamento.


![Result findAllOrder post Payment Confirmed](https://github.com/sirphp95/test-software-enginner/assets/70244618/d8799a9c-2665-4659-8620-4c2d37ae788b)


E por fim, é gerado um cache para consultar os produtos mais vendidos por ordem de maior quantidade vendida.


![Result FindTopSoldProducts post payment](https://github.com/sirphp95/test-software-enginner/assets/70244618/2414386c-c8a3-4f44-9b59-b790083608e3)


