(ns ragtime.cli.duct
  (:require [ragtime.core :as ragtime]
            [clojure.java.io :as io]
            [ragtime.cli.core :as cc]
            [duct.core :as duct]
            [integrant.core :as ig]))

(duct/load-hierarchy)

(defn- migrate [index {:keys [migrations] :as opts}]
  (let [db    (cc/get-database opts)
        strat (cc/get-strategy opts)
        rep   (:reporter opts)]
    (ragtime/migrate-all db index migrations {:reporter rep, :strategy strat})
    (ragtime/into-index index migrations)))

(defn get-config [config-file resources-dir]
  (duct/read-config (io/file config-file)
                    {'duct/resource (fn [f] (io/file (str resources-dir f)))}))

(defn remove-duct-module
  "Remove duct modules from config"
  [config]
  (reduce (fn [acc [k v]]
            (if-not (re-find #"duct\.module" (namespace k))
              (assoc acc k v)
              acc))
          {} config))

(defmethod cc/prepare-options :duct
  [{:keys [database-url config-file resources-dir reporter] :as ins}]
  (let [raw-config (get-config config-file resources-dir)
        migration-order (get-in raw-config [:duct.profile/base :duct.migrator/ragtime :migrations])
        config (-> raw-config
                   (remove-duct-module)
                   (duct/prep-config [:duct.profile/prod])
                   (ig/init [:duct.migrator.ragtime/sql]))]
    (merge
      ins
      {:database {:spec {:connection-uri database-url}}
       :reporter (cc/get-reporter reporter)
       :migrations (map (fn [{:keys [key]}]
                            (get config [:duct.migrator.ragtime/sql key]))
                     migration-order)})))

(defmethod cc/migrate-all :duct
  [opts]
  (migrate {} opts))