(ns reading.core
  (:require [datascript.core :as d]
            [reading.actions :as actions]
            [reading.domain :as domain]
            [reading.state :as state]
            [reading.storage :as storage]
            [reading.ui :as ui]
            [replicant.dom :as r]))

;; The books: an in-memory DataScript database
(defonce conn (d/create-conn (domain/datascript-schema)))

;; Everything else on screen: draft, filter, storage status
(defonce ui-state (atom state/initial-state))

;; The storage in use right now
(defonce store (atom nil))

(def ctx
  {:conn conn
   :ui ui-state
   :store store
   :storage-for (fn [mode] (storage/storage-for mode))})

(defn render! []
  (r/render (js/document.getElementById "app")
            (ui/render-app @ui-state (domain/books d/q @conn))))

(defn ^:dev/after-load reload! []
  (render!))

(defn init []
  (r/set-dispatch!
   (fn [{:replicant/keys [trigger dom-event]} action-data]
     (when (= trigger :replicant.trigger/dom-event)
       (actions/handle! ctx dom-event action-data))))
  ;; re-render when UI state changes; remember the chosen storage
  (add-watch ui-state ::render
             (fn [_ _ old new]
               (let [mode (get-in new [:storage :mode])]
                 (when (not= mode (get-in old [:storage :mode]))
                   (storage/remember-mode! mode)))
               (render!)))
  ;; re-render when the books change
  (d/listen! conn ::render (fn [_] (render!)))
  (actions/use-storage! ctx (storage/saved-mode))
  (render!))
