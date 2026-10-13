(ns reading.actions
  (:require [clojure.walk :as walk]
            [reading.state :as state]))


(defn interpolate
  "Replace placeholders like :event/target.value with real values."
  [event actions]
  (walk/postwalk
   (fn [x]
     (case x
       :event/target.value (.. event -target -value)
       x))
   actions))

(defn perform!
  [store event [action & args]]
  (case action
    :action/prevent-default (.preventDefault event)
    :action/assoc-in (let [[path value] args]
                       (swap! store
                              assoc-in
                              path
                              value))
    :action/add-book (swap! store
                            state/add-book)
    :action/toggle-read (swap! store
                               state/toggle-read
                               (first args))
    :action/remove-book (swap! store
                               state/remove-book
                               (first args))

    (js/console.warn "Unknown action: " (pr-str action))))


(defn handle!
  [store event actions]
  (doseq [action (interpolate event actions)]
    (perform! store event action)))
