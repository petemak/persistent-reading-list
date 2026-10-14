(ns reading.state-test
  (:require [cljs.test :refer [deftest is testing]]
            [reading.state :as state]))

(def id #uuid "00000000-0000-0000-0000-000000000001")

(deftest initial-state-test
  (is (= "" (:draft state/initial-state)))
  (is (= :all (:show state/initial-state)))
  (is (= {:mode :local :status :loading} (:storage state/initial-state))))

(deftest new-book-command-test
  (testing "trims the draft into a :book/add command"
    (is (= [:book/add id "Kindred" 1000]
           (state/new-book-command "  Kindred " id 1000))))
  (testing "a blank draft gives no command"
    (is (nil? (state/new-book-command "   " id 1000)))))

(def books
  [{:book/id 1 :book/title "Kindred"}
   {:book/id 2 :book/title "Beloved" :book/read? true}])

(deftest visible-books-test
  (is (= ["Kindred" "Beloved"] (map :book/title (state/visible-books books :all))))
  (is (= ["Kindred"] (map :book/title (state/visible-books books :unread))))
  (is (= ["Beloved"] (map :book/title (state/visible-books books :read)))))
