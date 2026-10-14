(ns reading.domain
  "The book data model, shared by the browser (DataScript)
  and the server (Datomic). Portable Clojure only."
  (:require [clojure.string :as str]))

;; ============================================================
;; Schema: described once, converted for each database

(def attrs
  {:book/id         {:type :uuid :unique :identity}
   :book/title      {:type :string}
   :book/read?      {:type :boolean}
   :book/created-at {:type :long}})

(defn datascript-schema
  "DataScript only needs to hear about special attributes."
  []
  (into {}
        (for [[attr {:keys [unique]}] attrs
              :when unique]
          [attr {:db/unique (keyword "db.unique" (name unique))}])))

(defn datomic-schema
  "Datomic needs every attribute declared, with a type."
  []
  (vec (for [[attr {:keys [type unique]}] attrs]
         (cond-> {:db/ident attr
                  :db/valueType (keyword "db.type" (name type))
                  :db/cardinality :db.cardinality/one}
           unique (assoc :db/unique (keyword "db.unique" (name unique)))))))

;; ============================================================
;; Commands: small domain events, as data
;; ============================================================
(defn valid-command?
  "Is cmd a well-formed command? The server checks every command
  it receives with this before touching the database."
  [cmd]
  (and (vector? cmd)
       (let [[op & args] cmd]
         (case op
           :book/add      (let [[id title created-at] args]
                            (and (= 3 (count args))
                                 (uuid? id)
                                 (string? title)
                                 (not (str/blank? title))
                                 (int? created-at)))
           :book/set-read (let [[id read?] args]
                             (and (= 2 (count args)) (uuid? id) (boolean? read?)))
           :book/remove (and (= 1 (count args)) (uuid? (first args)))
           false))))

(defn command->tx
  "Transaction data for one command. Both databases accept it."
  [[op & args]]
  (case op
    :book/add      (let [[id title created-at] args]
                     [{:book/id id
                       :book/title title
                       :book/created-at created-at}])
    :book/set-read (let [[id read?] args]
                     [{:book/id id :book/read? read?}])
    :book/remove   (let [[id] args]
                     [[:db/retractEntity [:book/id id]]])))

(defn commands->tx [commands]
  (vec (mapcat command->tx commands)))

(defn book->commands
  "The commands that recreate a book somewhere else."
  [{:book/keys [id title created-at read?]}]
  (cond-> [[:book/add id title created-at]]
    read? (conj [:book/set-read id true])))

;; ============================================================
;; Queries
;; ============================================================
(def books-query
  '[:find (pull ?e [:book/id :book/title :book/read? :book/created-at])
    :where [?e :book/id]])

(defn books
  "All books, oldest first. q is datascript.core/q or datomic.client.api/q."
  [q db]
  (->> (q books-query db)
       (map first)
       (sort-by :book/created-at)
       vec))
