(defproject demo "0.1.0-SNAPSHOT"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [
                 [com.h2database/h2 "1.4.200"] ; #todo cannot upgrade yet or crash!
                 [hikari-cp "3.0.1"]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/test.check "1.1.1"]
                 [org.postgresql/postgresql "42.6.0"]
                 [prismatic/schema "1.4.1"]
                 [seancorfield/next.jdbc "1.2.659"] ; #todo try porsas
                 [tupelo "23.07.04"]
                 ]

  :plugins      [[com.jakemccrary/lein-test-refresh "0.25.0"]
                 [lein-ancient "0.7.0"]
                 ]

  :global-vars {*warn-on-reflection* false}
  :main ^:skip-aot demo.core

  :source-paths ["src"]
  :test-paths ["test"]
  :target-path "target/%s"
  :jvm-opts ["-Xms500m" "-Xmx2g"]
  )

