(ns nayim.dev
  (:require
     [suweet.twitter :as suweet :only (make-twitter-creds)]))

(defn get-twitter-cfg
  "Get twitter keys and tokens from config file"
  []
  (-> "twitter-cfg.txt"
      clojure.java.io/resource
      slurp
      read-string))

(defrecord DefaultCfg [db-params])
(defrecord DefaultDbParams [db-name views])

(defn default-db-params []
  (map->DefaultDbParams {:db-name "tw-db"
                         :views {:twitter-list {:view-name "tw-list-view" :keys []}
                                 :tweets       {:view-name "by-list"}}}))

(defn default-cfg []
  (map->DefaultCfg {:db-params (default-db-params)
                    :twitter-params (->> (get-twitter-cfg)
                                         ((juxt :app-consumer-key
                                                :app-consumer-secret
                                                :access-token-key
                                                :access-token-secret))
                                         (apply suweet/make-twitter-creds))
                    :ignore-lists #{"pics" "fitness" "food" "bfcyclists"
                                    "cycling" "deals" "friends"
                                    "egypt-middleeast"}}))

