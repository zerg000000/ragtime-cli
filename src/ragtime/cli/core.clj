(ns ragtime.cli.core
  (:require [ragtime.strategy :as strategy]
            [ragtime.core :as ragtime]
            [ragtime.reporter :as reporter]
            [ragtime.jdbc :as jdbc]
            [ragtime.protocols :as p]
            [duct.core.resource]
            [duct.migrator.ragtime :as migrator]
            [clojure.pprint :as pp]))

(def reporters
  {:slient reporter/silent
   :print  reporter/print})

(def strategies
  {:apply-new   strategy/apply-new
   :raise-error strategy/raise-error
   :rebase      strategy/rebase})

(defn get-database [{{:keys [spec]} :database :as opts}]
  (jdbc/sql-database spec (select-keys opts [:migrations-table])))

(defn get-strategy [{:keys [strategy] :or {strategy :raise-error}}]
  (strategies strategy))

(defn get-reporter [reporter]
  (reporters reporter))

(defmulti prepare-options (fn [{:keys [mode]}] mode))

(defmulti migrate-all (fn [{:keys [mode]}] mode))

(defmulti info (fn [{:keys [mode]}] mode))

(defmethod info :default
  [{:keys [migrations] :as opts}]
  (let [applied-migrations (p/applied-migration-ids (get-database opts))
        list (map (fn [idx applied-id mig]
                    {:idx idx
                     :migration-id (when mig (p/id mig))
                     :applied applied-id})
                  (range (max (count migrations)
                              (count applied-migrations)))
                  (concat applied-migrations (repeat nil))
                  (concat migrations (repeat nil)))]
    (pp/print-table [:idx :migration-id :applied] list)))

(defmulti rollback-to (fn [{:keys [mode]}]
                        mode))

(defmulti rollback-last (fn [{:keys [mode]}]
                          mode))

(defmethod migrate-all :default
  [{:keys [migrations] :as opts}]
  (ragtime/migrate-all (get-database opts) {} migrations opts))

(defmethod rollback-to :default
  [{:keys [migration-id migrations] :as opts}]
  (ragtime/rollback-to (get-database opts) (ragtime/into-index migrations) migration-id opts))

(defmethod rollback-last :default
  [{:keys [last-n migrations] :as opts}]
  (ragtime/rollback-last (get-database opts) (ragtime/into-index migrations) last-n opts))


(extend-protocol migrator/StringSource
  String
  (get-string [s] s)
  java.io.File
  (get-string [s] (slurp s))
  java.net.URL
  (get-string [s] (slurp s)))
