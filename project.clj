(defproject demo  "0.1.0-SNAPSHOT"
  :description    "FIXME: write description"
  :url            "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [com.h2database/h2 "1.4.200"] ; #todo cannot upgrade yet or crash!
                 [hikari-cp "2.13.0"]
                 ; #todo try porsas
                 ; [org.clojure/java.jdbc "0.7.11"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/test.check "1.1.1"]
                 ; [org.postgresql/postgresql "42.2.18"] ; https://mvnrepository.com/artifact/org.postgresql/postgresql
                 [prismatic/schema "1.2.0"]
                 [seancorfield/next.jdbc "1.2.659"]
                 [tupelo "22.02.09"]
                 ]
  :profiles {:dev     {:dependencies []
                       :plugins      [[com.jakemccrary/lein-test-refresh "0.25.0"]
                                      [lein-ancient "0.7.0"]
                                      ]}
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

