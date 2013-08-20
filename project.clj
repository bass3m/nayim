(defproject nayim "0.1.0-SNAPSHOT"
  :description "Periodic task. Feeder for baseet app"
  :url "https://github.com/bass3m/nayim"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :main nayim.core
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.ashafa/clutch "0.4.0-RC1"]
                 [suweet "0.1.6-SNAPSHOT"]
                 [baseet-twdb "0.1.2-SNAPSHOT"]])
