(ns reading.state
  (:require [clojure.string :as str]))

(def initial-state
  {:draft ""
   :show :all
   :books []})


(defn add-book [state]
  (let [title (str/trim (:draft state))]
    (if (str/blank? title)
      state
      (-> state
          (update :books conj {:id (random-uuid)
                               :title title
                               :read? false})
          (assoc :draft "")))))


(defn toggle-read
  [state id]
  (update state :books
          (fn [books]
            (mapv #(if (= id (:id %)) (update % :read? not) %)
                  books))))


(defn remove-book
  [state id]
  (update state :books
          (fn [books]
            (filterv #(not= id (:id %))
                     books))))

(defn visible-books
  "The state key is named :show rather than :filter on purpose. 
   Destructuring {:keys [filter]} would shadow clojure.core/filter 
   inside the function, and the :read branch would break. 
   A test like the one above catches that immediately."
  [{:keys [books show]}]
  (case show
    :all books
    :unread (remove :read? books)
    :read (filter :read? books)))
