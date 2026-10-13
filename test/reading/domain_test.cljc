(ns reading.domain-test
    "CLJC because it runs twice: in Node (shadow-cljs) and on the JVM (clojure -M:test)."
  (:require  [datascript.core :as d]
             [reading.domain :as domain]
             [clojure.test :refer [deftest is testing]]))

(ns reading.domain-test
  "Runs twice: in Node (shadow-cljs) and on the JVM (clojure -M:test)."
  (:require [clojure.test :refer [deftest is testing]]
            [datascript.core :as d]
            [reading.domain :as domain]))

(def id1 #uuid "00000000-0000-0000-0000-000000000001")
(def id2 #uuid "00000000-0000-0000-0000-000000000002")

(defn books-after
  "Apply commands to a fresh DataScript database and return its books."
  [commands]
  (let [conn (d/create-conn (domain/datascript-schema))]
    (d/transact! conn (domain/commands->tx commands))
    (domain/books d/q @conn)))

(deftest schema-test
  (testing "DataScript hears only about the unique id"
    (is (= {:book/id {:db/unique :db.unique/identity}}
           (domain/datascript-schema))))
  (testing "Datomic gets every attribute, each with a type"
    (is (= #{:book/id :book/title :book/read? :book/created-at}
           (set (map :db/ident (domain/datomic-schema)))))
    (is (every? :db/valueType (domain/datomic-schema)))))

(deftest add-test
  (is (= [{:book/id id1 :book/title "Kindred" :book/created-at 1}]
         (books-after [[:book/add id1 "Kindred" 1]])))
  (testing "adding the same book twice keeps one book"
    (is (= 1 (count (books-after [[:book/add id1 "Kindred" 1]
                                  [:book/add id1 "Kindred" 1]]))))))

(deftest books-are-oldest-first
  (is (= ["Kindred" "Beloved"]
         (map :book/title (books-after [[:book/add id2 "Beloved" 2]
                                        [:book/add id1 "Kindred" 1]])))))

(deftest set-read-test
  (is (true? (:book/read? (first (books-after [[:book/add id1 "Kindred" 1]
                                               [:book/set-read id1 true]])))))
  (is (false? (:book/read? (first (books-after [[:book/add id1 "Kindred" 1]
                                                [:book/set-read id1 true]
                                                [:book/set-read id1 false]]))))))

(deftest remove-test
  (is (= ["Beloved"]
         (map :book/title (books-after [[:book/add id1 "Kindred" 1]
                                        [:book/add id2 "Beloved" 2]
                                        [:book/remove id1]])))))

(deftest valid-command-test
  (is (domain/valid-command? [:book/add id1 "Kindred" 1]))
  (is (domain/valid-command? [:book/set-read id1 true]))
  (is (domain/valid-command? [:book/remove id1]))
  (is (not (domain/valid-command? [:book/add id1 "  " 1])) "blank title")
  (is (not (domain/valid-command? [:book/add "not-a-uuid" "Kindred" 1])))
  (is (not (domain/valid-command? [:book/set-read id1 "yes"])))
  (is (not (domain/valid-command? [:db/retractEntity 1])) "unknown op")
  (is (not (domain/valid-command? "drop everything")) "not a vector"))

(deftest book->commands-test
  (is (= [[:book/add id1 "Kindred" 1]]
         (domain/book->commands {:book/id id1 :book/title "Kindred" :book/created-at 1})))
  (is (= [[:book/add id1 "Kindred" 1] [:book/set-read id1 true]]
         (domain/book->commands {:book/id id1 :book/title "Kindred"
                                 :book/created-at 1 :book/read? true}))))
