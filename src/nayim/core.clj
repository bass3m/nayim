(ns nayim.core
  (:require [clojure.set :as clj-set :only (difference)]
            [com.ashafa.clutch :as db]
            [clj-time.core :as clj-time]
            [clj-time.coerce :as coerce]
            [suweet.twitter :as suweet :only (get-twitter-lists
                                              get-twitter-list-tweets)]
            [suweet.score :as score :only (score-tweet)]
            [nayim.dev :as dev :only (default-cfg)]
            [nayim.deploy :as deploy :only (default-cfg)]))

(defn too-old?
  ([tweet-activity-view] (too-old? 3 tweet-activity-view))
  ([days tweet-activity-view]
  ((complement clj-time/within?)
          (clj-time/interval (-> days clj-time/days clj-time/ago) (clj-time/now))
          (coerce/from-string tweet-activity-view))))

(defn generate-unique-score
  "Make a unique score string while preserving the correct order.
  This is needed because since we base our tweet scores on favs/retweets
  there is the possibility of getting 0 scores, so we need a tie-breaker.
  Use the tweet-id as the lowest significant 20 digits,
  and for the upper 8 digits, multiply the raw score by 10^8 taking
  care of rounding etc.."
  [raw-score tweet]
  (let [raw-score-factor 100000000
        max-raw-score (- raw-score-factor 1)
        score (java.lang.Math/round (double (* raw-score raw-score-factor)))]
    (format "%08d%020d"
            (if (> score max-raw-score) max-raw-score score) (:id tweet))))

(defn make-tweet-db-doc
  "Score our tweets and save them in db"
  [list-id tweets]
  (->> (:links tweets)
       (map #(let [score (score/score-tweet
                            {:tw-score :default}
                            ((juxt :fav-counts :rt-counts :follow-count) %))]
               (-> %
                   (assoc :schema "tweet")
                   (assoc :unread true)
                   (assoc :save false)
                   (assoc :list-id list-id)
                   (assoc :score score)
                   (assoc :unique-score (generate-unique-score score %)))))))

(defn mark-old-tweets-for-deletion
  "Get rid of : unread tweets older than 15 days and read tweets older than 3 days
  from db. Use the by-list view in order to get all the tweets from a given list"
  [db-params list-id]
  (let [db-name (:db-name db-params)
        db-tw-activity-view (-> (:db-name db-params)
                                (db/get-view
                                  (-> db-params :views :tweets :view-name)
                                  (-> db-params :views :tweets :view-name keyword)
                                  {:key (str list-id)}))]
    (if-let [old-tweets (and (seq db-tw-activity-view)
                             (seq (->> db-tw-activity-view
                                       ;; delete all tweets older than 15 days
                                       (filter (comp (partial too-old? 15)
                                                     (comp :last-activity :value)))
                                       ;; get tweets that were marked read
                                       (filter (comp false? :unread :value))
                                       ;; delete tweets which were read older than 3 days
                                       (filter (comp too-old? (comp :last-activity :value))))))]
      (map #(as-> % _
              (assoc _ :_rev (-> _ :value :_rev))
              (assoc _ :_id (:id _))
              (assoc _ :_deleted true)) old-tweets))))

(defn update-db-since-id!
  "Side effects a-plenty. First get the list view from db containing the
  latest since-id. Get tweets since the since-id. Update db entry with
  the id obtained from the view query. Finally return the new tweets."
  [{:keys [db-params twitter-params]} list-id]
  (let [db-name (:db-name db-params)
        db-tw-lists-view (-> db-name
                             (db/get-view
                               (-> db-params :views :twitter-list :view-name)
                               (-> db-params :views :twitter-list :view-name keyword)
                               {:key (Integer. list-id)}))]
    (when (seq db-tw-lists-view)
      (let [doc-id (:id (first db-tw-lists-view))
            since-id (:since-id (db/get-document db-name doc-id))
            tweets (->> {:list-id list-id :since-id since-id}
                        (suweet/get-twitter-list-tweets twitter-params))]
        (when (> (:since-id tweets) since-id)
          (db/update-document db-name
                              (db/get-document db-name doc-id)
                              assoc :since-id (:since-id tweets))
          tweets)))))

(defn tweet-db-housekeep!
  [{:keys [db-params] :as cfg} list-id]
  (let [old-tweets (mark-old-tweets-for-deletion db-params list-id)]
    (if-let [new-tweets (some->> list-id
                                 (update-db-since-id! cfg)
                                 (make-tweet-db-doc list-id))]
      (apply conj old-tweets new-tweets)
      old-tweets)))

(defn get-tweets-from-list
  "Get the tweet view for the twitter list"
  [{:keys [db-params] :as cfg} list-id]
  (let [db-name (:db-name db-params)]
    (some->> list-id
         (tweet-db-housekeep! cfg)
         (db/bulk-update db-name))))

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
       (map (comp :list-id :value) _)
       (nth _ (rem (clj-time/hour (clj-time/now)) (count _)))
       (get-tweets-from-list config _)))))
