(ns ^:test-refresh/focus
  tst.demo.jdbc-postgres
  (:use demo.core tupelo.core tupelo.test)
  (:require
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [next.jdbc.sql :as sql]
    ))

(def db-info {:dbtype   "postgres"
              :dbname   "example"
              :user     "postgres"
              :password "docker"
              })

(def ds (jdbc/get-datasource db-info))

; NOTE: ***** Must MANUALLY  create DB 'example' before run this test! *****
(dotest
  (jdbc/execute! ds ["drop table if exists address"])
  (let [r11 (jdbc/execute! ds ["
                create table address (
                  id      serial primary key,
                  name    varchar(32),
                  email   varchar(255)
                ) "])
        r12 (jdbc/execute! ds ["
                insert into address(name, email)
                  values( 'Homer Simpson', 'homer@springfield.co' ) "])
        r13 (jdbc/execute! ds ["select * from address "])
        ]
    (is= r11 [#:next.jdbc{:update-count 0}])
    (is= r12 [#:next.jdbc{:update-count 1}])
    (is= r13 [#:address{:id 1, :name "Homer Simpson", :email "homer@springfield.co"}]))

  (let [r22 (jdbc/execute-one! ds ["
                  insert into address(name, email)
                    values( 'Marge Simpson', 'marge@springfield.co' ) "]
              {:return-keys true})
        r23 (jdbc/execute-one! ds ["select * from address where id= ?" 2])]
    (is= r22 #:address{:id 2, :name "Marge Simpson", :email "marge@springfield.co"})
    (is= r23 #:address{:id 2, :name "Marge Simpson", :email "marge@springfield.co"}))

  (let [r32     (jdbc/execute-one! ds ["
                insert into address(name, email)
                  values( 'Bart Simpson', 'bart@mischief.com' ) "]
                  {:return-keys true :builder-fn rs/as-unqualified-lower-maps})
        r33     (jdbc/execute-one! ds ["select * from address where id= ?" 3]
                  {:builder-fn rs/as-unqualified-lower-maps})
        ds-opts (jdbc/with-options ds {:builder-fn rs/as-lower-maps})
        r34     (jdbc/execute-one! ds-opts ["select * from address where id= ?" 3])
        ]
    (is= r32 {:id 3, :name "Bart Simpson", :email "bart@mischief.com"})
    (is= r33 {:id 3, :name "Bart Simpson", :email "bart@mischief.com"})
    (is= r34 #:address{:id 3, :name "Bart Simpson", :email "bart@mischief.com"})))

(dotest
  (jdbc/execute! ds ["drop table if exists invoice"])
  (let [r41 (jdbc/execute! ds ["
                create table invoice (
                  id            serial primary key,
                  product       varchar(32),
                  unit_price    decimal(10,2),
                  unit_count    int,
                  customer_id   int
                ) "]) ; postgres does not support "unsigned" integer types
        r42 (jdbc/execute! ds ["
                insert into invoice(product, unit_price, unit_count, customer_id)
                  values
                    ( 'apple',    0.99, 6, 100 ),
                    ( 'banana',   1.25, 3, 100 ),
                    ( 'cucumber', 2.49, 2, 100 )
                "])
        r43 (reduce
              (fn [cost row]
                (+ cost (* (:unit_price row)
                          (:unit_count row))))
              0
              (jdbc/plan ds ["select * from invoice where customer_id = ? " 100]))
        ]
    (is= r41 [#:next.jdbc{:update-count 0}])
    (is= r42 [#:next.jdbc{:update-count 3}])
    (is= r43 14.67M)))

(dotest
  ; creates & drops a connection (& transaction) for each command
  (jdbc/execute-one! ds ["drop table if exists langs"])
  (jdbc/execute-one! ds ["drop table if exists releases"])
  (throws? (sql/query ds ["select * from langs"])) ; table does not exist

  ; Creates and uses a connection for all commands
  (with-open [conn (jdbc/get-connection ds)]
    (jdbc/execute-one! conn ["
        create table langs (
          id      serial,
          lang    varchar not null
        ) "])

    ; NOTE: Postgres reserves 'desc' for 'descending' (unlike H2), so must use 'descr' here
    (jdbc/execute-one! conn ["
        create table releases (
          id        serial,
          descr      varchar not null,
          langId    numeric
        ) "]))
  (is= [] (sql/query ds ["select * from langs"])) ; table exists and is empty

  ; uses one connection in a transaction for all commands
  (jdbc/with-transaction [tx ds]
    (let [tx-opts (jdbc/with-options tx {:builder-fn rs/as-lower-maps})]
      (is= #:langs{:id 1, :lang "Clojure"}
        (sql/insert! tx-opts :langs {:lang "Clojure"}))
      (sql/insert! tx-opts :langs {:lang "Java"})

      (is= (sql/query tx-opts ["select * from langs"])
        [#:langs{:id 1, :lang "Clojure"}
         #:langs{:id 2, :lang "Java"}])))

  ; uses one connection in a transaction for all commands
  (jdbc/with-transaction [tx ds]
    (let [tx-opts (jdbc/with-options tx {:builder-fn rs/as-lower-maps})]
      (let [clj-id (grab :langs/id (only (sql/query tx-opts ["select id from langs where lang='Clojure'"])))] ; all 1 string
        (is= 1 clj-id)
        (sql/insert-multi! tx-opts :releases
          [:descr :langId]
          [["ancients" clj-id]
           ["1.8" clj-id]
           ["1.9" clj-id]]))
      (let [java-id (grab :langs/id (only (sql/query tx-opts ["select id from langs where lang=?" "Java"])))] ; with query param
        (is= 2 java-id)
        (sql/insert-multi! tx-opts :releases
          [:descr :langId]
          [["dusty" java-id]
           ["8" java-id]
           ["9" java-id]
           ["10" java-id]]))

      (let [; note cannot wrap select list in parens or get "bulk" output
            result-0 (sql/query tx-opts ["select langs.lang, releases.descr
                                            from    langs join releases
                                              on     (langs.id = releases.langId)
                                              where  (lang = 'Clojure') "])
            result-1 (sql/query tx-opts ["select l.lang, r.descr
                                            from    langs as l
                                                      join releases as r
                                              on     (l.id = r.langId)
                                              where  (l.lang = 'Clojure') "])
            result-2 (sql/query tx-opts ["select langs.lang, releases.descr
                                            from    langs, releases
                                              where  ( (langs.id = releases.langId)
                                                and    (lang = 'Clojure') ) "])
            result-3 (sql/query tx-opts ["select l.lang, r.descr
                                            from    langs as l, releases as r
                                              where  ( (l.id = r.langId)
                                                and    (l.lang = 'Clojure') ) "])
            ]
        (let [expected [{:langs/lang "Clojure", :releases/descr "ancients"}
                        {:langs/lang "Clojure", :releases/descr "1.8"}
                        {:langs/lang "Clojure", :releases/descr "1.9"}]]
          (is-set= result-0 expected)
          (is-set= result-1 expected)
          (is-set= result-2 expected)
          (is-set= result-3 expected)))
      )))


(comment            ; from old JDBC usage
  (def raw-db-spec
    {:classname   "org.h2.Driver"
     :subprotocol "h2:mem" ; the prefix `jdbc:` is added automatically
     :subname     "demo;DB_CLOSE_DELAY=-1" ; ***** `;DB_CLOSE_DELAY=-1` very important!!!  *****
     ;    http://www.h2database.com/html/features.html#in_memory_databases
     ;    http://makble.com/using-h2-in-memory-database-in-clojure
     :user        "sa" ; "system admin"
     :password    "" ; empty string by default
     }))

