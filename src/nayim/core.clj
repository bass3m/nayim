(ns nayim.core
  (:require [clojure.set :as clj-set :only (difference)]
            [com.ashafa.clutch :as db]
            [clj-time.core :as clj-time]
            [baseet-twdb.tweetdb :as twdb :only (db-update-tweets!)]
            [nayim.dev :as dev :only (default-cfg)]
            [nayim.deploy :as deploy :only (default-cfg)]))

(defn get-twitter-lists-of-interest
  "Return a map containing user's twitter lists (list name and id)
  The twitter list db is called tw-lists as well as the document itself."
  [cfg]
  (let [db-tw-lists-view (-> (-> cfg :db-params :db-name)
                             (db/get-view
                               (-> cfg :db-params :views :twitter-list :view-name)
                               (-> cfg :db-params :views :twitter-list :view-name keyword)))]
    (when (not (empty? db-tw-lists-view))
      (for [interests (clj-set/difference
                        (into #{} (map (comp :name :value) db-tw-lists-view))
                        (:ignore-lists cfg))
            a-list db-tw-lists-view
            :when (= interests ((comp :name :value) a-list))]
        a-list))))

(defn default-cfg []
  (if (System/getenv "DEPLOY")
    (deploy/default-cfg)
    (dev/default-cfg)))

(defn -main
  ([] (-main (default-cfg)))
  ([config]
   (when-not (empty? config)
     (as-> config _
       (get-twitter-lists-of-interest _)
       (nth _ (rem (clj-time/hour (clj-time/now)) (count _)))
       ((juxt (constantly (-> _ :value :name))
              (constantly (count (twdb/db-update-tweets!
                                   config (-> _ :value :list-id))))))
       (format (str "Got: " (second _) " new tweets for Twitter list: " (first _)))))))
