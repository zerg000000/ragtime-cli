(ns ragtime.cli
  (:gen-class)
  (:require
    [cli-matic.core :refer [run-cmd]]
    [ragtime.cli.duct]
    [ragtime.cli.dir]
    [ragtime.cli.core :as cc]
    [clojure.java.jdbc :as j]))

(def CONFIGURATION
  {:app         {:command     "ragtime"
                 :description "A ragtime migration command-line"
                 :version     "0.0.1"}

   :global-opts [{:option  "reporter"
                  :as      "The number base for output"
                  :type    #{:slient :print}
                  :default :print}
                 {:option  "database-url"
                  :short   "db"
                  :as      "The jdbc connection string"
                  :default :present
                  :type    :string
                  :env     "DATABASE_URL"}
                 {:option "migrations-table"
                  :as     "The name of the table to store the applied migrations"
                  :type   :string
                  :default "ragtime_migrations"
                  :env    "MIGRATIONS_TABLE"}
                 {:option  "mode"
                  :type    #{:duct :dir}
                  :default :duct
                  :env     "MIGRATIONS_MODE"}
                 {:option "config-file"
                  :short  "f"
                  :as     "config.edn which contains migrations (Duct only)"
                  :type   :string
                  :env    "CONFIG_FILE"}
                 {:option  "resources-dir"
                  :short   "d"
                  :as      "Duct resources directory / SQL files directory"
                  :type    :string
                  :default "resources/"
                  :env     "RESOURCES_DIR"}]

   :commands    [{:command     "migrate-all"
                  :description "Migrate a data store with the supplied index and migration sequence."
                  :opts        [{:option "strategy"
                                 :short "s"
                                 :as "migration strategy"
                                 :type (-> cc/strategies keys set)
                                 :default :raise-error}]
                  :runs        (comp cc/migrate-all cc/prepare-options)}
                 {:command     "info"
                  :description "Show current migration info"
                  :runs        (comp cc/info cc/prepare-options)}
                 {:command     "q"
                  :description "Send test query to database"
                  :opts        [{:option "sql"
                                 :short 0
                                 :as "SQL"
                                 :type :string
                                 :default :present}
                                {:option "format"
                                 :short "f"
                                 :as "Print Format"
                                 :type :string
                                 :default "table"}]
                  :runs        (fn [{:keys [database-url sql format]}]
                                 (cond-> (j/query {:connection-uri database-url} sql)
                                         (= format "edn")
                                         (prn)
                                         (= format "table")
                                         (clojure.pprint/print-table)))}
                 {:command     "rollback-to"
                  :description "Rollback to a specific migration ID, using the supplied migration index."
                  :opts        [{:option "migration-id"
                                 :short 0
                                 :as "migration ID"
                                 :type :string
                                 :default :present}]
                  :runs        (comp cc/rollback-to cc/prepare-options)}
                 {:command     "rollback-last"
                  :description "Rollback the last n migrations from the database, using the supplied migration index."
                  :opts        [{:option "last-n"
                                 :short 0
                                 :as "n migrations"
                                 :type :int
                                 :default 1}]
                  :runs        (comp cc/rollback-last cc/prepare-options)}]})

(defn -main [& args]
  (run-cmd args CONFIGURATION))
