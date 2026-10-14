(ns reading.storage-test
(:require  [reading.storage :as storage]
           [cljs.test :refer [deftest is]]))

(def books
  [{:book/id #uuid "00000000-0000-0000-0000-000000000001"
    :book/title "Kindred"
    :book/read? true
    :book/created-at 1}])

(deftest edn-round-trip
  (is (= books (storage/deserialize (storage/serialize books)))))

(deftest nothing-saved-yet
  (is (= [] (storage/deserialize nil)))
  (is (= [] (storage/deserialize ""))))
