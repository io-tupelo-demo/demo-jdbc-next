(ns tst.demo.jdbc-pool
  (:use demo.core tupelo.core tupelo.test)
  (:require
    [clojure.test :as cljtst]
    [clojure.java.jdbc :as jdbc]
    [hikari-cp.core :as pool]
    [tupelo.java-time :as jt]
    [clojure.walk :as walk])
  (:import [java.time ZonedDateTime Instant OffsetDateTime]
           [clojure.java.api Clojure]))

(def datasource-options-sample {:auto-commit        true
                                :read-only          false
                                :connection-timeout 30000
                                :validation-timeout 5000
                                :idle-timeout       600000
                                :max-lifetime       1800000
                                :minimum-idle       10
                                :maximum-pool-size  10
                                :pool-name          "db-pool"
                                :adapter            "h2" ; "postgresql"
                                :database-name      "database"
                                :server-name        "localhost"
                                :port-number        5432
                                :username           "sa"
                                :password           ""
                                :register-mbeans    false})

(def datasource-options-h2 {:adapter  "h2"
                            :url      "jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1"
                            :username "sa"
                            :password ""})

(def datasource-options-pg
  (glue datasource-options-sample
    {:adapter       "postgresql"
     :database-name "alan"
     :server-name   "localhost"
     :port-number   5433
     :username      "alan"
     :password      "secret"}))

(def ^:dynamic db-conn nil)

(defn with-connection-pool
  "Creates and uses a connection for test function"
  [tst-fn]
  (let [datasource
       ;(pool/make-datasource datasource-options-pg)
        (pool/make-datasource datasource-options-h2)
        ]
    (binding [db-conn {:datasource datasource}]
      (tst-fn)
      (pool/close-datasource datasource)))) ; close the connection - also closes/destroys the in-memory database

(cljtst/use-fixtures
  :once with-connection-pool) ; use the same db connection pool for all tests

(dotest
  (println "---------------------------------------------------------------------------------------------------")
  (jdbc/db-do-commands db-conn ["drop table if exists langs"])
  (jdbc/db-do-commands db-conn
    [(jdbc/create-table-ddl :langs [[:id :serial]
                                    [:lang "varchar not null"]
                                   ;[:creation :timestamptz] ; PG select => java.sql.TimeStamp
                                    [:creation :timestamp] ; H2 select => java.sql.TimeStamp
                                   ;[:creation "timestamp with time zone" ] ; H2 => org.h2.api.TimestampWithTimeZone
                                   ])])

  ; #todo need walk->Instant to accept any #inst-like value
  ;  j.u.Date, java.sql.Date, java.sql.Timestamp, ZonedDateTime, org.h2.api.TimestampWithTimeZone, OffsetDateTime

  (let [java-bday-str "1995-06-01T07:08:09.123Z"
        clj-bday-str  "2008-01-01T12:34:56.123Z"]
    (jdbc/insert-multi! db-conn :langs
      (jt/walk-instant->timestamp
        [{:lang "Clojure" :creation (Instant/parse clj-bday-str)} ; can also use OffsetDateTime/parse w/o conversion
         {:lang "Java" :creation (Instant/parse java-bday-str)}]))
    (let [result     (vec (jdbc/query db-conn ["select * from langs"]))
          final-1    (jt/walk-timestamp->instant result)
          final-2    (jt/walk-instant->str final-1)
          expected-1 [{:id 1, :lang "Clojure", :creation (Instant/parse clj-bday-str)}
                      {:id 2, :lang "Java", :creation (Instant/parse java-bday-str)}]
          expected-2 [{:id 1, :lang "Clojure", :creation clj-bday-str}
                      {:id 2, :lang "Java", :creation java-bday-str}]
          ]
      (is= (spyx-pretty final-1) (spyx-pretty expected-1))
      (is= (spyx-pretty final-2) (spyx-pretty expected-2))

      ))
  (println "---------------------------------------------------------------------------------------------------")
  )























