

(ns reading.server-test
  (:require [clojure.test :refer [deftest is testing]]
            [reading.server :as server])
  (:import (java.io ByteArrayInputStream)))

(defn test-app
  "The real app, on a fresh in-memory Datomic database."
  []
  (server/app (server/connect {:storage-dir :mem
                               :db-name (str (random-uuid))})))

(defn call [app method uri & [body]]
  (let [response (app (cond-> {:request-method method :uri uri}
                        body (assoc :body (ByteArrayInputStream.
                                           (.getBytes (server/write-transit body)
                                                      "UTF-8")))))]
    (assoc response :data (server/read-transit
                           (ByteArrayInputStream. (.getBytes (:body response) "UTF-8"))))))

(def id #uuid "00000000-0000-0000-0000-000000000001")

(deftest health-test
  (is (= 200 (:status (call (test-app) :get "/api/health")))))

(deftest commands-are-stored
  (let [app (test-app)]
    (testing "an empty database has no books"
      (is (= [] (:data (call app :get "/api/books")))))
    (testing "commands are accepted"
      (is (= 200 (:status (call app :post "/api/commands"
                                [[:book/add id "Kindred" 1]
                                 [:book/set-read id true]])))))
    (testing "and the books come back"
      (is (= [{:book/id id :book/title "Kindred" :book/read? true :book/created-at 1}]
             (:data (call app :get "/api/books")))))))

(deftest invalid-commands-are-rejected
  (let [app (test-app)]
    (is (= 400 (:status (call app :post "/api/commands"
                              [[:db/retractEntity 1]]))))
    (is (= [] (:data (call app :get "/api/books"))) "nothing was stored")))



