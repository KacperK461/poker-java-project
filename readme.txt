Projekt implementuje pełną grę w pokera wariant "5-Card Draw" (Pokera
z wymianą) dla 2-4 graczy.

LINK DO ZASAD GRY:
https://pl.wikipedia.org/wiki/Poker_pi%C4%99ciokartowy_dobierany
https://terazpoker.pl/poker-pieciokartowy-dobierany-5-card-draw/


URUCHOMIENIE PROJEKTU

mvn clean package

Po kompilacji zostaną utworzone pliki JAR:
  - poker-server/target/poker-server.jar  (serwer)
  - poker-client/target/poker-client.jar  (klient)

URUCHOMIENIE SERVERA
java -jar poker-server/target/poker-server.jar
  
URUCHOMIENIE CLIENTA
java -jar poker-client/target/poker-client.jar


PROTOKÓŁ KOMUNIKACYJNY

OGÓLNY FORMAT WIADOMOŚCI:

Wszystkie wiadomości używają formatu tekstowego czytelnego dla człowieka:
  GAME_ID PLAYER_ID ACTION [PARAM1=VALUE1 PARAM2=VALUE2 ...]

Gdzie:
  - GAME_ID: identyfikator gry (lub "-" jeśli nie dotyczy)
  - PLAYER_ID: identyfikator gracza (lub "-" jeśli nie dotyczy)
  - ACTION: nazwa akcji/komendy
  - PARAM: parametry w formacie klucz=wartość

Przykłady:
  - HELLO VERSION=1.0
  GAME123 PLAYER_Alice BET AMOUNT=20
  GAME123 - DEAL PLAYER=PLAYER_Bob CARDS=AS,KH,QD,JC,10S

OGRANICZENIA:
  - Maksymalna długość wiadomości: 512 bajtów
  - Kodowanie: UTF-8
  - Separator parametrów: spacja
  - Format parametru: KLUCZ=WARTOŚĆ


--------------------------------------------------------------------------------
KOMUNIKATY KLIENT → SERWER
--------------------------------------------------------------------------------

1. HELLO - Inicjalizacja połączenia
   Format: - - HELLO VERSION=<wersja>
   Parametry:
     VERSION: wersja protokołu klienta (np. "1.0")
   Kiedy wysyłany: Jako pierwsza wiadomość po nawiązaniu połączenia TCP
   Oczekiwana odpowiedź serwera: OK lub ERR
   Przykład: - - HELLO VERSION=1.0


2. CREATE - Utworzenie nowej gry
   Format: - - CREATE ANTE=<n> BET=<n> LIMIT=FIXED
   Parametry:
     ANTE: wysokość ante (obowiązkowy wpis do gry)
     BET: wysokość zakładu (w grze fixed limit)
     LIMIT: typ limitów (obecnie tylko FIXED)
   Kiedy wysyłany: Przez gracza, który chce stworzyć nową grę
   Oczekiwana odpowiedź serwera: OK z MESSAGE=GAME_ID lub ERR
   Przykład: - - CREATE ANTE=10 BET=20 LIMIT=FIXED


3. JOIN - Dołączenie do gry
   Format: - - JOIN GAME=<id> NAME=<nazwa>
   Parametry:
     GAME: identyfikator gry do której gracz dołącza
     NAME: nick gracza (będzie użyty jako część ID gracza)
   Kiedy wysyłany: Przez gracza, który chce dołączyć do istniejącej gry
   Oczekiwana odpowiedź serwera: WELCOME, następnie LOBBY lub ERR
   Przykład: - - JOIN GAME=GAME123 NAME=Alice


4. LEAVE - Opuszczenie gry
   Format: <gameId> <playerId> LEAVE
   Parametry: brak
   Kiedy wysyłany: Gdy gracz chce opuścić grę (ale pozostać połączony)
   Oczekiwana odpowiedź serwera: OK lub ERR
   Uwagi: Gracz może dołączyć do innej gry po opuszczeniu obecnej
   Przykład: GAME123 PLAYER_Alice LEAVE


5. START - Rozpoczęcie gry
   Format: <gameId> <playerId> START
   Parametry: brak
   Kiedy wysyłany: Przez hosta gry, gdy jest co najmniej 2 graczy
   Oczekiwana odpowiedź serwera: STARTED (broadcast do wszystkich) lub ERR
   Uwagi: Tylko host (twórca gry) może rozpocząć grę
   Przykład: GAME123 PLAYER_Alice START


6. CHECK - Sprawdzenie (bez obstawiania)
   Format: <gameId> <playerId> CHECK
   Parametry: brak
   Kiedy wysyłany: W rundzie zakładów, gdy kolej gracza i nikt nie obstawił
   Oczekiwana odpowiedź serwera: ACTION (broadcast), następnie TURN lub ERR
   Uwagi: Możliwe tylko gdy currentBet == 0
   Przykład: GAME123 PLAYER_Alice CHECK


7. CALL - Zrównanie obstawienia
   Format: <gameId> <playerId> CALL
   Parametry: brak
   Kiedy wysyłany: W rundzie zakładów, gdy gracz chce zrównać obstawienie
   Oczekiwana odpowiedź serwera: ACTION (broadcast), następnie TURN lub SHOWDOWN
   Uwagi: Gracz wpłaca różnicę między swoim obecnym a najwyższym zakładem
   Przykład: GAME123 PLAYER_Bob CALL


8. BET - Postawienie/podniesienie zakładu
   Format: <gameId> <playerId> BET AMOUNT=<kwota>
   Parametry:
     AMOUNT: kwota zakładu (musi być >= minimalny zakład z konfiguracji)
   Kiedy wysyłany: W rundzie zakładów, gdy gracz chce obstawić lub podbić
   Oczekiwana odpowiedź serwera: ACTION (broadcast), następnie TURN lub ERR
   Uwagi: W Fixed Limit kwota musi być równa skonfigurowanemu BET
   Przykład: GAME123 PLAYER_Charlie BET AMOUNT=20


9. FOLD - Pasowanie
   Format: <gameId> <playerId> FOLD
   Parametry: brak
   Kiedy wysyłany: W rundzie zakładów, gdy gracz chce spasować
   Oczekiwana odpowiedź serwera: ACTION (broadcast), następnie TURN
   Uwagi: Gracz, który spasował, traci szansę na wygraną w tej rundzie
   Przykład: GAME123 PLAYER_Dave FOLD


10. DRAW - Wymiana kart
    Format: <gameId> <playerId> DRAW CARDS=<indeksy>
    Parametry:
      CARDS: indeksy kart do wymiany oddzielone przecinkami (0-4)
             lub "none" jeśli gracz nie chce wymieniać kart
    Kiedy wysyłany: W fazie DRAW, gdy kolej gracza
    Oczekiwana odpowiedź serwera: DRAWOK, następnie TURN lub ERR
    Uwagi: Maksymalnie 3 karty można wymienić
    Przykład 1: GAME123 PLAYER_Alice DRAW CARDS=0,2,4
    Przykład 2: GAME123 PLAYER_Bob DRAW CARDS=none


11. STATUS - Zapytanie o status gry
    Format: <gameId> <playerId> STATUS
    Parametry: brak
    Kiedy wysyłany: Gdy gracz chce sprawdzić aktualny stan gry
    Oczekiwana odpowiedź serwera: ROUND z informacjami o puli i zakładach
    Uwagi: Można wysłać w dowolnym momencie
    Przykład: GAME123 PLAYER_Alice STATUS


12. QUIT - Rozłączenie
    Format: <gameId> <playerId> QUIT
    Parametry: brak
    Kiedy wysyłany: Gdy gracz chce zakończyć klienta i rozłączyć się
    Oczekiwana odpowiedź serwera: OK, następnie serwer zamyka połączenie
    Uwagi: Gracz automatycznie opuszcza grę jeśli był w jakiejś
    Przykład: GAME123 PLAYER_Alice QUIT


--------------------------------------------------------------------------------
KOMUNIKATY SERWER → KLIENT
--------------------------------------------------------------------------------

1. OK - Potwierdzenie sukcesu
   Format: - - OK [MESSAGE=<tekst>]
   Parametry:
     MESSAGE (opcjonalny): dodatkowa informacja, np. ID utworzonej gry
   Kiedy wysyłany: W odpowiedzi na poprawnie wykonaną komendę
   Wymagane działanie klienta: Wyświetlić potwierdzenie
   Przykład 1: - - OK
   Przykład 2: - - OK MESSAGE=GAME123


2. ERR - Komunikat błędu
   Format: - - ERR CODE=<kod> REASON=<powód>
   Parametry:
     CODE: kod błędu (np. INVALID_MOVE, NOT_YOUR_TURN, INSUFFICIENT_CHIPS)
     REASON: opisowy powód błędu
   Kiedy wysyłany: Gdy komenda klienta nie może być wykonana
   Wymagane działanie klienta: Wyświetlić komunikat błędu użytkownikowi
   Przykład: - - ERR CODE=NOT_YOUR_TURN REASON=Wait for your turn


3. WELCOME - Powitanie po dołączeniu
   Format: - - WELCOME GAME=<gameId> PLAYER=<playerId>
   Parametry:
     GAME: identyfikator gry do której dołączono
     PLAYER: unikalny identyfikator nadany graczowi (np. PLAYER_Alice)
   Kiedy wysyłany: Po pomyślnym dołączeniu do gry (po JOIN)
   Wymagane działanie klienta: Zapisać gameId i playerId do dalszej komunikacji
   Przykład: - - WELCOME GAME=GAME123 PLAYER=PLAYER_Alice


4. LOBBY - Status poczekalnia
   Format: <gameId> - LOBBY PLAYERS=<lista>
   Parametry:
     PLAYERS: lista graczy oddzielona przecinkami
   Kiedy wysyłany: Po dołączeniu gracza do gry (broadcast do wszystkich w grze)
   Wymagane działanie klienta: Wyświetlić listę graczy oczekujących
   Przykład: GAME123 - LOBBY PLAYERS=Alice,Bob,Charlie


5. STARTED - Gra rozpoczęta
   Format: <gameId> - STARTED DEALER=<playerId> ANTE=<n> BET=<n>
   Parametry:
     DEALER: identyfikator gracza-dealera
     ANTE: wysokość ante
     BET: wysokość zakładu
   Kiedy wysyłany: Po wykonaniu START przez hosta (broadcast do wszystkich)
   Wymagane działanie klienta: Przygotować interfejs do rozgrywki
   Przykład: GAME123 - STARTED DEALER=PLAYER_Alice ANTE=10 BET=20


6. ANTE - Żądanie wpłaty ante
   Format: <gameId> - ANTE PLAYER=<playerId> AMOUNT=<n>
   Parametry:
     PLAYER: identyfikator gracza, który ma wpłacić ante
     AMOUNT: kwota ante do wpłacenia
   Kiedy wysyłany: W fazie ANTE dla każdego gracza (broadcast)
   Wymagane działanie klienta: Wyświetlić informację o wpłacie ante
   Przykład: GAME123 - ANTE PLAYER=PLAYER_Alice AMOUNT=10


7. ANTE_OK - Potwierdzenie wpłaty ante
   Format: <gameId> - ANTE_OK PLAYER=<playerId> STACK=<n>
   Parametry:
     PLAYER: identyfikator gracza, który wpłacił ante
     STACK: pozostała liczba chipów gracza
   Kiedy wysyłany: Po pomyślnej wpłacie ante przez gracza (broadcast)
   Wymagane działanie klienta: Zaktualizować stan chipów gracza
   Przykład: GAME123 - ANTE_OK PLAYER=PLAYER_Alice STACK=490


8. DEAL - Rozdanie kart
   Format: <gameId> - DEAL PLAYER=<playerId> CARDS=<karty>
   Parametry:
     PLAYER: identyfikator gracza otrzymującego karty
     CARDS: lista kart oddzielona przecinkami (np. AS,KH,QD,JC,10S)
           dla innych graczy: XX,XX,XX,XX,XX (karty zakryte)
   Kiedy wysyłany: W fazie DEAL dla każdego gracza (broadcast)
   Wymagane działanie klienta:
     - Jeśli PLAYER to ja: wyświetlić moje karty
     - Jeśli PLAYER to inny gracz: wyświetlić zakryte karty
   Przykład 1: GAME123 - DEAL PLAYER=PLAYER_Alice CARDS=AS,KH,QD,JC,10S
   Przykład 2: GAME123 - DEAL PLAYER=PLAYER_Bob CARDS=XX,XX,XX,XX,XX


9. TURN - Kolej gracza
   Format: <gameId> - TURN PLAYER=<playerId> PHASE=<faza> CALL=<n> MINRAISE=<n>
   Parametry:
     PLAYER: identyfikator gracza, który ma wykonać ruch
     PHASE: aktualna faza gry (BET1, DRAW, BET2)
     CALL: kwota potrzebna do zrównania zakładu
     MINRAISE: minimalna kwota do podbicia
   Kiedy wysyłany: Gdy przychodzi kolej na akcję gracza (broadcast)
   Wymagane działanie klienta:
     - Jeśli PLAYER to ja: wyświetlić dostępne akcje i czekać na input
     - Jeśli PLAYER to inny: wyświetlić że gracz myśli
   Przykład: GAME123 - TURN PLAYER=PLAYER_Bob PHASE=BET1 CALL=20 MINRAISE=20


10. ACTION - Informacja o wykonanej akcji
    Format: <gameId> - ACTION PLAYER=<playerId> TYPE=<typ> [ARGS=<argumenty>]
    Parametry:
      PLAYER: identyfikator gracza, który wykonał akcję
      TYPE: typ akcji (CHECK, CALL, BET, FOLD, DRAW)
      ARGS (opcjonalny): argumenty akcji (np. kwota dla BET)
    Kiedy wysyłany: Po wykonaniu akcji przez gracza (broadcast do wszystkich)
    Wymagane działanie klienta: Wyświetlić informację o akcji gracza
    Przykład 1: GAME123 - ACTION PLAYER=PLAYER_Alice TYPE=CHECK
    Przykład 2: GAME123 - ACTION PLAYER=PLAYER_Bob TYPE=BET ARGS=20
    Przykład 3: GAME123 - ACTION PLAYER=PLAYER_Charlie TYPE=FOLD


11. DRAWOK - Potwierdzenie wymiany kart
    Format: <gameId> - DRAWOK PLAYER=<playerId> COUNT=<n> NEW=<karty>
    Parametry:
      PLAYER: identyfikator gracza, który wymienił karty
      COUNT: liczba wymienionych kart (0-3)
      NEW: nowe karty (widoczne tylko dla danego gracza, dla innych XX,XX,...)
    Kiedy wysyłany: Po wykonaniu DRAW przez gracza (broadcast)
    Wymagane działanie klienta:
      - Jeśli PLAYER to ja: zaktualizować moje karty
      - Jeśli PLAYER to inny: wyświetlić liczbę wymienionych kart
    Przykład 1: GAME123 - DRAWOK PLAYER=PLAYER_Alice COUNT=2 NEW=9S,7H
    Przykład 2: GAME123 - DRAWOK PLAYER=PLAYER_Bob COUNT=0 NEW=


12. ROUND - Informacja o rundzie
    Format: <gameId> - ROUND POT=<n> HIGHESTBET=<n>
    Parametry:
      POT: aktualna pula
      HIGHESTBET: najwyższy zakład w tej rundzie
    Kiedy wysyłany: W odpowiedzi na STATUS lub automatycznie po zmianach
    Wymagane działanie klienta: Zaktualizować wyświetlane informacje o grze
    Przykład: GAME123 - ROUND POT=100 HIGHESTBET=20


13. SHOWDOWN - Pokazanie kart w finale
    Format: <gameId> - SHOWDOWN PLAYER=<playerId> HAND=<karty> RANK=<układ>
    Parametry:
      PLAYER: identyfikator gracza pokazującego karty
      HAND: karty gracza
      RANK: nazwa układu pokerowego (np. PAIR_K, FLUSH_A)
    Kiedy wysyłany: W fazie SHOWDOWN dla każdego gracza który nie spasował
    Wymagane działanie klienta: Wyświetlić karty i układ gracza
    Przykład: GAME123 - SHOWDOWN PLAYER=PLAYER_Alice HAND=AS,AH,KD,QC,JS
              RANK=PAIR_A


14. WINNER - Ogłoszenie zwycięzcy
    Format: <gameId> - WINNER PLAYER=<playerId> POT=<n> RANK=<układ>
    Parametry:
      PLAYER: identyfikator zwycięzcy (lub lista przy remisie)
      POT: kwota wygranej
      RANK: wygrywający układ
    Kiedy wysyłany: Po SHOWDOWN gdy określono zwycięzcę
    Wymagane działanie klienta: Wyświetlić komunikat o zwycięzcy
    Przykład: GAME123 - WINNER PLAYER=PLAYER_Bob POT=120 RANK=FLUSH_A


15. PAYOUT - Wypłata wygranej
    Format: <gameId> - PAYOUT PLAYER=<playerId> AMOUNT=<n> STACK=<n>
    Parametry:
      PLAYER: identyfikator gracza otrzymującego wypłatę
      AMOUNT: kwota wypłaty
      STACK: nowa liczba chipów gracza po wypłacie
    Kiedy wysyłany: W fazie PAYOUT dla każdego zwycięzcy (broadcast)
    Wymagane działanie klienta: Zaktualizować stan chipów gracza
    Przykład: GAME123 - PAYOUT PLAYER=PLAYER_Bob AMOUNT=120 STACK=620


16. END - Zakończenie gry
    Format: <gameId> - END REASON=<powód>
    Parametry:
      REASON: powód zakończenia (COMPLETE, INSUFFICIENT_PLAYERS, ERROR)
    Kiedy wysyłany: Na końcu rundy lub przy przedwczesnym zakończeniu
    Wymagane działanie klienta: Wyświetlić podsumowanie, wrócić do menu
    Przykład: GAME123 - END REASON=COMPLETE


--------------------------------------------------------------------------------
PRZYKŁADOWY PRZEBIEG KOMUNIKACJI
--------------------------------------------------------------------------------

SCENARIUSZ: Dwóch graczy (Alice - host, Bob) grają w pokera

1. Nawiązanie połączenia i utworzenie gry przez Alice:
   C→S: - - HELLO VERSION=1.0
   S→C: - - OK
   
   C→S: - - CREATE ANTE=10 BET=20 LIMIT=FIXED
   S→C: - - OK MESSAGE=GAME123
   
   C→S: - - JOIN GAME=GAME123 NAME=Alice
   S→C: - - WELCOME GAME=GAME123 PLAYER=PLAYER_Alice
   S→C: GAME123 - LOBBY PLAYERS=Alice

2. Bob dołącza do gry:
   C→S: - - HELLO VERSION=1.0
   S→C: - - OK
   
   C→S: - - JOIN GAME=GAME123 NAME=Bob
   S→C: - - WELCOME GAME=GAME123 PLAYER=PLAYER_Bob
   S→C: GAME123 - LOBBY PLAYERS=Alice,Bob  (do obu graczy)

3. Alice rozpoczyna grę:
   C→S: GAME123 PLAYER_Alice START
   S→C: GAME123 - STARTED DEALER=PLAYER_Alice ANTE=10 BET=20  (do obu)

4. Faza ANTE:
   S→C: GAME123 - ANTE PLAYER=PLAYER_Alice AMOUNT=10  (do obu)
   S→C: GAME123 - ANTE_OK PLAYER=PLAYER_Alice STACK=490  (do obu)
   S→C: GAME123 - ANTE PLAYER=PLAYER_Bob AMOUNT=10  (do obu)
   S→C: GAME123 - ANTE_OK PLAYER=PLAYER_Bob STACK=490  (do obu)

5. Faza DEAL:
   S→C: GAME123 - DEAL PLAYER=PLAYER_Alice CARDS=AS,KH,QD,JC,10S  (do Alice)
   S→C: GAME123 - DEAL PLAYER=PLAYER_Alice CARDS=XX,XX,XX,XX,XX  (do Bob)
   S→C: GAME123 - DEAL PLAYER=PLAYER_Bob CARDS=XX,XX,XX,XX,XX  (do Alice)
   S→C: GAME123 - DEAL PLAYER=PLAYER_Bob CARDS=9H,9D,7C,6S,3H  (do Bob)

6. Faza BET1:
   S→C: GAME123 - TURN PLAYER=PLAYER_Bob PHASE=BET1 CALL=0 MINRAISE=20  (do obu)
   
   C→S: GAME123 PLAYER_Bob CHECK
   S→C: GAME123 - ACTION PLAYER=PLAYER_Bob TYPE=CHECK  (do obu)
   
   S→C: GAME123 - TURN PLAYER=PLAYER_Alice PHASE=BET1 CALL=0 MINRAISE=20
   
   C→S: GAME123 PLAYER_Alice BET AMOUNT=20
   S→C: GAME123 - ACTION PLAYER=PLAYER_Alice TYPE=BET ARGS=20  (do obu)
   
   S→C: GAME123 - TURN PLAYER=PLAYER_Bob PHASE=BET1 CALL=20 MINRAISE=20
   
   C→S: GAME123 PLAYER_Bob CALL
   S→C: GAME123 - ACTION PLAYER=PLAYER_Bob TYPE=CALL  (do obu)

7. Faza DRAW:
   S→C: GAME123 - TURN PLAYER=PLAYER_Bob PHASE=DRAW CALL=0 MINRAISE=0
   
   C→S: GAME123 PLAYER_Bob DRAW CARDS=2,3,4
   S→C: GAME123 - DRAWOK PLAYER=PLAYER_Bob COUNT=3 NEW=XX,XX,XX  (do Alice)
   S→C: GAME123 - DRAWOK PLAYER=PLAYER_Bob COUNT=3 NEW=8H,5D,4C  (do Bob)
   
   S→C: GAME123 - TURN PLAYER=PLAYER_Alice PHASE=DRAW CALL=0 MINRAISE=0
   
   C→S: GAME123 PLAYER_Alice DRAW CARDS=none
   S→C: GAME123 - DRAWOK PLAYER=PLAYER_Alice COUNT=0 NEW=  (do obu)

8. Faza BET2:
   S→C: GAME123 - TURN PLAYER=PLAYER_Alice PHASE=BET2 CALL=0 MINRAISE=20
   
   C→S: GAME123 PLAYER_Alice BET AMOUNT=20
   S→C: GAME123 - ACTION PLAYER=PLAYER_Alice TYPE=BET ARGS=20  (do obu)
   
   S→C: GAME123 - TURN PLAYER=PLAYER_Bob PHASE=BET2 CALL=20 MINRAISE=20
   
   C→S: GAME123 PLAYER_Bob CALL
   S→C: GAME123 - ACTION PLAYER=PLAYER_Bob TYPE=CALL  (do obu)

9. Faza SHOWDOWN:
   S→C: GAME123 - SHOWDOWN PLAYER=PLAYER_Alice HAND=AS,KH,QD,JC,10S
        RANK=STRAIGHT_A  (do obu)
   S→C: GAME123 - SHOWDOWN PLAYER=PLAYER_Bob HAND=9H,9D,8H,5D,4C
        RANK=PAIR_9  (do obu)

10. Faza PAYOUT i END:
    S→C: GAME123 - WINNER PLAYER=PLAYER_Alice POT=100 RANK=STRAIGHT_A  (do obu)
    S→C: GAME123 - PAYOUT PLAYER=PLAYER_Alice AMOUNT=100 STACK=570  (do obu)
    S→C: GAME123 - END REASON=COMPLETE  (do obu)


--------------------------------------------------------------------------------
OBSŁUGA BŁĘDÓW
--------------------------------------------------------------------------------

Możliwe kody błędów (CODE w wiadomości ERR):

INVALID_COMMAND:
  - Nieprawidłowy format komendy
  - Nieznana akcja
  
NOT_YOUR_TURN:
  - Akcja poza kolejką gracza
  - Próba gry w niewłaściwej fazie

INVALID_MOVE:
  - Niedozwolony ruch (np. CHECK gdy trzeba CALL)
  - Wymiana więcej niż 3 kart
  
INSUFFICIENT_CHIPS:
  - Próba obstawienia większej kwoty niż posiadane chipy
  - Brak chipów na ante

GAME_NOT_FOUND:
  - Próba dołączenia do nieistniejącej gry
  
GAME_FULL:
  - Gra ma już maksymalną liczbę graczy (4)
  
GAME_IN_PROGRESS:
  - Próba dołączenia do rozpoczętej gry
  
NOT_HOST:
  - Próba wykonania akcji hosta przez nie-hosta
  
INVALID_STATE:
  - Akcja w niewłaściwej fazie gry

PROTOCOL_ERROR:
  - Błąd parsowania wiadomości
  - Wiadomość przekracza 512 bajtów


Przykłady komunikatów błędów:

- - ERR CODE=NOT_YOUR_TURN REASON=Wait for your turn
- - ERR CODE=INSUFFICIENT_CHIPS REASON=Not enough chips to call
- - ERR CODE=INVALID_MOVE REASON=Cannot check when there is a bet
- - ERR CODE=GAME_FULL REASON=Game already has 4 players


--------------------------------------------------------------------------------
GENEROWANIE DOKUMENTACJI JAVADOC
--------------------------------------------------------------------------------

GENEROWANIE JAVADOC DLA CAŁEGO PROJEKTU:

Komenda:
  mvn javadoc:javadoc

Opis: 
  Generuje dokumentację Javadoc dla wszystkich modułów projektu.

Lokalizacja wygenerowanej dokumentacji:
  - poker-common/target/site/apidocs/index.html
  - poker-model/target/site/apidocs/index.html
  - poker-server/target/site/apidocs/index.html
  - poker-client/target/site/apidocs/index.html


GENEROWANIE JAVADOC Z AGREGACJĄ (ZALECANE):

Komenda:
  mvn javadoc:aggregate

Opis:
  Generuje jedną skonsolidowaną dokumentację dla wszystkich modułów.

Lokalizacja:
  - target/site/apidocs/index.html (w katalogu głównym)

Zaleta: 
  Cała dokumentacja w jednym miejscu z łatwą nawigacją między modułami.


OTWIERANIE DOKUMENTACJI:

Windows:
  start target\site\apidocs\index.html

Linux/macOS:
  open target/site/apidocs/index.html
  # lub
  xdg-open target/site/apidocs/index.html


GENEROWANIE Z KODEM ŹRÓDŁOWYM:

Komenda:
  mvn javadoc:aggregate -Dshow-private=true

Opis:
  Generuje dokumentację włączając również metody prywatne.


DODATKOWE OPCJE:

1. Javadoc tylko dla jednego modułu:
   cd poker-model
   mvn javadoc:javadoc

2. Javadoc z pełną wersją Maven Site:
   mvn site
   
   Generuje kompletną stronę projektu z:
   - Javadoc (target/site/apidocs/)
   - Raporty testów (target/site/surefire-report.html)
   - Pokrycie kodu JaCoCo (target/site/jacoco/)
   - Informacje o projekcie

   Lokalizacja: target/site/index.html

3. Javadoc JAR (do publikacji):
   mvn javadoc:jar
   
   Tworzy plik JAR z dokumentacją w target/poker-XXX-javadoc.jar