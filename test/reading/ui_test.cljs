(ns reading.ui-test
  (:require [cljs.test :refer [deftest is testing]]
            [reading.state :as state]
            [reading.test-util :as tu]
            [reading.ui :as ui]))

(def ready (assoc-in state/initial-state [:storage :status] :ready))

(def books
  [{:book/id 1 :book/title "Kindred" :book/created-at 1}
   {:book/id 2 :book/title "Beloved" :book/read? true :book/created-at 2}])

(defn texts [hiccup tag]
  (map tu/text (tu/find-tag hiccup tag)))

(defn clicks [hiccup]
  (map #(get-in (tu/attrs %) [:on :click]) (tu/find-tag hiccup "button")))

(deftest form-test
  (let [app (ui/render-app (assoc ready :draft "Kin") [])
        [form] (tu/find-tag app "form")
        [input] (tu/find-tag app "input")]
    (is (= [[:action/prevent-default] [:action/add-book]]
           (get-in (tu/attrs form) [:on :submit])))
    (is (= "Kin" (:value (tu/attrs input))))))

(deftest book-item-test
  (let [li (ui/book-item {:book/id 42 :book/title "Kindred"})
        [checkbox] (tu/find-tag li "input")]
    (testing "each item is keyed by the book id"
      (is (= 42 (:replicant/key (tu/attrs li)))))
    (testing "the checkbox asks for the opposite of the current state"
      (is (= [[:action/set-read 42 true]]
             (get-in (tu/attrs checkbox) [:on :change])))
      (let [read-li (ui/book-item {:book/id 42 :book/title "Kindred" :book/read? true})
            [read-box] (tu/find-tag read-li "input")]
        (is (= [[:action/set-read 42 false]]
               (get-in (tu/attrs read-box) [:on :change])))
        (is (= "read" (:class (tu/attrs read-li))))))
    (testing "remove sends the book id"
      (is (= [[[:action/remove-book 42]]] (clicks li))))))

(deftest only-visible-books-are-listed
  (let [app (ui/render-app (assoc ready :show :unread) books)]
    (is (= ["Kindred"] (texts app "span")))))

(deftest loading-message-test
  (is (some #{"Loading your list…"} (texts (ui/render-app state/initial-state []) "p")))
  (is (not-any? #{"Loading your list…"} (texts (ui/render-app ready []) "p"))))

(defn progress-nodes [app]
  (filter #(= ui/progress (first %)) (tu/nodes app)))

(deftest progress-test
  (is (empty? (progress-nodes (ui/render-app ready []))))
  (let [[p] (progress-nodes (ui/render-app ready books))]
    (is (= {::ui/done 1 ::ui/total 2} (tu/attrs p)))))
