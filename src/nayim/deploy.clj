(ns nayim.deploy
     (:require
       [suweet.twitter :as suweet :only (make-twitter-creds)]))

(defrecord DefaultCfg [db-params])
(defrecord DefaultDbParams [db-name views])

(defn get-twitter-cfg
  "Get twitter params from environ variables"
  []
  [(System/getenv "APP_CONSUMER_KEY")
   (System/getenv "APP_CONSUMER_SECRET")
   (System/getenv "ACCESS_TOKEN_KEY")
   (System/getenv "ACCESS_TOKEN_SECRET")])

(defn default-db-params []
  (map->DefaultDbParams {:db-name (System/getenv "CLOUDANT_URL")
                         :views {:twitter-list {:view-name "tw-list-view" :keys []}
                                 :tweets       {:view-name "by-list"}}}))
(defn default-cfg []
  (map->DefaultCfg {:db-params (default-db-params)
                    :twitter-params (->> (get-twitter-cfg)
                                         (apply suweet/make-twitter-creds))
                    :ignore-lists #{"pics" "fitness" "food" "bfcyclists"
                                    "cycling" "deals" "friends"
                                    "egypt-middleeast"}}))

