(ns tst.demo.jdbc
  (:use demo.core tupelo.core tupelo.test)
  (:require
    [next.jdbc :as jdbc]
    ))

(defonce db {:dbtype "h2"
             :dbname "example"})

(defonce ds (jdbc/get-datasource db))

(dotest
  (spy :drop--result (jdbc/execute! ds ["drop table if exists address"]))
  (let [r11 (jdbc/execute! ds ["
                create table address (
                  id      int auto_increment primary key,
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
    (is= r13 [#:ADDRESS{:ID 1, :NAME "Homer Simpson", :EMAIL "homer@springfield.co"}]))

  (let [r22 (jdbc/execute-one! ds ["
                insert into address(name, email)
                  values( 'Marge Simpson', 'marge@springfield.co' ) "]
              {:return-keys true})
        r23 (jdbc/execute-one! ds ["select * from address where id= ?" 2])]
    (is= r22 #:ADDRESS{:ID 2})
    (is= r23 #:ADDRESS{:ID 2, :NAME "Marge Simpson", :EMAIL "marge@springfield.co"}))

  )


(comment


  (def raw-db-spec
    {:classname   "org.h2.Driver"
     :subprotocol "h2:mem" ; the prefix `jdbc:` is added automatically
     :subname     "demo;DB_CLOSE_DELAY=-1" ; ***** `;DB_CLOSE_DELAY=-1` very important!!!  *****
     ;    http://www.h2database.com/html/features.html#in_memory_databases
     ;    http://makble.com/using-h2-in-memory-database-in-clojure
     :user        "sa" ; "system admin"
     :password    "" ; empty string by default
     })

  (dotest
    (println "---------------------------------------------------------------------------------------------------")
    (println "tst.demo.jdbc - enter")

    ; creates & drops a connection (& transaction) for each command
    (jdbc/db-do-commands raw-db-spec ["drop table if exists langs"
                                      "drop table if exists releases"])

    ; Creates and uses a connection for all commands
    (jdbc/with-db-connection [conn raw-db-spec]
      (jdbc/db-do-commands conn
        [(jdbc/create-table-ddl :langs
           [[:id :serial]
            [:lang "varchar not null"]])
         (jdbc/create-table-ddl :releases
           [[:id :serial]
            [:desc "varchar not null"]
            [:langId "numeric"]])]))

    ; create & use a connection for multiple commands
    (jdbc/with-db-connection [conn raw-db-spec]
      (jdbc/insert-multi! raw-db-spec :langs ; => ({:id 1} {:id 2})
        [{:lang "Clojure"}
         {:lang "Java"}])

      (let [result (jdbc/query raw-db-spec ["select * from langs"])]
        (is= result [{:id 1, :lang "Clojure"}
                     {:id 2, :lang "Java"}])))

    ; Wraps all commands in a single transaction
    (jdbc/with-db-transaction [tx raw-db-spec]
      (let [clj-id (grab :id (only (jdbc/query tx ["select id from langs where lang='Clojure'"])))]
        (jdbc/insert-multi! tx :releases
          [{:desc "ancients" :langId clj-id}
           {:desc "1.8" :langId clj-id}
           {:desc "1.9" :langId clj-id}]))
      (let [java-id (grab :id (only (jdbc/query tx ["select id from langs where lang='Java'"])))]
        (jdbc/insert-multi! tx :releases
          [{:desc "dusty" :langId java-id}
           {:desc "8" :langId java-id}
           {:desc "9" :langId java-id}
           {:desc "10" :langId java-id}])))

    ; Creates and uses a connection for each command
    (let [
          ; note cannot wrap select list in parens or get "bulk" output
          result-0 (jdbc/query raw-db-spec ["select langs.lang, releases.desc
                                             from    langs join releases
                                               on     (langs.id = releases.langId)
                                               where  (lang = 'Clojure') "])
          result-1 (jdbc/query raw-db-spec ["select l.lang, r.desc
                                             from    langs as l
                                                       join releases as r
                                               on     (l.id = r.langId)
                                               where  (l.lang = 'Clojure') "])
          result-2 (jdbc/query raw-db-spec ["select langs.lang, releases.desc
                                            from    langs, releases
                                              where  ( (langs.id = releases.langId)
                                                and    (lang = 'Clojure') ) "])
          result-3 (jdbc/query raw-db-spec ["select l.lang, r.desc
                                            from    langs as l, releases as r
                                              where  ( (l.id = r.langId)
                                                and    (l.lang = 'Clojure') ) "])
          ]
      (let [expected [{:lang "Clojure", :desc "ancients"}
                      {:lang "Clojure", :desc "1.8"}
                      {:lang "Clojure", :desc "1.9"}]]
        (spyx-pretty expected)
        (is-set= result-0 expected)
        (is-set= result-1 expected)
        (is-set= result-2 expected)
        (is-set= result-3 expected)))

    (println "tst.demo.jdbc - leave")
    (println "---------------------------------------------------------------------------------------------------")

    )

  )
