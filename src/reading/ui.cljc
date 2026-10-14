(ns reading.ui
  (:require [reading.state :as state]
            [replicant.alias :refer [defalias]]))

(defalias progress [{::keys [done total]}]
  [:div.progress
   [:progress {:max total :value done}]
   [:span (str done " of " total " read")]])

(defn book-item [{:book/keys [id title read?]}]
  [:li {:replicant/key id
        :class (when read? "read")}
   [:label
    [:input {:type "checkbox"
             :checked (boolean read?)
             :on {:change [[:action/set-read id (not read?)]]}}]
    " "
    [:span title]]
   [:button {:type "button"
             :title "Remove"
             :on {:click [[:action/remove-book id]]}}
    "Remove"]])

(defn filter-button [current value label]
  [:button {:type "button"
            :class (when (= current value) "active")
            :on {:click [[:action/assoc-in [:show] value]]}}
   label])

(defn book-list [books show loading?]
  (let [visible (state/visible-books books show)]
    (cond
      loading?
      [:p.empty "Loading your list…"]

      (empty? visible)
      [:p.empty "Nothing to show. Add a title above, or pick another filter."]

      :else
      [:ul.books (map book-item visible)])))

(defn render-app [{:keys [draft show storage]} books]
  [:main.app
   [:h1 "Reading list"]
   [:hr]
   [:form {:on {:submit [[:action/prevent-default]
                         [:action/add-book]]}}
    [:input {:type "text"
             :placeholder "Add a book title"
             :value draft
             :replicant/on-mount (fn [{:replicant/keys [node]}]
                                   (.focus node))
             :on {:input [[:action/assoc-in [:draft] :event/target.value]]}}]
    [:button {:type "submit"} "Add"]]
   (when (seq books)
     [progress {::done (count (filter :book/read? books))
                ::total (count books)}])
   [:nav.filters
    (filter-button show :all "All")
    (filter-button show :unread "Unread")
    (filter-button show :read "Read")]
   
   (book-list books show (= :loading (:status storage)))
   
   [:hr]
   [:button {:on {:click (fn [e] (js/alert "Hi"))}} "Test Action!"]])


   
