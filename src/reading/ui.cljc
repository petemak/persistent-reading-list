(ns reading.ui
  (:require [reading.state :as state]
            [replicant.alias :refer [defalias]]))

;; (defn book-item
;;   [book]
;;   [:li {:replicant/key (:id book)}
;;    [:span (:title book)]])

(defalias progress [{::keys [done total]}]
  [:div.progress
   [:progress {:max total :value done}]
   [:span (str done " of " total " read")]])

(defn book-item
  [book]
  [:li {:replicant/key (:id book)
        :class (when (:read? book) "read")}
   [:label
    [:input {:type "checkbox"
             :checked (:read? book)
             :on {:change [[:action/toggle-read (:id book)]]}}]
    " "
    [:span (:title book)]]
   [:button {:type "button"
             :title "Remove"
             :on {:click [[:action/remove-book (:id book)]]}}
    "Remove"]])

(defn filter-button
  [current value label]
  [:button {:type "button"
            :class (when (= current value) "active")
            :on {:click [[:action/assoc-in [:show] value]]}}
   label])


(defn render-app
  [state]
  (let [books (:books state)
        visible (state/visible-books state)]
    [:main.app
     [:h1 "Reading List"]
     [:hr]
     [:form {:on {:submit [[:action/prevent-default]
                           [:action/add-book]]}}
      [:input {:type "text"
               :placeholder "Add a book title"
               :value (:draft state)
               :replicant/on-mount (fn [{:replicant/keys [node]}]
                                     (.focus node))
               :on {:input [[:action/assoc-in [:draft] :event/target.value]]}}]
      [:button {:type "submit"} "Add"]]

     (when (seq books)
       [progress {::done (count (filter :read? books))
                  ::total (count books)}])

     [:nav.filters
      (filter-button (:show state) :all    "All")
      (filter-button (:show state) :unread "Unread")
      (filter-button (:show state) :read   "Read")]
     
     (if (empty? visible)
       [:p.empty "Nothing to show. Add a title above, or pick another filter."]
       [:ul.books (map book-item visible)])
     
     [:hr]
     [:button {:on {:click (fn [e] (js/alert "Hi"))}} "Say hi"]]))
   
