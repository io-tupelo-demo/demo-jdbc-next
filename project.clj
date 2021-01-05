(defproject demo  "0.1.0-SNAPSHOT"
  :description    "FIXME: write description"
  :url            "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
     [com.h2database/h2 "1.4.200"]
     [hikari-cp "2.13.0"]
                 ; #todo try jdbc.next and porsas 
     [org.clojure/java.jdbc "0.7.11"]
     [org.clojure/clojure "1.10.1"]
     [org.clojure/test.check "1.1.0"]
     [org.postgresql/postgresql "42.2.18"] ; https://mvnrepository.com/artifact/org.postgresql/postgresql
     [prismatic/schema "1.1.12"]
     [tupelo "20.12.03"]
     ]
  :profiles {:dev     {:dependencies []
                       :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                      [lein-ancient "0.6.15"]
                                      [lein-codox "0.10.7"]]}
             :uberjar {:aot :all}}

  :global-vars {*warn-on-reflection* false}
  :main ^:skip-aot demo.core

  :source-paths ["src"]
  :test-paths ["test"]
  :libs [ "libs/"]
  :java-source-paths ["src-java"]
  :target-path "target/%s"
  :jvm-opts ["-Xms500m" "-Xmx2g"]
  )

