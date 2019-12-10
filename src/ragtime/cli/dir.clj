(ns ragtime.cli.dir
  (:require
    [ragtime.jdbc :as jdbc]
    [ragtime.cli.core :as cc]))

(defmethod cc/prepare-options :dir
  [{:keys [database-url strategy resources-dir reporter] :as ins}]
  (let [migrations (jdbc/load-directory resources-dir)
        opts {:database {:spec {:connection-uri database-url}}
              :strategy (cc/get-strategy strategy)
              :reporter (cc/get-reporter reporter)
              :migrations migrations}]
    (merge ins opts)))