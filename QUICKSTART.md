# Quick Start Guide - Poker Game

## Szybkie uruchomienie

### 1. Kompilacja projektu

```bash
cd poker
mvn clean package
```

### 2. Uruchomienie serwera

Otwórz terminal i uruchom:

```bash
java -jar poker-server/target/poker-server.jar
```

Serwer uruchomi się na porcie 7777.

### 3. Uruchomienie klientów

Otwórz kolejne terminale dla każdego gracza i uruchom:

```bash
java -jar poker-client/target/poker-client.jar
```

### 4. Rozgrywka - przykładowy scenariusz

#### Terminal 1 (Gracz Alice - Host):
```
> create 10 20
> join GAME123 Alice
> start
```

#### Terminal 2 (Gracz Bob):
```
> join GAME123 Bob
```

Po starcie gry gracze automatycznie otrzymują karty.

#### Faza zakładów (BET1):
```
Alice> check
Bob> bet 20
Alice> call
```

#### Faza wymiany kart (DRAW):
```
Alice> draw 0,2       # Wymienia karty na pozycjach 0 i 2
Bob> draw none        # Nie wymienia kart
```

#### Druga faza zakładów (BET2):
```
Alice> bet 20
Bob> call
```

#### Showdown:
System automatycznie pokazuje karty i określa zwycięzcę.

## Komendy klienta

### Podstawowe komendy:

- `create <ante> <bet>` - Utwórz nową grę
  - Przykład: `create 10 20`
  
- `join <gameId> <name>` - Dołącz do gry
  - Przykład: `join GAME123 Alice`
  
- `start` - Rozpocznij grę (tylko host)

### Komendy podczas gry:

- `check` - Sprawdź (gdy nie ma obstawienia)
- `call` - Zrównaj obstawienie
- `bet <amount>` - Podbij obstawienie
  - Przykład: `bet 20`
- `fold` - Spasuj
- `draw <indices>` - Wymień karty
  - Przykład: `draw 0,2,4` (wymienia karty 0, 2 i 4)
  - Przykład: `draw none` (nie wymienia kart)

### Inne komendy:

- `status` - Sprawdź status gry
- `leave` - Opuść grę
- `quit` - Zakończ klienta
- `help` - Pokaż pomoc

## Zasady gry

### 1. Ante
Wszyscy gracze wpłacają ante (wejście do gry).

### 2. Rozdanie
Każdy gracz otrzymuje 5 kart.

### 3. Pierwsza runda zakładów
Gracze mogą:
- **Check** - sprawdzić (jeśli nikt nie obstawił)
- **Bet** - obstawić
- **Call** - zrównać obstawienie
- **Fold** - spasować

### 4. Wymiana kart
Każdy gracz może wymienić do 3 kart.

### 5. Druga runda zakładów
Kolejna runda zakładów.

### 6. Showdown
Gracze pokazują karty, najlepsza ręka wygrywa pulę.

## Ranking rąk (od najlepszej):

1. **Royal Flush** - A, K, Q, J, 10 tego samego koloru
2. **Straight Flush** - 5 kolejnych kart tego samego koloru
3. **Four of a Kind** - Cztery karty tej samej wartości
4. **Full House** - Trzy + dwa karty tej samej wartości
5. **Flush** - 5 kart tego samego koloru
6. **Straight** - 5 kolejnych kart
7. **Three of a Kind** - Trzy karty tej samej wartości
8. **Two Pair** - Dwie pary
9. **Pair** - Para
10. **High Card** - Najwyższa karta

## Rozwiązywanie problemów

### Serwer nie startuje:
- Sprawdź czy port 7777 jest wolny
- Uruchom na innym porcie: `java -jar poker-server.jar 8888`

### Klient nie może się połączyć:
- Sprawdź czy serwer działa
- Sprawdź adres i port: `java -jar poker-client.jar localhost 7777`

### Błędy kompilacji:
- Upewnij się że masz JDK 21+: `java -version`
- Sprawdź wersję Maven: `mvn -version`

## Testy

Uruchomienie testów:
```bash
mvn test
```

Raport pokrycia:
```bash
mvn jacoco:report
```

Raport dostępny w: `target/site/jacoco/index.html`

## Przykładowa sesja gry

```
[Alice tworzy grę]
Alice> create 10 20
[SERVER] - - OK MESSAGE=Game created: abc123def456

[Alice dołącza]
Alice> join abc123def456 Alice
[SERVER] - - WELCOME GAME=abc123def456 PLAYER=p1a2b3c4
[SERVER] abc123def456 - LOBBY PLAYERS=Alice

[Bob dołącza]
Bob> join abc123def456 Bob
[SERVER] - - WELCOME GAME=abc123def456 PLAYER=p5d6e7f8
[SERVER] abc123def456 - LOBBY PLAYERS=Alice,Bob

[Alice rozpoczyna]
Alice> start
[SERVER] abc123def456 - STARTED DEALER=p1a2b3c4 ANTE=10 BET=20
[SERVER] abc123def456 - ANTE_OK PLAYER=p1a2b3c4 STACK=990
[SERVER] abc123def456 - ANTE_OK PLAYER=p5d6e7f8 STACK=990
[SERVER] abc123def456 - DEAL PLAYER=p1a2b3c4 CARDS=AS,KH,QD,JC,TH
[SERVER] abc123def456 - DEAL PLAYER=p5d6e7f8 CARDS=*,*,*,*,*
[SERVER] abc123def456 - TURN PLAYER=p1a2b3c4 PHASE=BET1 CALL=0 MINRAISE=20

[... gra trwa ...]
```

## Dodatkowe informacje

- Logi serwera zapisywane są w pliku `poker-server.log`
- Protokół komunikacji jest czytelny dla człowieka
- Każda akcja jest walidowana po stronie serwera
- Obsługa do 4 graczy jednocześnie

## Kontakt

W razie pytań lub problemów, sprawdź plik README.md lub skontaktuj się z autorem projektu.
