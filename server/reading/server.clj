(ns reading.server
  (:require [reitit.ring :as ring]
            [clojure.java.io :as io]
            [datomic.client.api :as d]
            [reading.domain :as domain]
            [org.httpkit.server :as http]
            [cognitect.transit :as transit]       )
  (:import (java.io ByteArrayOutputStream)))

;; ============================================================
;; Transit
;; ============================================================
(defn read-transit [in]
  (transit/read (transit/reader in :json)))

(defn write-transit [data]
  (let [out (ByteArrayOutputStream.)]
    (transit/write (transit/writer out :json) data)
    (.toString out "UTF-8")))

(defn respond [status data]
  {:status status
   :headers {"Content-Type" "application/transit+json"}
   :body (write-transit data)})

;; ============================================================
;; Datomic
;; ============================================================
(defn connect
  "Connect to the reading-list database, creating it and installing
  the schema if needed. storage-dir is an absolute path, or :mem."
  [{:keys [storage-dir db-name]}]
  (let [client (d/client {:server-type :datomic-local
                          :system "reading-list"
                          :storage-dir storage-dir})]
    (when-not (some #{db-name} (d/list-databases client {}))
      (d/create-database client {:db-name db-name}))
    (let [conn (d/connect client {:db-name db-name})]
      ;; asserting the same schema again changes nothing
      (d/transact conn {:tx-data (domain/datomic-schema)})
      conn)))

;; ============================================================
;; HTTP
;; ============================================================
(defn get-books [conn]
  (respond 200 (domain/books d/q (d/db conn))))

(defn post-commands [conn commands]
  (if (and (vector? commands)
           (seq commands)
           (every? domain/valid-command? commands))
    (try
      (d/transact conn {:tx-data (domain/commands->tx commands)})
      (respond 200 {:ok true})
      (catch Exception e
        (respond 409 {:error (ex-message e)})))
    (respond 400 {:error "Expected a vector of valid commands"})))

(defn app [conn]
  (ring/ring-handler
   (ring/router
    [["/api/health"   {:get  (fn [_] (respond 200 {:ok true}))}]
     ["/api/books"    {:get  (fn [_] (get-books conn))}]
     ["/api/commands" {:post (fn [req]
                               (post-commands conn (some-> (:body req) read-transit)))}]])
   ;; anything else: a file from public/, or 404
   (ring/routes
    (ring/create-file-handler {:path "/" :root "public"})
    (ring/create-default-handler))))

(defn -main [& _]
  (let [dir (doto (io/file "data" "datomic") .mkdirs)
        conn (connect {:storage-dir (.getAbsolutePath dir)
                       :db-name "reading-list"})]
    (http/run-server (app conn) {:port 3000})
    (println "Reading list server on http://localhost:3000")
    @(promise)))
