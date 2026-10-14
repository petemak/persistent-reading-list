(ns reading.actions-test
  (:require [cljs.test :refer [async deftest is testing]]
            [datascript.core :as d]
            [reading.actions :as actions]
            [reading.domain :as domain]
            [reading.state :as state]
            [reading.storage :as storage]))

;; Test doubles: two more implementations of the Storage protocol

(defrecord FakeStorage [books saved]
  storage/Storage
  (load-books [_] (js/Promise.resolve @books))
  (persist! [_ commands _db]
    (swap! saved into commands)
    (js/Promise.resolve true)))

(defn fake-storage [books]
  (->FakeStorage (atom books) (atom [])))

(defrecord BrokenStorage []
  storage/Storage
  (load-books [_] (js/Promise.reject (js/Error. "Failed to fetch")))
  (persist! [_ _ _] (js/Promise.reject (js/Error. "Failed to fetch"))))

(defn context
  "Everything handle! needs, with s as the storage for every mode."
  [s]
  {:conn (d/create-conn (domain/datascript-schema))
   :ui (atom (assoc-in state/initial-state [:storage :status] :ready))
   :store (atom s)
   :storage-for (constantly s)})

(defn fake-event [value]
  #js {:target #js {:value value}
       :preventDefault (fn [])})

(defn titles [ctx]
  (map :book/title (domain/books d/q @(:conn ctx))))

(def id #uuid "00000000-0000-0000-0000-000000000001")

(deftest interpolate-test
  (is (= [[:action/assoc-in [:draft] "Kindred"]]
         (actions/interpolate
          (fake-event "Kindred")
          [[:action/assoc-in [:draft] :event/target.value]]))))

(deftest adding-a-book
  (let [s (fake-storage [])
        ctx (context s)]
    (actions/handle! ctx (fake-event "Kindred")
                     [[:action/assoc-in [:draft] :event/target.value]])
    (actions/handle! ctx (fake-event nil)
                     [[:action/prevent-default] [:action/add-book]])
    (testing "the book is in the in-memory database"
      (is (= ["Kindred"] (titles ctx))))
    (testing "the draft is cleared"
      (is (= "" (:draft @(:ui ctx)))))
    (testing "one :book/add command went to the storage"
      (is (= [:book/add] (map first @(:saved s)))))))

(deftest blank-drafts-are-ignored
  (let [s (fake-storage [])
        ctx (context s)]
    (actions/handle! ctx (fake-event nil) [[:action/add-book]])
    (is (empty? (titles ctx)))
    (is (empty? @(:saved s)))))

(deftest set-read-and-remove
  (let [s (fake-storage [])
        ctx (context s)]
    (d/transact! (:conn ctx) (domain/commands->tx [[:book/add id "Kindred" 1]]))
    (actions/handle! ctx (fake-event nil) [[:action/set-read id true]])
    (is (true? (:book/read? (first (domain/books d/q @(:conn ctx))))))
    (actions/handle! ctx (fake-event nil) [[:action/remove-book id]])
    (is (empty? (titles ctx)))
    (is (= [[:book/set-read id true] [:book/remove id]] @(:saved s)))))

(deftest use-storage-loads-books
  (async done
    (let [s (fake-storage [{:book/id id :book/title "Beloved" :book/created-at 1}])
          ctx (context s)]
      (-> (actions/use-storage! ctx :local)
          (.then (fn [_]
                   (is (= ["Beloved"] (titles ctx)))
                   (is (= {:mode :local :status :ready} (:storage @(:ui ctx))))
                   (done)))))))

(deftest a-failed-load-shows-an-error
  (async done
    (let [ctx (context (->BrokenStorage))]
      (-> (actions/use-storage! ctx :server)
          (.then (fn [_]
                   (is (= :error (get-in @(:ui ctx) [:storage :status])))
                   (is (= "Failed to fetch" (get-in @(:ui ctx) [:storage :message])))
                   (done)))))))
