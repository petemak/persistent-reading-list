(ns reading.actions
  (:require [clojure.walk :as walk]
            [datascript.core :as d]
            [reading.domain :as domain]
            [reading.state :as state]
            [reading.storage :as storage]))

(defn interpolate
  "Replace placeholders like :event/target.value with real values."
  [event actions]
  (walk/postwalk
   (fn [x]
     (case x
       :event/target.value (.. event -target -value)
       x))
   actions))

(defn set-status! [ui status message]
  (swap! ui update :storage assoc :status status :message message))

(defn execute!
  "Apply commands to the in-memory database, then save them.
  Returns the storage's promise."
  [{:keys [conn ui store]} commands]
  (d/transact! conn (domain/commands->tx commands))
  (-> (storage/persist! @store commands @conn)
      (.catch (fn [e]
                (set-status! ui :error (str "Your last change wasn't saved: "
                                            (.-message e)))))))

(defn use-storage!
  "Switch to the storage for mode, and load its books into a fresh
  in-memory database. Returns a promise."
  [{:keys [conn ui store storage-for]} mode]
  (let [s (storage-for mode)]
    (reset! store s)
    (swap! ui assoc :storage {:mode mode :status :loading})
    (-> (storage/load-books s)
        (.then (fn [books]
                 ;; ignore a slow load if the user switched again meanwhile
                 (when (identical? s @store)
                   (d/reset-conn! conn (d/empty-db (domain/datascript-schema)))
                   (d/transact! conn (vec books))
                   (swap! ui assoc :storage {:mode mode :status :ready}))))
        (.catch (fn [e]
                  (when (identical? s @store)
                    (swap! ui assoc :storage {:mode mode
                                              :status :error
                                              :message (.-message e)})))))))

(defn perform! [ctx event [action & args]]
  (case action
    :action/prevent-default (.preventDefault event)
    :action/assoc-in (let [[path value] args]
                       (swap! (:ui ctx) assoc-in path value))
    :action/add-book (let [ui (:ui ctx)]
                       (when-let [command (state/new-book-command
                                           (:draft @ui) (random-uuid) (.now js/Date))]
                         (execute! ctx [command])
                         (swap! ui assoc :draft "")))
    :action/set-read (let [[id read?] args]
                       (execute! ctx [[:book/set-read id read?]]))
    :action/remove-book (execute! ctx [[:book/remove (first args)]])
    (js/console.warn "Unknown action:" (pr-str action))))

(defn handle! [ctx event actions]
  (doseq [action (interpolate event actions)]
    (perform! ctx event action)))
