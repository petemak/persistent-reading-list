(ns reading.state
    "UI state only. The books themselves live in DataScript."
  (:require [clojure.string :as str]))

(def initial-state
  {:draft ""
   :show :all
   :storage {:mode :local
             :status :loading}})

(defn new-book-command
  "A :book/add command for the draft, or nil if the draft is blank."
  [draft id now]
  (let [title (str/trim draft)]
    (when-not (str/blank? title)
      [:book/add id title now])))


(defn visible-books
  "List of all boks, read and unread"
  [books show]

  (case show
    :all books
    :unread (vec (remove :book/read? books))
    :read (vec (filter :book/read? books))))
