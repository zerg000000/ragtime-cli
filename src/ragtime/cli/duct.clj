(ns ragtime.cli.duct
  (:require [ragtime.jdbc :as jdbc]
            [ragtime.core :as ragtime]
            [pandect.algo.sha1 :refer [sha1]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ragtime.cli.core :as cc]))

;; copy from duct/ragtime.migrator
(defn- singularize [coll]
  (if (= (count coll) 1) (first coll) coll))

(defn- clean-key [base key]
  (if (vector? key)
    (singularize (remove #{base} key))
    key))

(def ^:private colon (.getBytes ":"  "US-ASCII"))
(def ^:private comma (.getBytes ","  "US-ASCII"))
(def ^:private u=    (.getBytes "u=" "US-ASCII"))
(def ^:private d=    (.getBytes "d=" "US-ASCII"))

(defn- netstring [bs]
  (let [size (.getBytes (str (count bs)) "US-ASCII")]
    (byte-array (concat size colon bs comma))))

(defn- get-bytes [^String s]
  (.getBytes s "UTF-8"))

(defn- coll->netstring [coll]
  (netstring (mapcat (comp netstring get-bytes) coll)))

(defn- hash-migration [{:keys [up down]}]
  (sha1 (byte-array (concat u= (coll->netstring up)
                            d= (coll->netstring down)))))

(defn- add-hash-to-id [migration]
  (update migration :id str "#" (subs (hash-migration migration) 0 8)))

(defn- migrate [index {:keys [migrations] :as opts}]
  (let [db    (cc/get-database opts)
        strat (cc/get-strategy opts)
        rep   (:reporter opts)]
    (ragtime/migrate-all db index migrations {:reporter rep, :strategy strat})
    (ragtime/into-index index migrations)))

(defn sql-migrations [key {:keys [up down] :as opts}]
  (-> (jdbc/sql-migration {:id   (:id opts (clean-key :duct.migrator.ragtime/sql key))
                           :up   (mapv cc/get-string up)
                           :down (mapv cc/get-string down)})
      (add-hash-to-id)))

(defn get-sql-migrations [config]
  (get-in config [:duct.profile/base :duct.migrator/ragtime :migrations]))

(defn read-config [config-file resources-dir]
  (->> (io/file config-file)
       (slurp)
       (edn/read-string {:readers {'ig/ref        identity
                                   'duct/resource (fn [f] (io/file (str resources-dir f)))
                                   'duct/include  identity
                                   'duct/env      identity}
                         :default (fn [_ value] value)})))

(defn to-sql-migrations [config migration-config]
  (mapv
    (fn [migration-id]
      (let [full-migration-id [:duct.migrator.ragtime/sql migration-id]]
        (sql-migrations
          full-migration-id
          (get-in config [:duct.profile/base full-migration-id]))))
    migration-config))

(defmethod cc/prepare-options :duct
  [{:keys [database-url config-file resources-dir reporter] :as ins}]
  (let [config (read-config config-file resources-dir)
        migration-config (get-sql-migrations config)
        migrations (to-sql-migrations config migration-config)
        opts (cond-> {:database {:spec {:connection-uri database-url}}
                      :reporter (cc/get-reporter reporter)
                      :migrations migrations}
                     (get-in config [:duct.profile/base :duct.migrator/ragtime :migrations-table])
                     (assoc :migrations-table (get-in config [:duct.profile/base :duct.migrator/ragtime :migrations-table])))]
    (merge ins opts)))

(defmethod cc/migrate-all :duct
  [opts]
  (migrate {} opts))